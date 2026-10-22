// AI-assisted: a small coloured pill such as "Active" or "Waiting to upload".
package lavs.ui;

import javafx.scene.control.Label;

// Thin control. Which variant a status gets is decided by the screen's own tested formatter.
public class Tag extends Label {
    public Tag(TagVariant variant, String text) {
        set(variant, text);
    }

    public void set(TagVariant variant, String text) {
        setText(text);
        getStyleClass().setAll("label", "lavs-tag", variant.getStyleClass());
    }
}
