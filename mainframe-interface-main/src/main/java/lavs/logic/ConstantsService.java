// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.data.ConstantsLoadResult;
import lavs.data.ConstantsSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Keeps the category constants in memory so forms can fill their dropdowns without
// asking the mainframe each time. refresh() is meant to run on a background thread
// while screens call getValues() on the UI thread.
public class ConstantsService {
    private final ConstantsSource _source;

    // null until the first successful refresh. Always replaced as a whole, never
    // edited, and volatile so a screen on another thread sees the new map at once.
    private volatile Map<String, List<String>> _values;

    public ConstantsService(ConstantsSource source) {
        _source = source;
    }

    // Loads the constants again. On failure the previous values are kept.
    // TEMPORARY: swap ConstantsLoadResult for Scott's gateway result type once it exists.
    public ConstantsLoadResult refresh() {
        ConstantsLoadResult result = _source.load();
        if (result.getOutcome() == ConstantsLoadResult.Outcome.SUCCESS) {
            // copy every list so the source cannot change the cache afterwards
            _values = result.getValues().entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            entry -> List.copyOf(entry.getValue())));
        }
        return result;
    }

    // The values for a category in display order; empty if the category is unknown
    // or nothing has loaded yet. The returned list cannot be modified.
    public List<String> getValues(String category) {
        Map<String, List<String>> values = _values;
        if (values == null) return List.of();
        return values.getOrDefault(category, List.of());
    }

    // True once a refresh has succeeded.
    public boolean isLoaded() {
        return _values != null;
    }
}
