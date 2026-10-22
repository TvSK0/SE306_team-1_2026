package lavs.model;

// AI-assisted: immutable note shown by both customer and loan screens.
public record RecordNote(long id, String type, String lines, String author, String dateAdded) {}
