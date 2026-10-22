// AI-assisted: a title with an optional subtitle and an optional primary button on the right.
package lavs.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

// Thin control. page(...) is the big 24px heading at the top of a screen ("Find a customer");
// section(...) is the 20px one inside a customer record ("Loans"). A null or blank subtitle
// is left out of the layout.
public class SectionHeading extends HBox {
    private final Label _subtitle = new Label();
    private final Button _action = new Button();

    private SectionHeading(String titleStyleClass, String title, String subtitle) {
        super(16);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(titleStyleClass);
        _subtitle.getStyleClass().add("lavs-subtitle");
        _subtitle.setWrapText(true);
        _subtitle.setText(subtitle);
        _subtitle.setVisible(UiText.hasText(subtitle));
        _subtitle.setManaged(UiText.hasText(subtitle));
        _action.getStyleClass().add("lavs-button-primary");

        VBox texts = new VBox(4, titleLabel, _subtitle);
        HBox.setHgrow(texts, Priority.ALWAYS);
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(texts, _action);
        clearAction();
    }

    public static SectionHeading page(String title, String subtitle) {
        return new SectionHeading("lavs-page-title", title, subtitle);
    }

    public static SectionHeading section(String title, String subtitle) {
        return new SectionHeading("lavs-section-title", title, subtitle);
    }

    // shows the primary button, e.g. "+ Add New Customer"
    public void setAction(String text, Runnable onClick) {
        _action.setText(text);
        _action.setOnAction(event -> onClick.run());
        _action.setVisible(true);
        _action.setManaged(true);
    }

    public void clearAction() {
        _action.setVisible(false);
        _action.setManaged(false);
    }
}
