package lavs.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// This keeps the category constants in the CategoryCosntant table so that dropdowns will
// still work offline. Values are kept per category in display order, matching ConstantsService.

public class CategoryConstantStore {

    private final LocalDatabase database;

    public CategoryConstantStore(LocalDatabase database) {
        this.database = database;
    }

    //Replaces everything saved with these values in one transaction
    public synchronized void saveAll(Map<String, List<String>> values) {
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try (Statement clear = connection.createStatement();
                 PreparedStatement insert = connection.prepareStatement(
                         "INSERT INTO CategoryConstant (category, position, value) VALUES (?, ?, ?)")) {
                clear.executeUpdate("DELETE FROM CategoryConstant");
                for (Map.Entry<String, List<String>> category : values.entrySet()) {
                    List<String> items = category.getValue();
                    for (int position = 0; position < items.size(); position++) {
                        insert.setString(1, category.getKey());
                        insert.setInt(2, position);
                        insert.setString(3, items.get(position));
                        insert.addBatch();
                    }
                }
                insert.executeBatch();
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new LocalStoreException("Could not save the category constants", e);
        }
    }

    // If anything's saved, it returns each category name with its values in display order
    // If nothing's saved, it returns an empty map.
    public synchronized Map<String, List<String>> loadAll() {
        String sql = "SELECT category, value FROM CategoryConstant ORDER BY category, position";
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            Map<String, List<String>> values = new LinkedHashMap<>();
            while (rows.next()) {
                values.computeIfAbsent(rows.getString("category"), category -> new ArrayList<>())
                        .add(rows.getString("value"));
            }
            values.replaceAll((category, items) -> Collections.unmodifiableList(items));
            return Collections.unmodifiableMap(values);
        } catch (SQLException e) {
            throw new LocalStoreException("Could not read the category constants", e);
        }
    }
}
