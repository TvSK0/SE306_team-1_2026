// AI-assisted.
package lavs.ui;

import lavs.logic.SyncService;

// The real StatusFeed: delegates online/pending state and change notifications to SyncService.
// Replaces FixedStatusFeed once the services are composed in LavsApp (see the TEMPORARY comment
// there). SyncService.isConnected() reads the last background check, so refreshing the
// top bar never pings the mainframe on the JavaFX thread.
public final class SyncStatusFeed implements StatusFeed {
    private final SyncService _sync;

    public SyncStatusFeed(SyncService sync) {
        _sync = sync;
    }

    @Override
    public boolean isOnline() {
        return _sync.isConnected();
    }

    @Override
    public int pendingCount() {
        return _sync.getPendingCount();
    }

    // Passes the listener straight to SyncService. The listener may be called from the sync
    // thread, so TopBar hops back to the JavaFX thread before touching the screen.
    @Override
    public void onChange(Runnable listener) {
        _sync.addListener(listener);
    }
}
