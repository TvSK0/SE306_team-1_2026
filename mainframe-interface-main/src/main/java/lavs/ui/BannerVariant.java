// AI-assisted: the three banner colours and the lavs.css class each one uses.
package lavs.ui;

public enum BannerVariant {
    ERROR("lavs-banner-error"),
    WARNING("lavs-banner-warning"),
    INFO("lavs-banner-info");

    private final String _styleClass;

    BannerVariant(String styleClass) {
        _styleClass = styleClass;
    }

    public String getStyleClass() {
        return _styleClass;
    }
}
