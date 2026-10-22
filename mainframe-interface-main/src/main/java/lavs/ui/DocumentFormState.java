// AI-assisted: whether the add or edit document form is open, and for which document.
package lavs.ui;

import lavs.logic.DocumentRow;

import java.util.Optional;

// Plain (no JavaFX), so it is unit tested. The form is an inline card under the table
// (Figma 11), not a dialog: "+ Add document" and Edit open it, Cancel and a good save close it.
public final class DocumentFormState {
    private boolean _open;
    private DocumentRow _editing;

    public void startAdd() {
        _open = true;
        _editing = null;
    }

    public void startEdit(DocumentRow row) {
        _open = true;
        _editing = row;
    }

    public void close() {
        _open = false;
        _editing = null;
    }

    public boolean isOpen() {
        return _open;
    }

    // "Add document" or "Edit document"
    public String title() {
        return _editing == null ? "Add document" : "Edit document";
    }

    // the document being edited, or empty while adding
    public Optional<DocumentRow> editing() {
        return Optional.ofNullable(_editing);
    }
}
