package lavs.logic;

// AI-assisted: service contract consumed by the application.
public interface CategoryValues {
    java.util.List<String> getValues(String category);
    boolean isLoaded();
    boolean loadSaved();
    lavs.data.ConstantsLoadResult refresh();
}
