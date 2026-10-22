// AI-assisted.
package lavs.ui;

import lavs.logic.SyncService;

// The real StatusFeed: delegates online/pending state and change notifications to SyncService.
// Replaces FixedStatusFeed once the services are composed in LavsApp (see the TEMPORARY comment
// there). The gateway's isConnected() is exposed through SyncService.isConnected() so this class
// only imports from lavs.logic.
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
