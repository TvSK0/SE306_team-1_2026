// AI-assisted: the customer record sidebar: counts, which sections can be opened and which is selected.
package lavs.ui;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;

// Plain (no JavaFX), so it is unit tested; CustomerRecordScreen copies it onto the controls.
// A count shows only once it is known: until its answer arrives, or if it could not be read,
// there is no number at all (CR-R5).
public final class RecordSidebarState {
    static final String ITEM = "lavs-nav-item";
    static final String SELECTED = "selected";

    private final boolean _saved;
    private final Map<Section, OptionalLong> _counts = new EnumMap<>(Section.class);
    private Section _selected = Section.DETAILS;

    // saved is false for a new customer
    public RecordSidebarState(boolean saved) {
        _saved = saved;
    }

    public void setCount(Section section, OptionalLong count) {
        _counts.put(section, count);
    }

    // the number to show at the right of the item, if one is known
    public Optional<String> countText(Section section) {
        OptionalLong count = _counts.getOrDefault(section, OptionalLong.empty());
        return Optional.of(count).filter(known -> section.hasCount() && known.isPresent())
                .map(known -> Long.toString(known.getAsLong()));
    }

    // a new customer has only Details until it is saved (CR-R4)
    public boolean isEnabled(Section section) {
        return section == Section.DETAILS || _saved;
    }

    // Selects the section; one that is not enabled is ignored.
    public void select(Section section) {
        if (isEnabled(section)) {
            _selected = section;
        }
    }

    public Section selected() {
        return _selected;
    }

    // the style classes of the item: highlighted when it is the selected one
    public List<String> styleClasses(Section section) {
        List<String> classes = new ArrayList<>(List.of(ITEM));
        if (section == _selected) {
            classes.add(SELECTED);
        }
        return classes;
    }
}
