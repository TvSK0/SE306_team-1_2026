// AI-assisted: a stand-in StatusFeed that always reports the same thing.
package lavs.ui;

// TEMPORARY: swap this for an adapter over SyncService (pending count and listener) and the
// gateway's isConnected() once the services are composed in LavsApp. Until then the top bar
// shows "Online" / "All changes uploaded" (online, 0 waiting) and never changes.
public final class FixedStatusFeed implements StatusFeed {
    private final boolean _online;
    private final int _pending;

    public FixedStatusFeed(boolean online, int pending) {
        _online = online;
        _pending = pending;
    }

    @Override
    public boolean isOnline() {
        return _online;
    }

    @Override
    public int pendingCount() {
        return _pending;
    }

    // never changes, so there is nothing to tell the listener about
    @Override
    public void onChange(Runnable listener) {
    }
}
