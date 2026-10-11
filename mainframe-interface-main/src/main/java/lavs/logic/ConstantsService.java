// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.data.ConstantsLoadResult;
import lavs.data.ConstantsSource;

import java.util.List;

// Keeps the category constants in memory so forms can fill their dropdowns without
// asking the mainframe each time. refresh() is meant to run on a background thread
// while screens call getValues() on the UI thread.
public class ConstantsService {
    private final ConstantsSource _source;

    public ConstantsService(ConstantsSource source) {
        _source = source;
    }

    // Loads the constants again. On failure the previous values are kept.
    // TEMPORARY: swap ConstantsLoadResult for Scott's gateway result type once it exists.
    public ConstantsLoadResult refresh() {
        // TODO step 8: implement. Replace the whole cache in one assignment so a
        // screen never sees a half-updated cache.
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // The values for a category in display order; empty if the category is unknown
    // or nothing has loaded yet. The returned list cannot be modified.
    public List<String> getValues(String category) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    // True once a refresh has succeeded.
    public boolean isLoaded() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
