package lavs.data;

// Where the category constants come from. CategoryConstantsLoader reads them from the
// mainframe; tests can pass a lambda instead. The offline copy is a ConstantsCache.
public interface ConstantsSource {
    ConstantsLoadResult load();
}
