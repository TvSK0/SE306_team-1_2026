package lavs.logic;

import lavs.data.LocalDatabase;
import lavs.data.MainframeGateway;
import lavs.data.SqlitePendingChangeStore;
import lavs.model.PendingChange;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. Phase 4 integration tests for dropout, restart and reconnect: the real
// SQLite file (in a temp folder), the real queue and SyncService, and the real gateway,
// with only the network replaced by a FakeConnection that a test can take offline.
//
// The sync thread is an executor this class owns, so a test can wait for it to finish
// (waitForSyncThread) before looking at what was sent: that keeps the checks free of
// timing guesses.
class SyncIntegrationTests {
    private static final Duration NEVER = Duration.ofHours(1);

    @TempDir
    Path folder;

    private final FakeConnection mainframe = new FakeConnection();
    private final List<SyncService> services = new ArrayList<>();
    private final List<ScheduledExecutorService> threads = new ArrayList<>();
    private SyncService sync;

    @BeforeEach
    void setUp() {
        sync = startApp();
    }

    @AfterEach
    void closeEverything() {
        // closed so Windows lets JUnit delete the temp folder
        services.forEach(SyncService::close);
    }

    // --- dropout ---

    @Test
    void aSaveMadeOfflineWaitsOnDiskInsteadOfFailing() throws Exception {
        mainframe.setIsConnected(false);

        SyncStep step = sync.queue("customer.addEmail", "C-1", null, fields("customerNumber", "C-1"))
                .get(5, TimeUnit.SECONDS);

        assertEquals(SyncStep.Outcome.WAITING, step.getOutcome());
        assertEquals(1, onDisk().size());
        assertEquals(1, sync.getPendingCount());
    }

    @Test
    void aDropoutHalfWayLeavesTheRestQueuedInOrder() throws Exception {
        mainframe.whenRequested("customer.addEmail", FakeConnection.success(Map.of("id", "1")));
        SyncStep first = sync.queue("customer.addEmail", "C-1", null, fields("address", "a@example.com"))
                .get(5, TimeUnit.SECONDS);

        mainframe.setIsConnected(false);
        sync.queue("customer.addEmail", "C-1", null, fields("address", "b@example.com")).get(5, TimeUnit.SECONDS);
        sync.queue("customer.addEmail", "C-1", null, fields("address", "c@example.com")).get(5, TimeUnit.SECONDS);

        assertEquals(SyncStep.Outcome.SENT, first.getOutcome());
        assertEquals(List.of("b@example.com", "c@example.com"),
                onDisk().stream().map(change -> change.getFields().get("address")).toList());
    }

    // --- reconnect ---

    @Test
    void reconnectingSendsWhatWasWaitingOldestFirst() throws Exception {
        mainframe.whenRequested("customer.addEmail", FakeConnection.success(Map.of("id", "1")));
        mainframe.setIsConnected(false);
        sync.queue("customer.addEmail", "C-1", null, fields("address", "a@example.com")).get(5, TimeUnit.SECONDS);
        sync.queue("customer.addEmail", "C-1", null, fields("address", "b@example.com")).get(5, TimeUnit.SECONDS);

        // while offline each new save tries the oldest one again, so only count from here
        int sentBefore = mainframe.getSentRequests().size();
        mainframe.setIsConnected(true);
        sync.start(NEVER);
        waitForSyncThread();

        assertTrue(onDisk().isEmpty());
        assertEquals(0, sync.getPendingCount());
        assertEquals(List.of("a@example.com", "b@example.com"), sentValues(sentBefore, "customer.addEmail", "address"));
    }

    @Test
    void aCustomerCreatedOfflineGetsItsRealNumberInEveryLaterChange() throws Exception {
        mainframe.setIsConnected(false);
        sync.queue("customer.create", null, "TMP-1", newCustomer()).get(5, TimeUnit.SECONDS);
        sync.queue("customer.addEmail", "TMP-1", null, fields("customerNumber", "TMP-1")).get(5, TimeUnit.SECONDS);
        mainframe.whenRequested("customer.search", searchResults());
        mainframe.whenRequested("customer.create", FakeConnection.success(Map.of("number", "C-77")));
        mainframe.whenRequested("customer.addEmail", FakeConnection.success(Map.of("id", "1")));

        mainframe.setIsConnected(true);
        sync.start(NEVER);
        waitForSyncThread();

        assertTrue(onDisk().isEmpty());
        assertEquals(List.of("C-77"), sentValues("customer.addEmail", "customerNumber"));
        assertEquals("C-77", sync.resolveNumber("TMP-1"));
    }

