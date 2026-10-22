// AI-assisted: a coloured message box with a title and a body (error, warning or info).
package lavs.ui;

import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Thin control: the wording and colour come from BannerContent (unit tested). Starts hidden;
// show(content) displays it and hide() removes it from the layout. Styled by lavs-banner*.
public class Banner extends VBox {
    private final Label _title = new Label();
    private final Label _body = new Label();
    private final Hyperlink _link = new Hyperlink();

    public Banner() {
        super(4);
        _title.getStyleClass().add("lavs-banner-title");
        _title.setWrapText(true);
        _body.getStyleClass().add("lavs-banner-body");
        _body.setWrapText(true);
        _link.getStyleClass().add("lavs-banner-link");
        getChildren().addAll(_title, _body, _link);
        hideLink();
        hide();
    }

    public Banner(BannerContent content) {
        this();
        show(content);
    }

    public void show(BannerContent content) {
        hideLink();
        _title.setText(content.title());
        _body.setText(content.body());
        getStyleClass().setAll("lavs-banner", content.variant().getStyleClass());
        setVisible(true);
        setManaged(true);
    }

    // The banner with a link under its body, e.g. "Go to Addresses →" (Figma 08).
    public void showWithLink(BannerContent content, String linkText, Runnable onClick) {
        show(content);
        _link.setText(linkText);
        _link.setOnAction(event -> onClick.run());
        _link.setVisible(true);
        _link.setManaged(true);
    }

    private void hideLink() {
        _link.setVisible(false);
        _link.setManaged(false);
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }
}
