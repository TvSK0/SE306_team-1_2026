// AI-assisted: which view builds each customer record section, with a placeholder for unbuilt ones.
package lavs.ui;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;

// Generic over the view type N so it can be unit tested with plain strings; the app uses
// SectionRegistry<javafx.scene.Node>. Teammates call register(Section, factory) once at
// start-up, like navigator.register(Screen, factory).
public final class SectionRegistry<N> {
    private final Map<Section, Function<SectionContext, N>> _factories = new EnumMap<>(Section.class);
    private final Function<Section, N> _placeholder;

    // placeholder builds the "Not built yet" view for a section with no factory
    public SectionRegistry(Function<Section, N> placeholder) {
        _placeholder = placeholder;
    }

    // Registering a section again replaces the earlier factory.
    public void register(Section section, Function<SectionContext, N> factory) {
        _factories.put(section, factory);
    }

    public N create(Section section, SectionContext context) {
        Function<SectionContext, N> factory = _factories.get(section);
        return factory == null ? _placeholder.apply(section) : factory.apply(context);
    }
}
