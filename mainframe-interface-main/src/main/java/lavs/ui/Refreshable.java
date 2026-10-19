// AI-assisted: lets a view reload itself when Back returns to it.
package lavs.ui;

// Optional. A view that implements this has onReturn() called each time the Navigator
// goes Back to it, e.g. the customer record reloads its counts after a loan was added.
// Views that do not implement it (search keeps its results) are simply shown again.
public interface Refreshable {
    void onReturn();

    // Calls onReturn() if the view supports it; does nothing otherwise.
    static void notifyReturned(Object view) {
        if (view instanceof Refreshable refreshable) {
            refreshable.onReturn();
        }
    }
}
