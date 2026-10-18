package lavs.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class CategoryConstantStoreTests {

    @TempDir
    Path folder;

    private LocalDatabase database;
    private CategoryConstantStore store;

    @BeforeEach
    void setUp() {
        database = new LocalDatabase(folder.resolve("lavs.db"));
        store = new CategoryConstantStore(database);
    }

    @Test
    void emptyStoreLoadsNothing() {
        assertTrue(store.loadAll().isEmpty());
    }

    @Test
    void savedValuesLoadBackInDisplayOrder() {
        Map<String, List<String>> values = new LinkedHashMap<>();
        values.put("AddressType", List.of("Residential", "Postal", "Other"));
        values.put("EmailType", List.of("Work", "Personal"));

        store.saveAll(values);

        Map<String, List<String>> loaded = store.loadAll();
        assertEquals(List.of("Residential", "Postal", "Other"), loaded.get("AddressType"));
        assertEquals(List.of("Work", "Personal"), loaded.get("EmailType"));
    }

    @Test
    void savingReplacesEverythingSavedBefore() {
        store.saveAll(Map.of("AddressType", List.of("Residential"), "EmailType", List.of("Work")));

        store.saveAll(Map.of("AddressType", List.of("Postal")));

        assertEquals(Map.of("AddressType", List.of("Postal")), store.loadAll());
    }

    @Test
    void loadedValuesCannotBeChanged() {
        store.saveAll(Map.of("AddressType", List.of("Residential")));
        Map<String, List<String>> loaded = store.loadAll();

        assertThrows(UnsupportedOperationException.class, () -> loaded.get("AddressType").add("x"));
        assertThrows(UnsupportedOperationException.class, () -> loaded.put("x", List.of()));
    }

    @Test
    void databaseFailuresAreReported() throws Exception {
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE CategoryConstant");
        }

        assertThrows(LocalStoreException.class, () -> store.saveAll(Map.of("AddressType", List.of("Residential"))));
        assertThrows(LocalStoreException.class, () -> store.loadAll());
    }
}
