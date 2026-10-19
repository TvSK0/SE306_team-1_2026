// AI-assisted: swaps the centre content of the window when the screen changes.
package lavs.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;
import java.util.function.Function;

// Thin control: the rules are in NavigatorState and ScreenRegistry (unit tested).
// Teammates call register(Screen, factory) once at start-up, then views call goTo, resetTo and back.
// goTo and resetTo build a new view first; back shows the view that was built for that screen before.
// If a factory throws, the error is printed to stderr and the current screen stays as it was.
public class Navigator {
    private final NavigatorState<Node> _state;
    private final ScreenRegistry<Node> _registry = new ScreenRegistry<>(Navigator::placeholder);
    private final StackPane _content = new StackPane();
    private Consumer<Screen> _onScreenChanged = screen -> { };

    public Navigator(Screen first) {
        _state = new NavigatorState<>(first, null, null);
    }

    // the node MainWindow puts in its centre
    public StackPane getContent() {
        return _content;
    }

    public void register(Screen screen, Function<Object, Node> factory) {
        _registry.register(screen, factory);
    }

    // MainWindow uses this to update the top bar title and hide the top bar on Login
    public void setOnScreenChanged(Consumer<Screen> listener) {
        _onScreenChanged = listener;
    }

    // Shows the first screen. Call after every screen has been registered.
    public void start() {
        resetTo(_state.getScreen(), null);
    }

    public void goTo(Screen screen, Object parameter) {
        _registry.tryCreate(screen, parameter, Throwable::printStackTrace).ifPresent(view -> {
            _state.goTo(screen, parameter, view);
            show();
        });
    }

    public void resetTo(Screen screen, Object parameter) {
        _registry.tryCreate(screen, parameter, Throwable::printStackTrace).ifPresent(view -> {
            _state.resetTo(screen, parameter, view);
            show();
        });
    }

    // Shows the previous screen's stored view and tells it, if it is Refreshable.
    // With nothing to go back to, the current screen just stays.
    public void back() {
        if (_state.back()) {
            show();
            Refreshable.notifyReturned(_state.getView());
        }
    }

    public boolean canGoBack() {
        return _state.canGoBack();
    }

    private void show() {
        _content.getChildren().setAll(_state.getView());
        _onScreenChanged.accept(_state.getScreen());
    }

    private static Node placeholder(Screen screen) {
        Label label = new Label("Not built yet: " + screen.name());
        label.getStyleClass().add("lavs-subtitle");
        return label;
    }
}
