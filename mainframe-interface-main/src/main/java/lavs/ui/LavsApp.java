package lavs.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lavs.data.CategoryConstantsLoader;
import lavs.data.CategoryConstantStore;
import lavs.data.LocalDatabase;
import lavs.data.LocalStoreException;
import lavs.data.MainframeGateway;
import lavs.data.SqlitePendingChangeStore;
import lavs.logic.AddressService;
import lavs.logic.AuthService;
import lavs.logic.ConstantsService;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.CustomerService;
import lavs.logic.EmailService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanSaveResult;
import lavs.logic.LoanBorrowerService;
import lavs.logic.LoanService;
import lavs.logic.LoanStatusService;
import lavs.logic.LoginFlow;
import lavs.logic.PhoneNumberService;
import lavs.logic.SyncService;
import lavs.model.Customer;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Starts the desktop app: builds the window (top bar + navigator) and shows the login screen.
public class LavsApp extends Application {
    // every screen shares this one stylesheet (colour tokens, type, components)
    private static final String STYLESHEET = "lavs.css";
    // TEMPORARY: swap this for the server address from the real login screen (US1) once Taran's
    // login exists. Same server as TestConstants.BASE_URL.
    private static final String SERVER_URL = "https://animating-eraser-arrogance.ngrok-free.dev/";
    // the SQLite file on this laptop that keeps the dropdown values between runs
    private static final Path LOCAL_DATABASE = Path.of(System.getProperty("user.home"), ".lavs", "lavs.db");
    // TEMPORARY: the organisation new customers are created in, as in the starter tests. Swap
    // for the signed-in MMM's organisation once the login screen (US1) knows it.
    private static final long ORGANISATION = 1;
    // AI-assisted: how often the sync thread retries queued uploads while the mainframe is offline.
    private static final Duration SYNC_RETRY_INTERVAL = Duration.ofSeconds(30);

