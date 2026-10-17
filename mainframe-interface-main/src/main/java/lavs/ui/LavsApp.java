package lavs.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

// TEMPORARY: swap this for the real package name once Scott's skeleton exists.
// Starts the desktop app and shows the first screen.
public class LavsApp extends Application {
    // TEMPORARY: swap this for the login screen once Taran's UI shell exists.
    private static final String FIRST_SCREEN = "main-window.fxml";

    @Override
    public void start(Stage stage) throws IOException {
        Parent root = FXMLLoader.load(LavsApp.class.getResource(FIRST_SCREEN));
        stage.setTitle("LAVS");
        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
