// TEMPORARY: swap this package for Scott's data package once his skeleton is on main.
package lavs.data;

import java.util.List;
import java.util.Map;

// Where ConstantsService keeps a copy of the category constants on this laptop, so the
// dropdowns still have values when the app starts offline. CategoryConstantStore is the
// SQLite one; tests pass an in-memory one. Both methods may throw LocalStoreException.
public interface ConstantsCache {
    // nowhere to save: nothing is kept and nothing is ever found
    ConstantsCache NONE = new ConstantsCache() {
        @Override
        public void saveAll(Map<String, List<String>> values) {
        }

        @Override
        public Map<String, List<String>> loadAll() {
            return Map.of();
        }
    };

    // replaces everything saved with these values
    void saveAll(Map<String, List<String>> values);

    // every saved category with its values in display order; empty if nothing is saved
    Map<String, List<String>> loadAll();
}
