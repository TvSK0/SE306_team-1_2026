// AI-assisted: a small modal "are you sure?" window with Cancel and a red confirm button.
package lavs.ui;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

// Thin control. There is no Figma frame for this, so it is kept plain and uses our classes.
// onConfirmed runs only if the user presses the confirm button; Cancel or closing the window
// does nothing.
public final class ConfirmDialog {
    private ConfirmDialog() {
    }

    public static void ask(Window owner, ConfirmContent content, Runnable onConfirmed) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setTitle(content.title());
        stage.setResizable(false);

        Label title = new Label(content.title());
        title.getStyleClass().add("lavs-section-title");
        Label message = new Label(content.message());
        message.setWrapText(true);
        message.getStyleClass().add("lavs-subtitle");

        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("lavs-button-secondary");
        cancel.setOnAction(event -> stage.close());
        Button confirm = new Button(content.confirmLabel());
        confirm.getStyleClass().add("lavs-button-danger");
        confirm.setOnAction(event -> {
            stage.close();
            onConfirmed.run();
        });
        HBox buttons = new HBox(12, cancel, confirm);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        VBox root = new VBox(16, title, message, buttons);
        root.getStyleClass().add("lavs-card");
        root.setPrefWidth(380);

        Scene scene = new Scene(root);
        // same stylesheet as the window that opened it
        scene.getStylesheets().setAll(owner.getScene().getStylesheets());
        stage.setScene(scene);
        stage.show();
    }
}
