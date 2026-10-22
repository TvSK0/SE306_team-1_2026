package lavs.data;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// The SQLite file on the MMM's laptop. This creates the PendingChange and CategoryConstant
// tables the first time the file is opened, so saves will survive in different cases like 
// a dropped connection or restart.
public class LocalDatabase {

    static final String CREATE_PENDING_CHANGE =
            "CREATE TABLE IF NOT EXISTS PendingChange ("
            + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + " createdAt TEXT NOT NULL,"
            + " messageType TEXT NOT NULL,"
            + " customerNumber TEXT,"
            + " createsNumber TEXT,"
            + " fields TEXT NOT NULL,"
            + " status TEXT NOT NULL,"
            + " statusMessage TEXT,"
            + " skipDuplicateCheck INTEGER NOT NULL DEFAULT 0,"
            + " duplicateMatches TEXT)";

    // AI-assisted: the customers a paused new customer may already be (US11). A file made before
    // they were kept has no column for them, so it is added when the file is opened.
    static final String DUPLICATE_MATCHES = "duplicateMatches";
    static final String ADD_DUPLICATE_MATCHES = "ALTER TABLE PendingChange ADD COLUMN " + DUPLICATE_MATCHES + " TEXT";

    static final String CREATE_CATEGORY_CONSTANT =
            "CREATE TABLE IF NOT EXISTS CategoryConstant ("
            + " category TEXT NOT NULL,"
            + " position INTEGER NOT NULL,"
            + " value TEXT NOT NULL,"
            + " PRIMARY KEY (category, position))";

    private final String url;

    public LocalDatabase(Path file) {
        this.url = "jdbc:sqlite:" + file.toAbsolutePath();
        createTables();
    }

    // Opens a new connection, the caller closes it. (so open in try {} statements in order for it to close automatically
    // in the case there's an exception.)
    public Connection connect() throws SQLException {
        return DriverManager.getConnection(url);
    }

    private void createTables() {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(CREATE_PENDING_CHANGE);
            statement.executeUpdate(CREATE_CATEGORY_CONSTANT);
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS NumberMapping (temporary TEXT PRIMARY KEY, actual TEXT NOT NULL)");
            if (!hasPendingChangeColumn(connection, DUPLICATE_MATCHES)) {
                statement.executeUpdate(ADD_DUPLICATE_MATCHES);
            }
        } catch (SQLException e) {
            throw new LocalStoreException("Could not open the local database at " + url, e);
        }
    }

    // AI-assisted: whether the PendingChange table already has the column (US11)
    private static boolean hasPendingChangeColumn(Connection connection, String column) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet columns = statement.executeQuery("PRAGMA table_info(PendingChange)")) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
