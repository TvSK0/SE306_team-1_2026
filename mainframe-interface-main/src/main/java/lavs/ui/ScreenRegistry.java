// AI-assisted: which view builds each screen, with a placeholder for screens nobody has built yet.
package lavs.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

// Generic over the view type N so it can be unit tested with plain strings; the app uses
// ScreenRegistry<javafx.scene.Node> through Navigator.
public final class ScreenRegistry<N> {
    private final Map<Screen, Function<Object, N>> _factories = new EnumMap<>(Screen.class);
    private final Function<Screen, N> _placeholder;

    // placeholder builds the "Not built yet" view for a screen with no factory
    public ScreenRegistry(Function<Screen, N> placeholder) {
        _placeholder = placeholder;
    }

    // Registering a screen again replaces the earlier factory.
    public void register(Screen screen, Function<Object, N> factory) {
        _factories.put(screen, factory);
    }

    // Builds the view for the screen, passing it the navigation parameter.
    public N create(Screen screen, Object parameter) {
        Function<Object, N> factory = _factories.get(screen);
        if (factory == null) {
            return _placeholder.apply(screen);
        }
        return factory.apply(parameter);
    }
}
