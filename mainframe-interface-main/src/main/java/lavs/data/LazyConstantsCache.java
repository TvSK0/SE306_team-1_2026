package lavs.data;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.List;
import java.util.Map;

// AI-assisted: opening SQLite is deferred until the background constants task uses it.
public final class LazyConstantsCache implements ConstantsCache {
    private final Path file;
    private ConstantsCache cache;
    public LazyConstantsCache(Path file) { this.file = file; }
    private synchronized ConstantsCache cache() {
        if (cache == null) {
            try { Files.createDirectories(file.toAbsolutePath().getParent()); }
            catch (IOException error) { throw new LocalStoreException("Could not create the cache folder", error); }
            cache = new CategoryConstantStore(new LocalDatabase(file));
        }
        return cache;
    }
    public Map<String, List<String>> loadAll() { return cache().loadAll(); }
    public void saveAll(Map<String, List<String>> values) { cache().saveAll(values); }
}
