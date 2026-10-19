// AI-assisted: which screen is showing, its built view, and where Back goes, with no JavaFX.
package lavs.ui;

import java.util.ArrayDeque;
import java.util.Deque;

// The navigation rules, kept apart from the JavaFX Navigator so they can be unit tested.
// V is the view type: Navigator uses javafx.scene.Node, tests use strings.
//
// goTo pushes the current screen, with the view already built for it, onto the back stack,
// so Back shows that same view again (nothing typed or searched is lost). resetTo starts a
// fresh stack and drops the stored views: use it for Log in and Log out, so Back can never
// return to the login screen (or to a customer after logging out).
public final class NavigatorState<V> {
    private record Entry<V>(Screen screen, Object parameter, V view) {
    }

    private final Deque<Entry<V>> _backStack = new ArrayDeque<>();
    private Entry<V> _current;

    public NavigatorState(Screen first, Object parameter) {
        _current = new Entry<>(first, parameter, null);
    }

    public Screen getScreen() {
        return _current.screen();
    }

    public Object getParameter() {
        return _current.parameter();
    }

    // The view built for the current screen. Null until setView is called.
    public V getView() {
        return _current.view();
    }

    // Called by the Navigator once it has built the view for the current screen.
    public void setView(V view) {
        _current = new Entry<>(_current.screen(), _current.parameter(), view);
    }

    // Moves to a new screen. The caller builds its view and passes it to setView.
    public void goTo(Screen screen, Object parameter) {
        _backStack.push(_current);
        _current = new Entry<>(screen, parameter, null);
    }

    public void resetTo(Screen screen, Object parameter) {
        _backStack.clear();
        _current = new Entry<>(screen, parameter, null);
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