    @Test
    void aPossibleDuplicateIsHeldUntilTheMmmPicksTheExistingCustomer() throws Exception {
        mainframe.setIsConnected(false);
        sync.queue("customer.create", null, "TMP-1", newCustomer()).get(5, TimeUnit.SECONDS);
        sync.queue("customer.addEmail", "TMP-1", null, fields("customerNumber", "TMP-1")).get(5, TimeUnit.SECONDS);
        mainframe.whenRequested("customer.search", searchResults("C-10"));
        mainframe.whenRequested("customer.addEmail", FakeConnection.success(Map.of("id", "1")));

        mainframe.setIsConnected(true);
        sync.start(NEVER);
        waitForSyncThread();

        assertEquals(List.of(PendingChange.Status.PAUSED, PendingChange.Status.PAUSED),
                onDisk().stream().map(PendingChange::getStatus).toList());
        assertTrue(sentValues("customer.create", "firstName").isEmpty());

        sync.useExistingCustomer("TMP-1", "C-10").get(5, TimeUnit.SECONDS);

        assertTrue(onDisk().isEmpty());
        assertEquals(List.of("C-10"), sentValues("customer.addEmail", "customerNumber"));
        assertTrue(sentValues("customer.create", "firstName").isEmpty(), "the duplicate is never created");
    }

    // AI-assisted: the same swap for a loan made offline, e.g. a co-borrower added to it straight after
    @Test
    void aLoanCreatedOfflineGetsItsRealNumberInEveryLaterChange() throws Exception {
        mainframe.setIsConnected(false);
        sync.queue("loan.create", "C-1", "TMP-L1", fields("customerNumber", "C-1")).get(5, TimeUnit.SECONDS);
        Map<String, String> coborrower = fields("loanNumber", "TMP-L1");
        coborrower.put("coborrowerNumber", "C-2");
        sync.queue("loan.addCoborrower", "C-1", null, coborrower).get(5, TimeUnit.SECONDS);
        mainframe.whenRequested("loan.create", FakeConnection.success(Map.of("loanNumber", "L-55")));
        mainframe.whenRequested("loan.addCoborrower", FakeConnection.success(Map.of()));

        mainframe.setIsConnected(true);
        sync.start(NEVER);
        waitForSyncThread();

        assertTrue(onDisk().isEmpty());
        assertEquals(List.of("L-55"), sentValues("loan.addCoborrower", "loanNumber"));
        assertEquals(List.of("C-2"), sentValues("loan.addCoborrower", "coborrowerNumber"));
        assertEquals("L-55", sync.resolveNumber("TMP-L1"));
    }

    // AI-assisted: "This is a new customer" in the pending uploads panel. The matches are kept on
    // disk while paused, and once confirmed the customer is created without searching again.
    @Test
    void aPossibleDuplicateConfirmedAsNewIsCreatedWithoutSearchingAgain() throws Exception {
        mainframe.setIsConnected(false);
        sync.queue("customer.create", null, "TMP-1", newCustomer()).get(5, TimeUnit.SECONDS);
        sync.queue("customer.addEmail", "TMP-1", null, fields("customerNumber", "TMP-1")).get(5, TimeUnit.SECONDS);
        mainframe.whenRequested("customer.search", searchResults("C-10"));
        mainframe.whenRequested("customer.create", FakeConnection.success(Map.of("number", "C-77")));
        mainframe.whenRequested("customer.addEmail", FakeConnection.success(Map.of("id", "1")));

        // while offline each new save tries the search again, so only count from here
        int sentBefore = mainframe.getSentRequests().size();
        mainframe.setIsConnected(true);
        sync.start(NEVER);
        waitForSyncThread();

        assertEquals(List.of("C-10"), onDisk().get(0).getDuplicateMatches(), "kept for the panel");

        sync.confirmNewCustomer("TMP-1").get(5, TimeUnit.SECONDS);

        assertTrue(onDisk().isEmpty());
        assertEquals(List.of("Jane"), sentValues("customer.create", "firstName"));
        assertEquals(List.of("C-77"), sentValues("customer.addEmail", "customerNumber"));
        List<mainframe.client.MessageRequest> sent = mainframe.getSentRequests();
        assertEquals(1, sent.subList(sentBefore, sent.size()).stream()
                .filter(request -> "customer.search".equals(request.getRequestType())).count(), "searched once only");
    }

    // --- restart ---

    @Test
    void savesLeftOverWhenTheAppClosedAreSentAfterItStartsAgain() throws Exception {
        mainframe.setIsConnected(false);
        sync.queue("customer.addEmail", "C-1", null, fields("address", "a@example.com")).get(5, TimeUnit.SECONDS);
        sync.queue("customer.addEmail", "C-1", null, fields("address", "b@example.com")).get(5, TimeUnit.SECONDS);
        sync.close();

        // a fresh start: new store, gateway and sync thread, on the same file
        mainframe.whenRequested("customer.addEmail", FakeConnection.success(Map.of("id", "1")));
        int sentBefore = mainframe.getSentRequests().size();
        mainframe.setIsConnected(true);
        SyncService restarted = startApp();
        restarted.start(NEVER);
        waitForSyncThread();

        assertTrue(onDisk().isEmpty());
        assertEquals(List.of("a@example.com", "b@example.com"), sentValues(sentBefore, "customer.addEmail", "address"));
    }

