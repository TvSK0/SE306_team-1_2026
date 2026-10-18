// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.logic.ConstantsService;

import java.util.ArrayList;
import java.util.List;

// What a category dropdown should show: its values, which one is selected, and the
// prompt to display when it is empty. Kept free of JavaFX so it can be unit tested;
// ConstantDropdown just copies these onto a ComboBox.
public final class DropdownOptions {
    static final String NOT_LOADED_PROMPT = "Values not loaded";
    static final String NO_VALUES_PROMPT = "No values available";
    static final String CHOOSE_PROMPT = "Choose...";

    private final List<String> _items;
    private final String _selected;
    private final String _promptText;

    private DropdownOptions(List<String> items, String selected, String promptText) {
        _items = List.copyOf(items);
        _selected = selected;
        _promptText = promptText;
    }

    // Builds the options for a category. currentValue is the value already saved on the
    // record being edited, or null/blank for a new record.
    public static DropdownOptions forCategory(ConstantsService service, String category, String currentValue) {
        List<String> items = new ArrayList<>(service.getValues(category));
        String selected = currentValue == null || currentValue.isBlank() ? null : currentValue;

        // a saved value that the server no longer lists stays visible, so editing a
        // record never silently drops it
        if (selected != null && !items.contains(selected)) {
            items.add(selected);
        }

        String promptText;
        if (!items.isEmpty()) {
            promptText = CHOOSE_PROMPT;
        } else if (service.isLoaded()) {
            promptText = NO_VALUES_PROMPT;
        } else {
            promptText = NOT_LOADED_PROMPT;
        }
        return new DropdownOptions(items, selected, promptText);
    }

    // The values to list, in display order. The returned list cannot be modified.
    public List<String> getItems() {
        return _items;
    }

    // The value to select, or null when nothing should be selected.
    public String getSelected() {
        return _selected;
    }

    // The grey text shown while nothing is selected.
    public String getPromptText() {
        return _promptText;
    }

    // False when there is nothing to choose from.
    public boolean isEnabled() {
        return !_items.isEmpty();
    }
}
