package lavs.data;

import lavs.model.PendingChange;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// Stores the pending-change queue in the PendingChange table. Fields are stored as JSON
public class SqlitePendingChangeStore implements ChangeQueue {

    private static final String COLUMNS = "id, createdAt, messageType, customerNumber, createsNumber,"
            + " fields, status, statusMessage, skipDuplicateCheck, duplicateMatches";

    private final LocalDatabase database;

    public SqlitePendingChangeStore(LocalDatabase database) {
        this.database = database;
    }

    @Override
    public synchronized PendingChange add(PendingChange change) {
        String sql = "INSERT INTO PendingChange (createdAt, messageType, customerNumber, createsNumber,"
                + " fields, status, statusMessage, skipDuplicateCheck, duplicateMatches)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, change.getCreatedAt().toString());
            statement.setString(2, change.getMessageType());
            statement.setString(3, change.getCustomerNumber());
            statement.setString(4, change.getCreatesNumber());
            statement.setString(5, JSONValue.toJSONString(change.getFields()));
            statement.setString(6, change.getStatus().name());
            statement.setString(7, change.getStatusMessage());
            statement.setInt(8, change.isSkipDuplicateCheck() ? 1 : 0);
            statement.setString(9, matchesJson(change));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                change.setId(keys.getLong(1));
            }
            return change;
        } catch (SQLException e) {
            throw new LocalStoreException("Could not save the change for upload", e);
        }
    }

    @Override
    public synchronized Optional<PendingChange> next() {
        List<PendingChange> found = query("SELECT " + COLUMNS + " FROM PendingChange"
                + " WHERE status = 'PENDING' ORDER BY id LIMIT 1");
        return found.stream().findFirst();
    }

    @Override
    public synchronized List<PendingChange> all() {
        return query("SELECT " + COLUMNS + " FROM PendingChange ORDER BY id");
    }

    @Override
    public synchronized void update(PendingChange change) {
        String sql = "UPDATE PendingChange SET customerNumber = ?, fields = ?, status = ?, statusMessage = ?,"
                + " skipDuplicateCheck = ?, duplicateMatches = ? WHERE id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, change.getCustomerNumber());
            statement.setString(2, JSONValue.toJSONString(change.getFields()));
            statement.setString(3, change.getStatus().name());
            statement.setString(4, change.getStatusMessage());
            statement.setInt(5, change.isSkipDuplicateCheck() ? 1 : 0);
            statement.setString(6, matchesJson(change));
            statement.setLong(7, change.getId());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new LocalStoreException("Could not update the queued change", e);
        }
    }

    @Override
    public synchronized void markRejected(long id, String reason) {
        String sql = "UPDATE PendingChange SET status = 'REJECTED', statusMessage = ? WHERE id = ?";
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, reason);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new LocalStoreException("Could not mark the change as rejected", e);
        }
    }

    @Override
    public synchronized void delete(long id) {
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM PendingChange WHERE id = ?")) {
            statement.setLong(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new LocalStoreException("Could not remove the uploaded change", e);
        }
    }

    // AI-assisted: deleting the create and rewriting its dependants must survive crashes together.
    @Override
    public synchronized void commit(List<PendingChange> updates, List<Long> removed, Map<String, String> numbers) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement statement = connection.prepareStatement(
                        "UPDATE PendingChange SET customerNumber=?, fields=?, status=?, statusMessage=?, skipDuplicateCheck=?,"
                                + " duplicateMatches=? WHERE id=?")) {
                    for (PendingChange change : updates) {
                        statement.setString(1, change.getCustomerNumber());
                        statement.setString(2, JSONValue.toJSONString(change.getFields()));
                        statement.setString(3, change.getStatus().name());
                        statement.setString(4, change.getStatusMessage());
                        statement.setInt(5, change.isSkipDuplicateCheck() ? 1 : 0);
                        statement.setString(6, matchesJson(change));
                        statement.setLong(7, change.getId());
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(
                        "INSERT OR REPLACE INTO NumberMapping (temporary, actual) VALUES (?, ?)")) {
                    for (var entry : numbers.entrySet()) {
                        statement.setString(1, entry.getKey());
                        statement.setString(2, entry.getValue());
                        statement.executeUpdate();
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement("DELETE FROM PendingChange WHERE id=?")) {
                    for (long id : removed) {
                        statement.setLong(1, id);
                        statement.executeUpdate();
                    }
                }
                connection.commit();
            } catch (SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        } catch (SQLException error) {
            throw new LocalStoreException("Could not finish the uploaded change", error);
        }
    }

    @Override
    public synchronized Map<String, String> knownNumbers() {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT temporary, actual FROM NumberMapping")) {
            Map<String, String> result = new LinkedHashMap<>();
            while (rows.next()) result.put(rows.getString(1), rows.getString(2));
            return result;
        } catch (SQLException error) {
            throw new LocalStoreException("Could not read accepted record numbers", error);
        }
    }

    private List<PendingChange> query(String sql) {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            List<PendingChange> changes = new ArrayList<>();
            while (rows.next()) {
                changes.add(read(rows));
            }
            return changes;
        } catch (SQLException e) {
            throw new LocalStoreException("Could not read the upload queue", e);
        }
    }

    private static PendingChange read(ResultSet row) throws SQLException {
        PendingChange change = new PendingChange(row.getString("messageType"), row.getString("customerNumber"),
                row.getString("createsNumber"), parseFields(row.getString("fields")));
        change.setId(row.getLong("id"));
        change.setCreatedAt(Instant.parse(row.getString("createdAt")));
        change.setStatus(PendingChange.Status.valueOf(row.getString("status")));
        change.setStatusMessage(row.getString("statusMessage"));
        change.setSkipDuplicateCheck(row.getInt("skipDuplicateCheck") == 1);
        change.setDuplicateMatches(parseMatches(row.getString("duplicateMatches")));
        return change;
    }

    // AI-assisted: the duplicate matches as a JSON list of customer numbers (US11)
    private static String matchesJson(PendingChange change) {
        return JSONValue.toJSONString(change.getDuplicateMatches());
    }

    // AI-assisted: nothing stored (a row from before the column was added) means no matches
    private static List<String> parseMatches(String json) {
        List<String> matches = new ArrayList<>();
        if (json == null) {
            return matches;
        }
        for (Object number : (JSONArray) JSONValue.parse(json)) {
            matches.add(number.toString());
        }
        return matches;
    }

    private static Map<String, String> parseFields(String json) {
        Map<String, String> fields = new LinkedHashMap<>();
        JSONObject parsed = (JSONObject) JSONValue.parse(json);
        for (Object key : parsed.keySet()) {
            Object value = parsed.get(key);
            fields.put((String) key, value == null ? null : value.toString());
        }
        return fields;
    }
}
