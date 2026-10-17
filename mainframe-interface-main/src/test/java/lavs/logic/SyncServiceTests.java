package lavs.logic;

import lavs.data.ChangeQueue;
import lavs.data.DuplicateCheckResult;
import lavs.data.LocalDatabase;
import lavs.data.SendResult;
import lavs.data.SqlitePendingChangeStore;
import lavs.model.PendingChange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

// AI-assisted: tests generated with Claude (team-approved).
class SyncServiceTests {

    private static final String TEMP = "TMP-1";

    @TempDir
    Path folder;

    private ChangeQueue queue;
    private FakeUploadGateway gateway;
    private SyncService service;

    @BeforeEach
    void setUp() {
        queue = new SqlitePendingChangeStore(new LocalDatabase(folder.resolve("lavs.db")));
        gateway = new FakeUploadGateway();
        service = SyncService.create(queue, gateway);
    }

    @AfterEach
    void tearDown() {
        service.close();
    }

    private static <T> T await(CompletableFuture<T> future) throws Exception {
        return future.get(5, TimeUnit.SECONDS);
    }

    private SyncStep queueAddress(String customer) throws Exception {
        return await(service.queue("customer.addAddress", customer, null, Map.of("customer", customer)));
    }

    private static void eventually(BooleanSupplier condition) throws InterruptedException {
        for (int i = 0; i < 100; i++) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(50);
        }
        fail("condition not met in time");
    }

    @Test
    void onlineSaveIsSentStraightAway() throws Exception {
        AtomicInteger notified = new AtomicInteger();
        service.addListener(notified::incrementAndGet);

        SyncStep step = queueAddress("C1");

        assertEquals(SyncStep.Outcome.SENT, step.getOutcome());
        assertEquals(List.of("customer.addAddress"), gateway.sent);
        assertEquals(0, service.getPendingCount());
        assertTrue(notified.get() >= 2);
    }

    @Test
    void offlineSavesWaitAndKeepTheirOrder() throws Exception {
        gateway.offline = true;

        SyncStep first = queueAddress("C1");
        SyncStep second = await(service.queue("customer.addEmail", "C1", null, Map.of()));

        assertEquals(SyncStep.Outcome.WAITING, first.getOutcome());
        assertEquals(SyncStep.Outcome.WAITING, second.getOutcome());
        assertEquals(2, service.getPendingCount());
        assertEquals("customer.addAddress", service.getChanges().get(0).getMessageType());
    }

    @Test
    void retrySendsOnlyOnceConnectionIsBack() throws Exception {
        gateway.offline = true;
        queueAddress("C1");
        gateway.offline = false;
        gateway.connected = false;

        service.retryIfConnected();
        assertEquals(1, service.getPendingCount());

        gateway.connected = true;
        service.retryIfConnected();
        assertEquals(0, service.getPendingCount());
        assertEquals(List.of("customer.addAddress"), gateway.sent);
    }

    @Test
    void startSendsWhatWasLeftAndRetriesOnSchedule() throws Exception {
        queue.add(new PendingChange("customer.addAddress", "C1", null, Map.of()));
        gateway.offline = true;
        gateway.connected = false;

        service.start(Duration.ofMillis(50));
        Thread.sleep(150);
        assertEquals(1, service.getPendingCount());

        gateway.offline = false;
        gateway.connected = true;
        eventually(() -> service.getPendingCount() == 0);
        assertEquals(List.of("customer.addAddress"), gateway.sent);
    }

    @Test
    void rejectedChangeIsReportedThenDismissed() throws Exception {
        gateway.answerSend(SendResult.rejected("Bad postcode"));
        SyncStep step = queueAddress("C1");
        gateway.offline = true;
        SyncStep waiting = queueAddress("C2");

        assertEquals(SyncStep.Outcome.REJECTED, step.getOutcome());
        assertEquals("Bad postcode", step.getMessage());
        assertEquals(1, service.getPendingCount());

        await(service.dismissRejected(waiting.getChange().getId()));
        await(service.dismissRejected(999));
        assertEquals(2, service.getChanges().size());

        await(service.dismissRejected(step.getChange().getId()));
        assertEquals(1, service.getChanges().size());
    }

    @Test
    void duplicateCustomerIsPausedUntilConfirmedNew() throws Exception {
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        SyncStep create = await(service.queue("customer.create", TEMP, TEMP, Map.of("firstName", "Aroha")));
        SyncStep address = queueAddress(TEMP);

        assertEquals(SyncStep.Outcome.PAUSED, create.getOutcome());
        assertEquals(SyncStep.Outcome.PAUSED, address.getOutcome());
        assertEquals(2, service.getPendingCount());

        gateway.answerSend(SendResult.success("C80"));
        await(service.confirmNewCustomer(TEMP));

        assertEquals(List.of("customer.create", "customer.addAddress"), gateway.sent);
        assertEquals("C80", gateway.sentFields.get(1).get("customer"));
        assertEquals("C80", service.resolveNumber(TEMP));
        assertEquals(0, service.getPendingCount());
    }

    @Test
    void duplicateCustomerCanBeSwappedForTheExistingOne() throws Exception {
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        await(service.queue("customer.create", TEMP, TEMP, Map.of()));
        queueAddress(TEMP);

        await(service.useExistingCustomer(TEMP, "C5"));

        assertEquals(List.of("customer.addAddress"), gateway.sent);
        assertEquals("C5", gateway.sentFields.get(0).get("customer"));
        assertTrue(service.getChanges().isEmpty());
    }

    @Test
    void failureToSaveLocallyFailsTheFuture() {
        ChangeQueue broken = new InMemoryChangeQueue() {
            @Override
            public synchronized PendingChange add(PendingChange change) {
                throw new IllegalStateException("disk full");
            }
        };
        try (SyncService brokenService = SyncService.create(broken, gateway)) {
            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> await(brokenService.queue("customer.addEmail", "C1", null, Map.of())));
            assertInstanceOf(IllegalStateException.class, error.getCause());
        }
    }

    @Test
    void unexpectedErrorWhileSendingIsRecorded() throws Exception {
        RuntimeException boom = new IllegalStateException("boom");
        FakeUploadGateway throwing = new FakeUploadGateway() {
            @Override
            public synchronized SendResult send(PendingChange change) {
                throw boom;
            }
        };
        try (SyncService throwingService = SyncService.create(queue, throwing)) {
            assertNull(throwingService.getLastError());
            CompletableFuture<SyncStep> future = throwingService.queue("customer.addEmail", "C1", null, Map.of());
            eventually(() -> throwingService.getLastError() != null);
            assertSame(boom, throwingService.getLastError());
            assertTrue(!future.isDone());
        }
    }

    @Test
    void failingResolutionFailsItsFuture() {
        ChangeQueue broken = new InMemoryChangeQueue() {
            @Override
            public synchronized List<PendingChange> all() {
                throw new IllegalStateException("disk error");
            }

            @Override
            public synchronized Optional<PendingChange> next() {
                return Optional.empty();
            }
        };
        try (SyncService brokenService = SyncService.create(broken, gateway)) {
            assertThrows(ExecutionException.class, () -> await(brokenService.confirmNewCustomer(TEMP)));
        }
    }
}
