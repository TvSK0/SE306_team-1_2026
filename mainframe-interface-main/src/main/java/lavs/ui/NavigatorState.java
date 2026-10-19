// AI-assisted: which screen is showing and where Back goes, with no JavaFX.
package lavs.ui;

import java.util.ArrayDeque;
import java.util.Deque;

// The navigation rules, kept apart from the JavaFX Navigator so they can be unit tested.
//
// goTo pushes the current screen onto the back stack, so Back returns to it with the
// parameter it had. resetTo starts a fresh stack: use it for Log in and Log out, so Back
// can never return to the login screen (or to a customer after logging out).
public final class NavigatorState {
    private record Entry(Screen screen, Object parameter) {
    }

    private final Deque<Entry> _backStack = new ArrayDeque<>();
    private Entry _current;

    public NavigatorState(Screen first, Object parameter) {
        _current = new Entry(first, parameter);
    }

    public Screen getScreen() {
        return _current.screen();
    }

    public Object getParameter() {
        return _current.parameter();
    }

    public void goTo(Screen screen, Object parameter) {
        _backStack.push(_current);
        _current = new Entry(screen, parameter);
    }

    public void resetTo(Screen screen, Object parameter) {
        _backStack.clear();
        _current = new Entry(screen, parameter);
    }

    public boolean canGoBack() {
        return !_backStack.isEmpty();
    }

    // Returns false, and stays where it is, when there is nothing to go back to.
    public boolean back() {
        if (_backStack.isEmpty()) {
            return false;
        }
        _current = _backStack.pop();
        return true;
    }
}
