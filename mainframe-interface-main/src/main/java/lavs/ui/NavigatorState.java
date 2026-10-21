// AI-assisted: which screen is showing, its built view, and where Back goes, with no JavaFX.
package lavs.ui;

import java.util.ArrayDeque;
import java.util.Deque;

// The navigation rules, kept apart from the JavaFX Navigator so they can be unit tested.
// V is the view type: Navigator uses javafx.scene.Node, tests use strings.
//
// The caller builds the view first and only then calls goTo or resetTo with it, so a view
// that fails to build never changes the state. goTo pushes the current screen, with its
// view, onto the back stack, so Back shows that same view again (nothing typed or searched
// is lost). resetTo starts a fresh stack and drops the stored views: use it for Log in and
// Log out, so Back can never return to the login screen (or to a customer after logging out).
public final class NavigatorState<V> {
    private record Entry<V>(Screen screen, Object parameter, V view) {
    }

    private final Deque<Entry<V>> _backStack = new ArrayDeque<>();
    private Entry<V> _current;

    // view may be null for the first screen until Navigator.start() builds it and calls resetTo
    public NavigatorState(Screen first, Object parameter, V view) {
        _current = new Entry<>(first, parameter, view);
    }

    public Screen getScreen() {
        return _current.screen();
    }

    public Object getParameter() {
        return _current.parameter();
    }

    public V getView() {
        return _current.view();
    }

    public void goTo(Screen screen, Object parameter, V view) {
        _backStack.push(_current);
        _current = new Entry<>(screen, parameter, view);
    }

    public void resetTo(Screen screen, Object parameter, V view) {
        _backStack.clear();
        _current = new Entry<>(screen, parameter, view);
    }

    public boolean canGoBack() {
        return !_backStack.isEmpty();
    }

    // Returns false, and stays where it is, when there is nothing to go back to. Otherwise
    // the previous screen becomes current again with the view it already had (getView).
    public boolean back() {
        if (_backStack.isEmpty()) {
            return false;
        }
        _current = _backStack.pop();
        return true;
    }
}
