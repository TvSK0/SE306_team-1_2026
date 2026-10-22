package lavs.data;

import lavs.model.PendingChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class SqlitePendingChangeStoreTests {

    @TempDir
    Path folder;

    private LocalDatabase database;
    private SqlitePendingChangeStore store;

    @BeforeEach
    void setUp() {
        database = new LocalDatabase(folder.resolve("lavs.db"));
        store = new SqlitePendingChangeStore(database);
    }

    private static PendingChange change(String type, String customer) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("customer", customer);
        fields.put("city", "Auckland");
        return new PendingChange(type, customer, null, fields);
    }

    @Test
    void addAssignsIdsInCreationOrder() {
        PendingChange first = store.add(change("customer.addAddress", "C1"));
        PendingChange second = store.add(change("customer.addEmail", "C1"));

        assertNotNull(first.getId());
        assertTrue(second.getId() > first.getId());
    }

    @Test
    void savedChangeReadsBackTheSame() {
        PendingChange saved = new PendingChange("loan.create", "TMP-1", "TMP-L", Map.of("principal", "1000"));
        saved.setCreatedAt(Instant.parse("2026-10-18T01:02:03Z"));
        saved.setSkipDuplicateCheck(true);
        store.add(saved);

        PendingChange read = store.all().get(0);

        assertAll(
                () -> assertEquals(saved.getId(), read.getId()),
                () -> assertEquals("loan.create", read.getMessageType()),
                () -> assertEquals("TMP-1", read.getCustomerNumber()),
                () -> assertEquals("TMP-L", read.getCreatesNumber()),
                () -> assertEquals(Map.of("principal", "1000"), read.getFields()),
                () -> assertEquals(Instant.parse("2026-10-18T01:02:03Z"), read.getCreatedAt()),
                () -> assertEquals(PendingChange.Status.PENDING, read.getStatus()),
                () -> assertNull(read.getStatusMessage()),
                () -> assertTrue(read.isSkipDuplicateCheck()));
    }

    @Test
    void fieldOrderAndNullValuesSurvive() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("line1", "1 Queen St");
        fields.put("line2", null);
        store.add(new PendingChange("customer.addAddress", "C1", null, fields));

        Map<String, String> read = store.all().get(0).getFields();

        assertEquals("1 Queen St", read.get("line1"));
        assertTrue(read.containsKey("line2"));
        assertNull(read.get("line2"));
    }

    @Test
    void nextIsEmptyWhenQueueIsEmpty() {
        assertTrue(store.next().isEmpty());
    }

    @Test
    void nextReturnsOldestPendingAndSkipsPausedAndRejected() {
        PendingChange rejected = store.add(change("customer.addAddress", "C1"));
        PendingChange paused = store.add(change("customer.addEmail", "C2"));
        PendingChange pending = store.add(change("customer.addEmail", "C3"));
        store.markRejected(rejected.getId(), "Bad postcode");
        paused.setStatus(PendingChange.Status.PAUSED);
        store.update(paused);

        assertEquals(pending.getId(), store.next().orElseThrow().getId());
    }

    @Test
    void markRejectedKeepsTheChangeWithItsReason() {
        PendingChange added = store.add(change("customer.addAddress", "C1"));

        store.markRejected(added.getId(), "Bad postcode");

        PendingChange read = store.all().get(0);
        assertEquals(PendingChange.Status.REJECTED, read.getStatus());
        assertEquals("Bad postcode", read.getStatusMessage());
    }

    @Test
    void updateSavesNumberFieldsStatusAndFlag() {
        PendingChange added = store.add(change("customer.addAddress", "TMP-1"));
        added.replaceNumber("TMP-1", "C9");
        added.setStatus(PendingChange.Status.PAUSED);
        added.setStatusMessage("Possible duplicate");
        added.setSkipDuplicateCheck(true);

        store.update(added);

        PendingChange read = store.all().get(0);
        assertAll(
                () -> assertEquals("C9", read.getCustomerNumber()),
                () -> assertEquals("C9", read.getFields().get("customer")),
                () -> assertEquals(PendingChange.Status.PAUSED, read.getStatus()),
                () -> assertEquals("Possible duplicate", read.getStatusMessage()),
                () -> assertTrue(read.isSkipDuplicateCheck()));
    }

    @Test
    void updateCanClearTheDuplicateFlag() {
        PendingChange added = change("customer.create", null);
        added.setSkipDuplicateCheck(true);
        store.add(added);
        added.setSkipDuplicateCheck(false);

        store.update(added);

        assertFalse(store.all().get(0).isSkipDuplicateCheck());
    }

    @Test
    void deleteRemovesOnlyThatChange() {
        PendingChange first = store.add(change("customer.addAddress", "C1"));
        PendingChange second = store.add(change("customer.addEmail", "C1"));

        store.delete(first.getId());

        List<PendingChange> left = store.all();
        assertEquals(1, left.size());
        assertEquals(second.getId(), left.get(0).getId());
    }

    @Test
    void everyOperationReportsADatabaseFailure() throws Exception {
        PendingChange added = store.add(change("customer.addAddress", "C1"));
        try (Connection connection = database.connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE PendingChange");
        }

        assertAll(
                () -> assertThrows(LocalStoreException.class, () -> store.add(change("customer.addEmail", "C1"))),
                () -> assertThrows(LocalStoreException.class, () -> store.next()),
                () -> assertThrows(LocalStoreException.class, () -> store.all()),
                () -> assertThrows(LocalStoreException.class, () -> store.update(added)),
                () -> assertThrows(LocalStoreException.class, () -> store.markRejected(1, "x")),
                () -> assertThrows(LocalStoreException.class, () -> store.delete(1)));
    }

    @Test
    void emptyFieldsAreAllowed() {
        store.add(new PendingChange("customer.create", null, "TMP-1", new HashMap<>()));

        assertTrue(store.all().get(0).getFields().isEmpty());
    }

    // AI-assisted: the customers a paused new customer may be survive a restart (US11)
    @Test
    void keepsTheDuplicateMatchesOfAPausedCustomer() {
        PendingChange create = store.add(new PendingChange("customer.create", "TMP-1", "TMP-1", new HashMap<>()));
        assertTrue(store.all().get(0).getDuplicateMatches().isEmpty());

        create.setStatus(PendingChange.Status.PAUSED);
        create.setDuplicateMatches(List.of("C-5", "C-6"));
        store.update(create);

        assertEquals(List.of("C-5", "C-6"), new SqlitePendingChangeStore(database).all().get(0).getDuplicateMatches());
    }

    // AI-assisted: a file from before the matches were kept gets the column, and its rows have none (US11)
    @Test
    void opensAFileFromBeforeTheDuplicateMatchesWereKept() throws Exception {
        Path old = folder.resolve("old.db");
        try (Connection connection = java.sql.DriverManager.getConnection("jdbc:sqlite:" + old.toAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(LocalDatabase.CREATE_PENDING_CHANGE.replace(", duplicateMatches TEXT", ""));
            statement.executeUpdate("INSERT INTO PendingChange (createdAt, messageType, customerNumber, fields, status)"
                    + " VALUES ('2026-10-22T10:00:00Z', 'customer.addEmail', 'C-1', '{}', 'PENDING')");
        }

        SqlitePendingChangeStore reopened = new SqlitePendingChangeStore(new LocalDatabase(old));

        assertTrue(reopened.all().get(0).getDuplicateMatches().isEmpty());
        PendingChange added = reopened.add(new PendingChange("customer.create", "TMP-2", "TMP-2", new HashMap<>()));
        added.setDuplicateMatches(List.of("C-7"));
        reopened.update(added);
        assertEquals(List.of("C-7"), reopened.all().get(1).getDuplicateMatches());
    }
}
