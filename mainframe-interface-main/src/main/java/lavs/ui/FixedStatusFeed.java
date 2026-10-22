// AI-assisted: a stand-in StatusFeed that always reports the same thing.
package lavs.ui;

// Only for the developer demo screen (DemoScreen), which shows the top bar without a real
// SyncService; the app itself uses SessionStatusFeed. It always reports the same thing.
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
