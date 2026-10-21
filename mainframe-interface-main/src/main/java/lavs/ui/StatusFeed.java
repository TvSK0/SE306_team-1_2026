// AI-assisted: what the top bar needs to know about the connection and the upload queue.
package lavs.ui;

// ui-side view of the sync state, so the top bar does not import lavs.logic or lavs.data.
// The real adapter (over SyncService.getPendingCount()/addListener(...) plus the gateway's
// isConnected()) is written when the services are composed in LavsApp.
public interface StatusFeed {
    boolean isOnline();

    // changes saved on this laptop and not yet uploaded
    int pendingCount();

    // listener runs whenever online or the pending count may have changed. It can be called
    // from any thread (SyncService calls its listeners on the sync thread), so TopBar hops
    // back to the UI thread before touching the screen.
    void onChange(Runnable listener);
}
