package lavs.ui;

import lavs.logic.SyncService;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// AI-assisted: one top bar follows the active login session without retaining old sessions.
public final class SessionStatusFeed implements StatusFeed {
    private volatile SyncService sync;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    public void attach(SyncService service) {
        sync = service;
        if (service != null) service.addListener(() -> { if (sync == service) changed(); });
        changed();
    }
    public boolean isOnline() { var current = sync; return current != null && current.isConnected(); }
    public int pendingCount() { var current = sync; return current == null ? 0 : current.getPendingCount(); }
    public void onChange(Runnable listener) { listeners.add(listener); }
    private void changed() { listeners.forEach(Runnable::run); }
}
