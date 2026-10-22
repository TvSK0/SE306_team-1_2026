package lavs.ui;

import javafx.application.Application;
import lavs.data.LazyConstantsCache;
import lavs.logic.SyncService;
import lavs.logic.SyncSessions;
import java.time.Duration;
import javafx.scene.control.Alert;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lavs.data.CategoryConstantsLoader;
import lavs.data.MainframeGateway;
import lavs.logic.AddressService;
import lavs.logic.CustomerSearchService;
import lavs.logic.CustomerLoansService;
import lavs.logic.CustomerEligibilityService;
import lavs.logic.EmployerService;
import lavs.logic.AuthService;
import lavs.logic.ConstantsService;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.CustomerService;
import lavs.logic.EmailService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanBorrowerService;
import lavs.logic.LoanService;
import lavs.logic.LoanStatusService;
import lavs.logic.LoginFlow;
import lavs.logic.PhoneNumberService;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Starts the desktop app: builds the window (top bar + navigator) and shows the login screen.
public class LavsApp extends Application {
    // every screen shares this one stylesheet (colour tokens, type, components)
    private static final String STYLESHEET = "lavs.css";
    private static final String SERVER_URL = System.getenv().getOrDefault("LAVS_SERVER_URL",
            "https://animating-eraser-arrogance.ngrok-free.dev/");
    // the SQLite file on this laptop that keeps the dropdown values between runs
    private static final Path LOCAL_DATABASE = Path.of(System.getProperty("user.home"), ".lavs", "lavs.db");
    private static final long ORGANISATION = Long.parseLong(System.getenv().getOrDefault("LAVS_ORGANISATION", "1"));
    private volatile SyncService sync;
    private PendingUploadsPanel pendingPanel;

    // daemon threads, so closing the window ends the app even while a call is running
    private final ExecutorService _background = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "lavs-background");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void start(Stage stage) {
        Connection connection = new HttpConnection(SERVER_URL);
        MainframeGateway gateway = new MainframeGateway(connection);
        ConstantsService constantsService = new ConstantsService(new CategoryConstantsLoader(gateway),
                new LazyConstantsCache(LOCAL_DATABASE));
        BackgroundTasks tasks = new BackgroundTasks(_background, Platform::runLater);
        // the dropdowns get last run's values straight away, even if the mainframe never answers


        Navigator navigator = new Navigator(Screen.LOGIN);
        SessionStatusFeed statusFeed = new SessionStatusFeed();
        TopBar topBar = new TopBar(statusFeed, Platform::runLater);
        MainWindow window = new MainWindow(navigator, topBar);

        // one AuthService and one LoginFlow, sharing the Connection and the constants
        AuthService authService = new AuthService(gateway);
        LoginFlow loginFlow = new LoginFlow(authService, constantsService);
        loginFlow.setOnReady(user -> {
            if (sync != null) sync.close();
            sync = SyncSessions.open(LOCAL_DATABASE.getParent(), user, gateway);
            sync.start(Duration.ofSeconds(10));
        });

        // Log out: forget the user and go back to a fresh login screen (no Back to the old screens)
        topBar.setOnLogOut(() -> {
            if (tasks.isBusy()) { waitForOperation(); return; }
            window.setDisable(true);
            if (pendingPanel != null) pendingPanel.hide();
            tasks.run(() -> {
                if (sync != null) sync.close();
                sync = null;
                gateway.setWriteDispatcher(request -> { throw new IllegalStateException("Please log in before saving"); });
                gateway.setLocalReads(request -> null);
                gateway.setNumberResolver(java.util.function.UnaryOperator.identity());
                authService.logout();
                return true;
            }, done -> {
                statusFeed.attach(null);
                topBar.setUserName(null);
                window.setDisable(false);
                navigator.resetTo(Screen.LOGIN, null);
            }, error -> { window.setDisable(false); new Alert(Alert.AlertType.ERROR, ResultText.forError(error)).show(); });
        });

        // Teammates register their screens here: navigator.register(Screen.X, parameter -> view).
        // Screens nobody has registered yet show a "Not built yet" placeholder.
        navigator.register(Screen.LOGIN, parameter -> new LoginScreen(loginFlow, tasks, user -> {
            statusFeed.attach(sync);
            pendingPanel = new PendingUploadsPanel(sync, tasks, navigator);
            topBar.setOnStatusClicked(() -> pendingPanel.toggle(topBar));
            topBar.setUserName(user);
            navigator.resetTo(Screen.CUSTOMER_SEARCH, null);
        }).create());
        LoanService loanService = new LoanService(gateway);
        CustomerSearchService searchService = new CustomerSearchService(gateway);
        navigator.register(Screen.CUSTOMER_SEARCH, parameter -> new CustomerSearchScreen(searchService, tasks, navigator, null));
        navigator.register(Screen.CUSTOMER_SELECT, parameter -> new CustomerSearchScreen(searchService, tasks, navigator,
                (CustomerSelection) parameter));
        new LoanScreens(new LoanScreenServices(loanService, new LoanRepaymentsService(gateway),
                constantsService, new LoanStatusService(gateway), new LoanBorrowerService(gateway), tasks, navigator, new lavs.logic.RecordNotesService(gateway), new lavs.logic.RecordDocumentsService(gateway)))
                .register(navigator);
        new CustomerScreens(new CustomerScreenServices(new CustomerService(gateway, ORGANISATION),
                new AddressService(gateway), new PhoneNumberService(gateway), new EmailService(gateway),
                new CustomerRelationshipService(gateway), constantsService, tasks, new EmployerService(gateway),
                new CustomerLoansService(gateway), new CustomerEligibilityService(gateway), loanService, navigator, new lavs.logic.RecordNotesService(gateway), new lavs.logic.RecordDocumentsService(gateway)))
                .register(navigator);
        // developer only: with LAVS_DEMO=true (or -Dlavs.demo=true) start on the component gallery
        if (DemoScreen.isEnabled()) {
            navigator.register(Screen.LOGIN, parameter -> DemoScreen.create());
        }
        navigator.start();

        stage.setTitle("LAVS");
        Scene scene = new Scene(window, 1280, 800);
        scene.getStylesheets().add(LavsApp.class.getResource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> {
            event.consume();
            if (tasks.isBusy()) { waitForOperation(); return; }
            window.setDisable(true);
            tasks.run(() -> {
                if (sync != null) sync.close();
                try { connection.close(); } catch (IOException error) { throw new UncheckedIOException(error); }
                return true;
            }, done -> { _background.shutdown(); Platform.exit(); }, error -> {
                window.setDisable(false);
                new Alert(Alert.AlertType.ERROR, ResultText.forError(error)).show();
            });
        });
        stage.show();
    }

    private static void waitForOperation() {
        new Alert(Alert.AlertType.INFORMATION, "Please wait for the current operation to finish before leaving. "
                + "Your saved changes will remain on this laptop.").show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
