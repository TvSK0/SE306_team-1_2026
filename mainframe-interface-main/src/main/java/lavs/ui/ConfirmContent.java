// AI-assisted: the wording of a confirmation dialog.
package lavs.ui;

// Plain, so the wording is unit tested. ConfirmDialog shows it.
public record ConfirmContent(String title, String message, String confirmLabel) {
    // "Delete this document?" / "This cannot be undone." / [Cancel] [Delete]
    public static ConfirmContent delete(String thing) {
        return new ConfirmContent("Delete this " + thing + "?", "This cannot be undone.", "Delete");
    }
}
