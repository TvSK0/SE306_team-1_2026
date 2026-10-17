package lavs.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class LocalDatabaseTests {

    @TempDir
    Path folder;

    @Test
    void createsFileAndBothTables() throws Exception {
        Path file = folder.resolve("lavs.db");
        LocalDatabase database = new LocalDatabase(file);

        List<String> tables = new ArrayList<>();
        try (Connection connection = database.connect();
             ResultSet rows = connection.getMetaData().getTables(null, null, "%", new String[] {"TABLE"})) {
            while (rows.next()) {
                tables.add(rows.getString("TABLE_NAME"));
            }
        }

        assertTrue(Files.exists(file));
        assertTrue(tables.contains("PendingChange"));
        assertTrue(tables.contains("CategoryConstant"));
    }

    @Test
    void openingAnExistingFileKeepsItsData() throws Exception {
        Path file = folder.resolve("lavs.db");
        SqlitePendingChangeStore first = new SqlitePendingChangeStore(new LocalDatabase(file));
        first.add(new lavs.model.PendingChange("customer.addEmail", "C1", null, java.util.Map.of()));

        SqlitePendingChangeStore reopened = new SqlitePendingChangeStore(new LocalDatabase(file));

        assertEquals(1, reopened.all().size());
    }

    @Test
    void unusablePathThrowsLocalStoreException() {
        LocalStoreException error = assertThrows(LocalStoreException.class, () -> new LocalDatabase(folder));

        assertTrue(error.getMessage().contains("Could not open the local database"));
    }
}
