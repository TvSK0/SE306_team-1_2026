package lavs.logic;

import lavs.data.ConstantsCache;
import lavs.data.ConstantsLoadResult;
import lavs.data.ConstantsSource;
import lavs.data.LocalStoreException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Keeps the category constants in memory so forms can fill their dropdowns without
// asking the mainframe each time. refresh() and loadSaved() are meant to run on a
// background thread while screens call getValues() on the UI thread.
//
// Each successful refresh is also saved to a ConstantsCache (the SQLite file), and
// loadSaved() reads it back, so the dropdowns still have values when the app starts offline.
public class ConstantsService {
    private final ConstantsSource _source;
    private final ConstantsCache _cache;

    // null until the first successful refresh or loadSaved. Always replaced as a whole,
    // never edited, and volatile so a screen on another thread sees the new map at once.
    private volatile Map<String, List<String>> _values;

    public ConstantsService(ConstantsSource source) {
        this(source, ConstantsCache.NONE);
    }

    public ConstantsService(ConstantsSource source, ConstantsCache cache) {
        _source = source;
        _cache = cache;
    }

    // Fills the dropdown values from the copy saved on this laptop, so forms work before
    // (or without) a successful refresh. Values already loaded from the mainframe are newer,
    // so they are never replaced. Returns true when values are available afterwards.
    public boolean loadSaved() {
        Map<String, List<String>> saved;
        try {
            saved = _cache.loadAll();
        } catch (LocalStoreException e) {
            // an unreadable file is the same as nothing saved: the next refresh fills it
            return isLoaded();
        }
        synchronized (this) {
            if (_values == null && !saved.isEmpty()) {
                _values = copyOf(saved);
            }
        }
        return isLoaded();
    }

    // Loads the constants again. On failure the previous values are kept.
    // TEMPORARY: swap ConstantsLoadResult for Scott's gateway result type once it exists.
    public ConstantsLoadResult refresh() {
        ConstantsLoadResult result = _source.load();
        if (result.getOutcome() == ConstantsLoadResult.Outcome.SUCCESS) {
            Map<String, List<String>> values = copyOf(result.getValues());
            // locked so a loadSaved running at the same time cannot put older values back
            synchronized (this) {
                _values = values;
            }
            try {
                _cache.saveAll(values);
            } catch (LocalStoreException e) {
                // the dropdowns still work for this run; only the offline copy is out of date
            }
        }
        return result;
    }

    // copies every list so whoever handed the map over cannot change the values afterwards
    private static Map<String, List<String>> copyOf(Map<String, List<String>> values) {
        Map<String, List<String>> copy = new HashMap<>();
        for (String category : values.keySet()) {
            copy.put(category, List.copyOf(values.get(category)));
        }
        return Map.copyOf(copy);
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