    // daemon threads, so closing the window ends the app even while a call is running
    private final ExecutorService _background = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "lavs-background");
        thread.setDaemon(true);
        return thread;
    });
    // AI-assisted: the upload queue + sync thread every save goes through; null only when the
    // app refused to start because the local database could not be opened.
    private SyncService _sync;

    @Override
    public void start(Stage stage) {
        Connection connection = new HttpConnection(SERVER_URL);
        // AI-assisted: one gateway over the Connection, shared by the customer services and the
        // sync thread, so queued uploads go out as the logged-in MMM (a fresh Connection would
        // have no session token).
        MainframeGateway gateway = new MainframeGateway(connection);
        // AI-assisted: open the one SQLite file the dropdown cache and the upload queue share.
        // Every save goes through that queue, so without it nothing could be kept safe: say so and
        // stop, rather than open screens that would lose the MMM's work.
        LocalDatabase database = openLocalDatabase();
        if (database == null) {
            refuseToStart();
            return;
        }
        ConstantsService constantsService = new ConstantsService(new CategoryConstantsLoader(connection),
                new CategoryConstantStore(database));
        BackgroundTasks tasks = new BackgroundTasks(_background, Platform::runLater);
        // the dropdowns get last run's values straight away, even if the mainframe never answers
        tasks.run(constantsService::loadSaved, loaded -> { }, failure -> failure.printStackTrace());

        Navigator navigator = new Navigator(Screen.LOGIN);
        // AI-assisted: start the upload queue + sync thread and drive the top bar's online/pending
        // state from it.
        _sync = SyncService.create(new SqlitePendingChangeStore(database), gateway);
        _sync.start(SYNC_RETRY_INTERVAL);
        TopBar topBar = new TopBar(new SyncStatusFeed(_sync), Platform::runLater);
        MainWindow window = new MainWindow(navigator, topBar);
        // AI-assisted: clicking the status pill in the top bar opens the pending-uploads panel.
        PendingUploadsPanel panel = new PendingUploadsPanel(_sync);
        topBar.setOnStatusClicked(() -> panel.toggle(topBar));

        // one AuthService and one LoginFlow, sharing the Connection and the constants
        AuthService authService = new AuthService(connection);
        LoginFlow loginFlow = new LoginFlow(authService, constantsService);

        // Log out: forget the user and go back to a fresh login screen (no Back to the old screens)
        topBar.setOnLogOut(() -> {
            authService.logout();
            topBar.setUserName(null);
            navigator.resetTo(Screen.LOGIN, null);
        });

        // Teammates register their screens here: navigator.register(Screen.X, parameter -> view).
        // Screens nobody has registered yet show a "Not built yet" placeholder.
        navigator.register(Screen.LOGIN, parameter -> new LoginScreen(loginFlow, tasks, user -> {
            topBar.setUserName(user);
            navigator.resetTo(Screen.CUSTOMER_SEARCH, null);
        }).create());
        // AI-assisted: loan saves go through the upload queue (US11)
        LoanService loanService = new LoanService(connection, _sync);
        navigator.register(Screen.CUSTOMER_SEARCH, parameter -> temporaryCustomerSearch(navigator, loanService, tasks));
        new LoanScreens(new LoanScreenServices(loanService, new LoanRepaymentsService(connection),
                // AI-assisted: status changes and co-borrower changes save through the upload queue (US11)
                constantsService, new LoanStatusService(connection, _sync), new LoanBorrowerService(connection, _sync),
                tasks))
                .register(navigator);
        new CustomerScreens(new CustomerScreenServices(new CustomerService(gateway, _sync, ORGANISATION),
                // AI-assisted: the contact services save through the upload queue (US11)
                new AddressService(gateway, _sync), new PhoneNumberService(gateway, _sync),
                new EmailService(gateway, _sync),
                new CustomerRelationshipService(connection, _sync), constantsService, tasks))
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
        stage.show();
    }

    // AI-assisted: stop the sync thread cleanly on exit so a queued change is never cut off
    // mid-write (the thread is a daemon and would otherwise die with the JVM).
    @Override
    public void stop() {
        if (_sync != null) {
            _sync.close();
        }
    }

    // AI-assisted: open the one SQLite file the dropdown cache and the upload queue share; null
    // if it cannot be opened (start() then refuses to start).
    private static LocalDatabase openLocalDatabase() {
        try {
            Files.createDirectories(LOCAL_DATABASE.getParent());
            return new LocalDatabase(LOCAL_DATABASE);
        } catch (IOException | LocalStoreException e) {
            e.printStackTrace();
            return null;
        }
    }

    // AI-assisted: tells the MMM why LAVS can't start, then closes it.
    private static void refuseToStart() {
        Alert alert = new Alert(Alert.AlertType.ERROR, "LAVS could not open its local database at "
                + LOCAL_DATABASE + ", so it can't keep your changes safe. Close any other copy of LAVS, "
                + "check the folder can be written to, and try again.");
        alert.setHeaderText("LAVS can't start");
        alert.showAndWait();
        Platform.exit();
    }

    // TEMPORARY: swap this for the real customer search (US2) once Taran's screen exists. Until
    // then it opens a customer's record or a new loan for a customer number typed in by hand,
    // starts a new customer, or opens a saved loan by its number.
    private static Node temporaryCustomerSearch(Navigator navigator, LoanService loanService, BackgroundTasks tasks) {
        Label label = new Label("Customer search: built in US2");
        label.getStyleClass().add("lavs-page-title");
        TextField customerNumber = new TextField();
        customerNumber.setPromptText("Customer number");
        customerNumber.setMaxWidth(240);
        Button newLoan = new Button("New loan (temporary)");
        newLoan.getStyleClass().add("lavs-button-primary");
        newLoan.disableProperty().bind(customerNumber.textProperty().isEmpty());
        newLoan.setOnAction(event -> {
            Customer customer = new Customer();
            customer.setNumber(customerNumber.getText().trim());
            navigator.goTo(Screen.LOAN, LoanScreenRequest.forNewLoan(customer));
        });
        Button openCustomer = new Button("Open customer (temporary)");
        openCustomer.getStyleClass().add("lavs-button-secondary");
        openCustomer.disableProperty().bind(customerNumber.textProperty().isEmpty());
        openCustomer.setOnAction(event -> navigator.goTo(Screen.CUSTOMER_RECORD, customerNumber.getText().trim()));
        Button newCustomer = new Button("New customer (temporary)");
        newCustomer.getStyleClass().add("lavs-button-secondary");
        newCustomer.setOnAction(event -> navigator.goTo(Screen.CUSTOMER_RECORD, null));

        TextField loanNumber = new TextField();
        loanNumber.setPromptText("Loan number");
        loanNumber.setMaxWidth(240);
        Button openLoan = new Button("Open loan (temporary)");
        openLoan.getStyleClass().add("lavs-button-secondary");
        Label error = new Label();
        LoadingView loading = new LoadingView(LoadingState.forRead("Open loan (temporary)", "Opening..."), openLoan);
        openLoan.setOnAction(event -> tasks.run(
                () -> {
                    try {
                        return loanService.loadLoan(loanNumber.getText().trim());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                loading::start,
                result -> {
                    loading.finish();
                    if (result.isSuccessful()) {
                        // the loan only knows its customer's number; the real customer record
                        // passes the whole customer so the Back link shows the name
                        Customer customer = new Customer();
                        customer.setNumber(result.getLoan().getCustomerNumber());
                        navigator.goTo(Screen.LOAN, LoanScreenRequest.forLoan(customer, result.getLoan()));
                    } else {
                        error.setText(openLoanError(result));
                    }
                },
                failure -> {
                    loading.finish();
                    error.setText("Could not open the loan: " + failure.getMessage());
                }));

        VBox box = new VBox(16, label, customerNumber, newLoan, openCustomer, newCustomer, loanNumber, openLoan, error);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private static String openLoanError(LoanSaveResult result) {
        if (result.getMainframeStatus() == null) {
            return "Type a loan number first.";
        }
        return "Could not open the loan: " + result.getMainframeStatus().getErrorMessage();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
