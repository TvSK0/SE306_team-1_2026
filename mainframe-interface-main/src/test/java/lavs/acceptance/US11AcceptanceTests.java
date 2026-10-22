package lavs.acceptance;

import lavs.data.MainframeGateway;
import lavs.logic.EmailInput;
import lavs.logic.EmailService;
import lavs.logic.ServiceResult;
import lavs.logic.SyncService;
import lavs.logic.SyncSessions;
import lavs.model.Email;
import lavs.model.PendingChange;
import lavs.ui.ResultText;
import lavs.ui.SyncStatusFeed;
import lavs.ui.TopBarState;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. US11 "Losing the connection": one test per GIVEN/WHEN/THEN in the proposal. The
// app's own wiring (SyncSessions: a per-user SQLite file, the upload queue and the gateway) runs
// on a real file in a temp folder; only the mainframe is a FakeConnection that can go offline.
@DisplayName("US11: Losing the connection")
class US11AcceptanceTests {
    private static final String USER = "jsmith";
    private static final String CUSTOMER = "C-10234";

    @TempDir
    Path laptop;

    private final FakeConnection mainframe = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(mainframe);
    private SyncService sync;

    @AfterEach
    void closeTheApp() {
        // closed so Windows lets JUnit delete the temp folder
        if (sync != null) {
            sync.close();
        }
    }

    @Test
    @DisplayName("GIVEN the connection has dropped, WHEN the MMM saves a change, "
            + "THEN the change is kept on the laptop and the application shows that it is waiting to upload")
    void aChangeSavedOfflineIsKeptAndShownAsWaiting() {
        sync = signIn();
        mainframe.setIsConnected(false);

        ServiceResult<Email> saved = saveEmail("jane@example.com");

        assertEquals(ServiceResult.Outcome.WAITING, saved.getOutcome());
        assertEquals("Saved locally. Waiting to upload.", ResultText.forChange(saved, "Email added."));
        assertEquals(1, sync.getChanges().size(), "kept on the laptop");
        assertEquals("1 waiting to upload", new TopBarState(new SyncStatusFeed(sync)).getStatusDetail());
    }

    @Test
    @DisplayName("GIVEN there are changes waiting, WHEN the application is closed and reopened, "
            + "THEN they are still there")
    void waitingChangesSurviveClosingAndReopening() throws Exception {
        sync = signIn();
        mainframe.setIsConnected(false);
        saveEmail("jane@example.com");
        saveEmail("jane.smith@example.com");

        sync.close();
        sync = signIn();
        waitUntil(() -> sync.getPendingCount() == 2, Duration.ofSeconds(10));

        List<PendingChange> waiting = sync.getChanges();
        assertEquals(List.of("jane@example.com", "jane.smith@example.com"),
                waiting.stream().map(change -> change.getFields().get("address")).toList());
        assertEquals(2, sync.getPendingCount());
    }

    @Test
    @DisplayName("GIVEN the connection comes back, WHEN the application notices, "
            + "THEN the waiting changes are uploaded automatically, in the order they were made")
    void waitingChangesUploadByThemselvesInOrderOnceBackOnline() throws Exception {
        sync = signIn();
        mainframe.setIsConnected(false);
        saveEmail("first@example.com");
        saveEmail("second@example.com");
        saveEmail("third@example.com");
        mainframe.whenRequested("customer.addEmail", FakeConnection.success(Map.of(
                "id", "1", "type", "Personal", "address", "x@example.com", "isPrimary", "n")));
        // only what goes out after the connection is back counts as uploaded
        int sentWhileOffline = mainframe.getSentRequests().size();

        // the app checks the connection in the background; nobody presses anything
        mainframe.setIsConnected(true);
        waitUntil(() -> sync.getPendingCount() == 0, Duration.ofSeconds(10));

        List<MessageRequest> sent = mainframe.getSentRequests();
        assertEquals(List.of("first@example.com", "second@example.com", "third@example.com"),
                sent.subList(sentWhileOffline, sent.size()).stream()
                        .filter(request -> "customer.addEmail".equals(request.getRequestType()))
                        .map(request -> request.getValue("address")).toList());
        assertTrue(sync.getChanges().isEmpty());
    }

    // --- helpers ---

    // what signing in does in LavsApp: open this user's queue on the laptop, route saves through
    // it, and start the background thread that keeps checking the connection
    private SyncService signIn() {
        SyncService opened = SyncSessions.open(laptop, USER, gateway);
        opened.start(Duration.ofMillis(50));
        return opened;
    }

    private ServiceResult<Email> saveEmail(String address) {
        return new EmailService(gateway).addEmail(CUSTOMER, new EmailInput("Personal", "", address, false));
    }

    private static void waitUntil(java.util.function.BooleanSupplier done, Duration limit) throws InterruptedException {
        long deadline = System.nanoTime() + limit.toNanos();
        while (!done.getAsBoolean()) {
            assertTrue(System.nanoTime() < deadline, "timed out waiting");
            Thread.sleep(20);
        }
    }
}
