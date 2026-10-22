// AI-assisted: the five tag colours and the lavs.css class each one uses.
package lavs.ui;

public enum TagVariant {
    SUCCESS("lavs-tag-success"),
    NEUTRAL("lavs-tag-neutral"),
    WARNING("lavs-tag-warning"),
    DANGER("lavs-tag-danger"),
    INFO("lavs-tag-info");

    private final String _styleClass;

    TagVariant(String styleClass) {
        _styleClass = styleClass;
    }

    public String getStyleClass() {
        return _styleClass;
    }
}
