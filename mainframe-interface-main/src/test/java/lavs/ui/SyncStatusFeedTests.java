// AI-assisted.
package lavs.ui;

import lavs.logic.SyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SyncStatusFeedTests {
    @Mock
    SyncService sync;

    @Test
    void isOnlineWhenGatewayIsConnected() {
        when(sync.isConnected()).thenReturn(true);
        assertTrue(new SyncStatusFeed(sync).isOnline());
    }

    @Test
    void isOfflineWhenGatewayIsDisconnected() {
        when(sync.isConnected()).thenReturn(false);
        assertFalse(new SyncStatusFeed(sync).isOnline());
    }

    @Test
    void pendingCountDelegatesToSyncService() {
        when(sync.getPendingCount()).thenReturn(5);
        assertEquals(5, new SyncStatusFeed(sync).pendingCount());
    }

    @Test
    void zeroPendingCountDelegatesToSyncService() {
        when(sync.getPendingCount()).thenReturn(0);
        assertEquals(0, new SyncStatusFeed(sync).pendingCount());
    }

    @Test
    void onChangeRegistersListenerWithSyncService() {
        SyncStatusFeed feed = new SyncStatusFeed(sync);
        Runnable listener = () -> { };

        feed.onChange(listener);

        verify(sync).addListener(any(Runnable.class));
    }

    @Test
    void onChangePropagatesTheSameListenerToSyncService() {
        // capture what SyncService receives so we can confirm it is the same listener
        AtomicReference<Runnable> captured = new AtomicReference<>();
        org.mockito.Mockito.doAnswer(inv -> { captured.set(inv.getArgument(0)); return null; })
                .when(sync).addListener(any());

        AtomicReference<Boolean> ran = new AtomicReference<>(false);
        Runnable listener = () -> ran.set(true);
        new SyncStatusFeed(sync).onChange(listener);

        captured.get().run();
        assertTrue(ran.get());
    }
}