    @Test
    void aRejectedChangeStaysRejectedAcrossARestartAndIsNeverRetried() throws Exception {
        mainframe.whenRequested("customer.addEmail", FakeConnection.rejected("email is not valid"));
        SyncStep step = sync.queue("customer.addEmail", "C-1", null, fields("address", "not-an-email"))
                .get(5, TimeUnit.SECONDS);
        sync.close();

        SyncService restarted = startApp();
        restarted.start(NEVER);
        waitForSyncThread();

        assertEquals(SyncStep.Outcome.REJECTED, step.getOutcome());
        PendingChange kept = onDisk().get(0);
        assertEquals(PendingChange.Status.REJECTED, kept.getStatus());
        assertEquals("email is not valid", kept.getStatusMessage());
        assertEquals(1, sentValues("customer.addEmail", "address").size(), "sent once, never again");
        assertEquals(0, restarted.getPendingCount());
    }

    // AI-assisted: a refused new customer holds back the changes that use it, across a restart too.
    // They are never sent, and dismissing the customer clears them with it.
    @Test
    void aRejectedNewCustomerKeepsItsChangesHeldAcrossARestart() throws Exception {
        mainframe.setIsConnected(false);
        sync.queue("customer.create", null, "TMP-1", newCustomer()).get(5, TimeUnit.SECONDS);
        sync.queue("customer.addEmail", "TMP-1", null, fields("customerNumber", "TMP-1")).get(5, TimeUnit.SECONDS);
        mainframe.whenRequested("customer.search", searchResults());
        mainframe.whenRequested("customer.create", FakeConnection.rejected("Unknown organisation"));
        mainframe.setIsConnected(true);
        sync.start(NEVER);
        waitForSyncThread();
        sync.close();

        SyncService restarted = startApp();
        restarted.start(NEVER);
        waitForSyncThread();

        List<PendingChange> kept = onDisk();
        assertEquals(List.of(PendingChange.Status.REJECTED, PendingChange.Status.REJECTED),
                kept.stream().map(PendingChange::getStatus).toList());
        assertEquals("Unknown organisation", kept.get(0).getStatusMessage());
        assertEquals(1, sentValues("customer.create", "firstName").size(), "sent once, never again");
        assertTrue(sentValues("customer.addEmail", "customerNumber").isEmpty(), "never sent with a temporary number");
        assertEquals(0, restarted.getPendingCount());

        restarted.dismissRejected(kept.get(0).getId()).get(5, TimeUnit.SECONDS);

        assertTrue(onDisk().isEmpty(), "dismissing the customer clears the change held behind it");
    }

    // --- helpers ---

    // what the app does at start-up: open the file, and build the queue, gateway and sync service on it
    private SyncService startApp() {
        SqlitePendingChangeStore store = new SqlitePendingChangeStore(new LocalDatabase(folder.resolve("lavs.db")));
        ScheduledExecutorService thread = Executors.newSingleThreadScheduledExecutor();
        threads.add(thread);
        SyncService service = new SyncService(store, new MainframeGateway(mainframe), thread);
        services.add(service);
        return service;
    }

    // everything handed to the sync threads so far has finished once this returns
    private void waitForSyncThread() throws Exception {
        for (ScheduledExecutorService thread : threads) {
            if (!thread.isShutdown()) {
                thread.submit(() -> { }).get(5, TimeUnit.SECONDS);
            }
        }
    }

    // read straight from the file, as a restarted app would
    private List<PendingChange> onDisk() {
        return new SqlitePendingChangeStore(new LocalDatabase(folder.resolve("lavs.db"))).all();
    }

    private List<String> sentValues(String requestType, String field) {
        return sentValues(0, requestType, field);
    }

    // the same, counting only requests sent after the first `from` of them
    private List<String> sentValues(int from, String requestType, String field) {
        List<mainframe.client.MessageRequest> sent = mainframe.getSentRequests();
        return sent.subList(from, sent.size()).stream()
                .filter(request -> requestType.equals(request.getRequestType()))
                .filter(request -> request.getValue(field) != null)
                .map(request -> request.getValue(field))
                .toList();
    }

    private static Map<String, String> fields(String name, String value) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put(name, value);
        return fields;
    }

    private static Map<String, String> newCustomer() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("firstName", "Jane");
        fields.put("familyName", "Smith");
        fields.put("dateOfBirth", "2001-02-03");
        return fields;
    }

    // a one-page customer.search reply; each number is a customer with newCustomer()'s details
    private static mainframe.client.MessageResponse searchResults(String... numbers) {
        HashMap<String, String> values = new HashMap<>();
        values.put("total", Integer.toString(numbers.length));
        values.put("items", Integer.toString(numbers.length));
        for (int i = 1; i <= numbers.length; i++) {
            values.put(i + ":number", numbers[i - 1]);
            values.put(i + ":firstName", "Jane");
            values.put(i + ":familyName", "Smith");
            values.put(i + ":dateOfBirth", "2001-02-03");
        }
        return FakeConnection.success(values);
    }
}
