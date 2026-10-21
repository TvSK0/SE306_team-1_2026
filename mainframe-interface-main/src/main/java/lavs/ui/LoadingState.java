// AI-assisted: what a button, its Cancel button and the table show while a call is running.
package lavs.ui;

// The rules for the loading look (Figma frame 12), free of JavaFX so they can be unit tested;
// LoadingView just copies them onto the controls.
//
// A read (Search) shows a Cancel button and skeleton rows while it runs. A save does not:
// saves go through SyncService.queue and still upload, so cancelling would only hide it.
public final class LoadingState {
    private final String _idleText;
    private final String _busyText;
    private final boolean _cancellable;
    private boolean _loading;

    private LoadingState(String idleText, String busyText, boolean cancellable) {
        _idleText = idleText;
        _busyText = busyText;
        _cancellable = cancellable;
    }

    // e.g. forRead("Search", "Searching...")
    public static LoadingState forRead(String idleText, String busyText) {
        return new LoadingState(idleText, busyText, true);
    }

    // e.g. forSave("Save document", "Saving...")
    public static LoadingState forSave(String idleText, String busyText) {
        return new LoadingState(idleText, busyText, false);
    }

    public void start() {
        _loading = true;
    }

    public void finish() {
        _loading = false;
    }

    public boolean isLoading() {
        return _loading;
    }

    public String getButtonText() {
        return _loading ? _busyText : _idleText;
    }

    // the button cannot be pressed again while its call is running
    public boolean isButtonDisabled() {
        return _loading;
    }

    // true while a read is running: the Cancel button and the placeholder rows are shown.
    // A save has neither, so this is always false for one.
    public boolean isReadInProgress() {
        return _loading && _cancellable;
    }
}
