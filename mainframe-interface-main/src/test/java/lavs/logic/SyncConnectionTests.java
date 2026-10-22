package lavs.logic;

import lavs.data.SendResult;
import lavs.data.UploadGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI-assisted: regressions for connection checks blocking the UI and stopping retries.
class SyncConnectionTests {
    @Test
    void readingStatusDoesNotCallTheGateway() {
        UploadGateway gateway = mock(UploadGateway.class);
        SyncService service = new SyncService(new InMemoryChangeQueue(), gateway,
                mock(ScheduledExecutorService.class));

        assertFalse(service.isConnected());
        assertFalse(service.isConnected());
        verifyNoInteractions(gateway);
    }

    @Test
    void emptyQueueStillReportsConnectionChanges() {
        UploadGateway gateway = mock(UploadGateway.class);
        when(gateway.isConnected()).thenReturn(true, true, false);
        SyncService service = new SyncService(new InMemoryChangeQueue(), gateway,
                mock(ScheduledExecutorService.class));
        AtomicInteger changes = new AtomicInteger();
        service.addListener(changes::incrementAndGet);

        service.retryIfConnected();
        assertTrue(service.isConnected());
        assertEquals(1, changes.get());
        service.retryIfConnected();
        assertEquals(1, changes.get());
        service.retryIfConnected();
        assertFalse(service.isConnected());
        assertEquals(2, changes.get());
        verify(gateway, times(3)).isConnected();
    }

    @Test
    void failedProbeDoesNotPreventLaterRetries() {
        UploadGateway gateway = mock(UploadGateway.class);
        RuntimeException failure = new IllegalStateException("connection unavailable");
        when(gateway.isConnected()).thenReturn(true).thenThrow(failure).thenReturn(true);
        SyncService service = new SyncService(new InMemoryChangeQueue(), gateway,
                mock(ScheduledExecutorService.class));

        service.retryIfConnected();
        assertTrue(service.isConnected());
        assertDoesNotThrow(service::retryIfConnected);
        assertFalse(service.isConnected());
        assertSame(failure, service.getLastError());
        service.retryIfConnected();
        assertTrue(service.isConnected());
    }

    @Test
    void startupDefersTheFirstProbeToTheExecutor() {
        UploadGateway gateway = mock(UploadGateway.class);
        when(gateway.isConnected()).thenReturn(true);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        SyncService service = new SyncService(new InMemoryChangeQueue(), gateway, executor);

        service.start(Duration.ofSeconds(10));

        verifyNoInteractions(gateway);
        ArgumentCaptor<Runnable> initial = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).execute(initial.capture());
        initial.getValue().run();
        assertTrue(service.isConnected());
        verify(executor).scheduleWithFixedDelay(any(Runnable.class), eq(10000L), eq(10000L),
                eq(TimeUnit.MILLISECONDS));
    }

    @Test
    void uploadResultsUpdateStatusBeforeCompletingTheFuture() throws Exception {
        FakeUploadGateway gateway = new FakeUploadGateway();
        gateway.answerSend(SendResult.success(null));
        gateway.answerSend(SendResult.networkFailure("offline"));
        try (SyncService service = SyncService.create(new InMemoryChangeQueue(), gateway)) {
            assertEquals(SyncStep.Outcome.SENT,
                    service.queue("customer.addEmail", "C1", null, Map.of()).get(5, TimeUnit.SECONDS).getOutcome());
            assertTrue(service.isConnected());
            assertEquals(SyncStep.Outcome.WAITING,
                    service.queue("customer.addEmail", "C1", null, Map.of()).get(5, TimeUnit.SECONDS).getOutcome());
            assertFalse(service.isConnected());
        }
    }
}
