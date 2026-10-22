package lavs.logic;

import lavs.data.ChangeQueue;
import lavs.data.MainframeGateway;
import lavs.model.PendingChange;
import mainframe.client.FakeConnection;
import mainframe.client.Message;
import mainframe.client.MessageRequest;
import mainframe.client.Status;
import mainframe.client.messages.customer.AddEmail;
import mainframe.client.messages.customer.DeleteAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted: tests written with Claude for the helper every service saves through (US11). A
// save is captured as exactly the request its message would send, queued, and later sent as
// that same request; reads and broken messages are refused.
class QueuedWritesTests {
    private static final String CUSTOMER = "C-1";

    private final FakeConnection connection = new FakeConnection();
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final SyncService sync = new SyncService(new InMemoryChangeQueue(), new MainframeGateway(connection),
            executor);

    @AfterEach
    void tearDown() {
        sync.close();
    }

    // --- capturing ---

    @Test
    void capturesTheRequestTypeAndEveryFieldTheMessageWouldSend() {
        QueuedWrites.Request request = QueuedWrites.capture(new DeleteAddress(CUSTOMER, 7));

        assertEquals("customer.deleteAddress", request.type);
        assertEquals(Map.of("customer", CUSTOMER, "id", "7"), request.fields);
        assertTrue(connection.getSentRequests().isEmpty(), "capturing sends nothing");
    }

    @Test
    void keepsAFieldTheMessageLeftEmpty() {
        QueuedWrites.Request request = QueuedWrites.capture(new AddEmail("jane@example.com", CUSTOMER, true, null,
                "Personal"));

        assertTrue(request.fields.containsKey("otherType"));
        assertNull(request.fields.get("otherType"));
        assertEquals("y", request.fields.get("isPrimary"));
    }

    @Test
    void refusesToQueueARead() {
        Message read = new mainframe.client.messages.customerAddress.Retrieve(7, CUSTOMER);

        assertThrows(IllegalArgumentException.class, () -> QueuedWrites.capture(read));
    }

    @Test
    void refusesAMessageThatSendsNothing() {
        Message silent = connection -> new Status(1);

        assertThrows(IllegalArgumentException.class, () -> QueuedWrites.capture(silent));
    }

    @Test
    void refusesAMessageThatSendsTwice() {
        Message twice = connection -> {
            connection.send(new MessageRequest(MessageRequest.UPDATE, "customer.addAddress"));
            connection.send(new MessageRequest(MessageRequest.UPDATE, "customer.addAddress"));
            return new Status(1);
        };

        assertThrows(IllegalStateException.class, () -> QueuedWrites.capture(twice));
    }

    @Test
    void reportsAMessageThatCouldNotBuildItsRequest() {
        Message broken = connection -> {
            throw new IOException("broken");
        };

        assertThrows(IllegalStateException.class, () -> QueuedWrites.capture(broken));
    }

    // --- queuing ---

    @Test
    void sendsTheQueuedSaveAsTheSameRequest() {
        connection.whenRequested("customer.deleteAddress", FakeConnection.success(Map.of("customer", CUSTOMER, "id", "7")));

        SyncStep step = QueuedWrites.queue(sync, new DeleteAddress(CUSTOMER, 7), CUSTOMER, null);

        assertEquals(SyncStep.Outcome.SENT, step.getOutcome());
        MessageRequest sent = connection.getSentRequests().get(0);
        assertEquals("customer.deleteAddress", sent.getRequestType());
        assertEquals(CUSTOMER, sent.getValue("customer"));
        assertEquals("7", sent.getValue("id"));
    }

    @Test
    void reportsASaveThatCouldNotBeKeptOnThisLaptop() {
        SyncService failing = new SyncService(queueThatCannotSave(), new MainframeGateway(connection), executor);

        SyncStep step = QueuedWrites.queue(failing, new DeleteAddress(CUSTOMER, 7), CUSTOMER, null);

        assertEquals(SyncStep.Outcome.REJECTED, step.getOutcome());
        assertTrue(step.getMessage().contains("disk full"));
        assertTrue(connection.getSentRequests().isEmpty());
    }

    // --- standing in for a mainframe Status, for the older result types ---

    @Test
    void aSentSaveStandsForASuccess() {
        SyncStep sent = new SyncStep(null, SyncStep.Outcome.SENT, null);

        assertTrue(QueuedWrites.statusFor(sent).getWasSuccessful());
        assertFalse(QueuedWrites.isWaiting(sent));
        assertEquals("kept", QueuedWrites.keptValue(sent, "kept"));
    }

    @Test
    void aRejectedSaveStandsForTheMainframeRefusingIt() {
        SyncStep rejected = new SyncStep(null, SyncStep.Outcome.REJECTED, "no such customer");

        Status status = QueuedWrites.statusFor(rejected);

        assertEquals(mainframe.client.StatusMessage.SERVER_FAILURE.getCode(), status.getErrorCode());
        assertEquals("no such customer", status.getErrorMessage());
        assertFalse(QueuedWrites.isWaiting(rejected));
        assertNull(QueuedWrites.keptValue(rejected, "dropped"));
    }

    @Test
    void aWaitingOrPausedSaveStandsForTheMainframeBeingUnreachable() {
        for (SyncStep.Outcome outcome : List.of(SyncStep.Outcome.WAITING, SyncStep.Outcome.PAUSED)) {
            SyncStep step = new SyncStep(null, outcome, "offline");

            assertEquals(mainframe.client.StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(),
                    QueuedWrites.statusFor(step).getErrorCode());
            assertTrue(QueuedWrites.isWaiting(step));
            assertEquals("kept", QueuedWrites.keptValue(step, "kept"));
        }
    }

    // a queue whose first save fails, like a local file that can't be written
    private static ChangeQueue queueThatCannotSave() {
        return new ChangeQueue() {
            @Override
            public PendingChange add(PendingChange change) {
                throw new RuntimeException("disk full");
            }

            @Override
            public Optional<PendingChange> next() {
                return Optional.empty();
            }

            @Override
            public List<PendingChange> all() {
                return List.of();
            }

            @Override
            public void update(PendingChange change) {
            }

            @Override
            public void markRejected(long id, String reason) {
            }

            @Override
            public void delete(long id) {
            }
        };
    }
}
