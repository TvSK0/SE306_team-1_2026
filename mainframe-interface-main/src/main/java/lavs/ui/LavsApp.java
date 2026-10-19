package lavs.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

// TEMPORARY: swap this for the real package name once Scott's skeleton exists.
// Starts the desktop app: builds the window (top bar + navigator) and shows the login screen.
public class LavsApp extends Application {
    // every screen shares this one stylesheet (colour tokens, type, components)
    private static final String STYLESHEET = "lavs.css";

    @Override
    public void start(Stage stage) {
        Navigator navigator = new Navigator(Screen.LOGIN);
        // TEMPORARY: swap the fixed feed for an adapter over SyncService once the services are
        // composed here (see StatusFeed)
        TopBar topBar = new TopBar(new FixedStatusFeed(true, 0), Platform::runLater);
        MainWindow window = new MainWindow(navigator, topBar);

        // Log out: forget the user and go back to a fresh login screen (no Back to the old screens)
        topBar.setOnLogOut(() -> {
            topBar.setUserName(null);
            navigator.resetTo(Screen.LOGIN, null);
        });

        // Teammates register their screens here: navigator.register(Screen.X, parameter -> view).
        // Screens nobody has registered yet show a "Not built yet" placeholder.
        navigator.register(Screen.LOGIN, parameter -> temporaryLogin(navigator, topBar));
        navigator.start();

        stage.setTitle("LAVS");
        Scene scene = new Scene(window, 1280, 800);
        scene.getStylesheets().add(LavsApp.class.getResource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    // TEMPORARY: swap this for the real login screen (US1, feature/us1-login). Until then this
    // button lets the shell be tried: it "signs in" and goes to customer search.
    private static Node temporaryLogin(Navigator navigator, TopBar topBar) {
        Label label = new Label("Login screen: built in US1");
        label.getStyleClass().add("lavs-page-title");
        Button continueButton = new Button("Continue (temporary)");
        continueButton.getStyleClass().add("lavs-button-primary");
        continueButton.setOnAction(event -> {
            topBar.setUserName("Jane Smith");
            navigator.resetTo(Screen.CUSTOMER_SEARCH, null);
        });
        VBox box = new VBox(16, label, continueButton);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
