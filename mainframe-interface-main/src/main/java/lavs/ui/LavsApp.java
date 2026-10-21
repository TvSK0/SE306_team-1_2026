package lavs.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lavs.data.CategoryConstantsLoader;
import lavs.data.CategoryConstantStore;
import lavs.data.ConstantsCache;
import lavs.data.ConstantsLoadResult;
import lavs.data.LocalDatabase;
import lavs.data.LocalStoreException;
import lavs.logic.ConstantsService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanSaveResult;
import lavs.logic.LoanBorrowerService;
import lavs.logic.LoanService;
import lavs.logic.LoanStatusService;
import lavs.model.Customer;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;
import mainframe.client.Status;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// TEMPORARY: swap this for the real package name once Scott's skeleton exists.
// Starts the desktop app: builds the window (top bar + navigator) and shows the login screen.
public class LavsApp extends Application {
    // every screen shares this one stylesheet (colour tokens, type, components)
    private static final String STYLESHEET = "lavs.css";
    // TEMPORARY: swap this for the server address from the real login screen (US1) once Taran's
    // login exists. Same server as TestConstants.BASE_URL.
    private static final String SERVER_URL = "https://animating-eraser-arrogance.ngrok-free.dev/";
    // the SQLite file on this laptop that keeps the dropdown values between runs
    private static final Path LOCAL_DATABASE = Path.of(System.getProperty("user.home"), ".lavs", "lavs.db");

    // daemon threads, so closing the window ends the app even while a call is running
    private final ExecutorService _background = Executors.newCachedThreadPool(runnable -> {
        Thread thread = new Thread(runnable, "lavs-background");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public void start(Stage stage) {
        // TEMPORARY: swap this Connection for Scott's gateway once it exists
        Connection connection = new HttpConnection(SERVER_URL);
        ConstantsService constantsService = new ConstantsService(new CategoryConstantsLoader(connection),
                openConstantsCache());
        BackgroundTasks tasks = new BackgroundTasks(_background, Platform::runLater);
        // the dropdowns get last run's values straight away, even if the mainframe never answers
        tasks.run(constantsService::loadSaved, loaded -> { }, failure -> failure.printStackTrace());

        Navigator navigator = new Navigator(Screen.LOGIN);
        // TEMPORARY: swap the fixed feed for an adapter over SyncService once the services are
        // composed here (see StatusFeed)
        TopBar topBar = new TopBar(new FixedStatusFeed(true, 0), Platform::runLater);
        MainWindow window = new MainWindow(navigator, topBar);

        // Log out: forget the user and go back to a fresh login screen (no Back to the old screens)
        topBar.setOnLogOut(() -> {
            connection.Logout();
            topBar.setUserName(null);
            navigator.resetTo(Screen.LOGIN, null);
        });

        // Teammates register their screens here: navigator.register(Screen.X, parameter -> view).
        // Screens nobody has registered yet show a "Not built yet" placeholder.
        navigator.register(Screen.LOGIN, parameter -> temporaryLogin(navigator, topBar, connection, constantsService, tasks));
        LoanService loanService = new LoanService(connection);
        navigator.register(Screen.CUSTOMER_SEARCH, parameter -> temporaryCustomerSearch(navigator, loanService, tasks));
        new LoanScreens(new LoanScreenServices(loanService, new LoanRepaymentsService(connection),
                constantsService, new LoanStatusService(connection), new LoanBorrowerService(connection), tasks))
                .register(navigator);
        navigator.start();

        stage.setTitle("LAVS");
        Scene scene = new Scene(window, 1280, 800);
        scene.getStylesheets().add(LavsApp.class.getResource(STYLESHEET).toExternalForm());
        stage.setScene(scene);
        stage.show();
    }

    // TEMPORARY: swap this for the real login screen (US1, feature/us1-login). Until then this
    // button signs in to the class server as admin, loads the dropdown values and goes to
    // customer search.
    private static Node temporaryLogin(Navigator navigator, TopBar topBar, Connection connection,
                                       ConstantsService constantsService, BackgroundTasks tasks) {
        Label label = new Label("Login screen: built in US1");
        label.getStyleClass().add("lavs-page-title");
        Label error = new Label();
        Button continueButton = new Button("Continue (temporary)");
        continueButton.getStyleClass().add("lavs-button-primary");
        LoadingView loading = new LoadingView(LoadingState.forSave("Continue (temporary)", "Signing in..."), continueButton);
        continueButton.setOnAction(event -> tasks.run(
                () -> signIn(connection, constantsService),
                loading::start,
                message -> {
                    loading.finish();
                    if (message == null) {
                        topBar.setUserName("admin");
                        navigator.resetTo(Screen.CUSTOMER_SEARCH, null);
                    } else {
                        error.setText(message);
                    }
                },
                failure -> {
                    loading.finish();
                    error.setText("Could not sign in: " + failure.getMessage());
                }));
        VBox box = new VBox(16, label, continueButton, error);
        box.setAlignment(Pos.CENTER);
        return box;
    }

    // Runs in the background. Returns null when signed in with the dropdown values loaded,
    // otherwise the message to show.
    private static String signIn(Connection connection, ConstantsService constantsService) {
        try {
            Status status = connection.Login("admin", "admin");
            if (!status.getWasSuccessful()) {
                return "Could not sign in: " + status.getErrorMessage();
            }
        } catch (IOException e) {
            return "Could not reach the server: " + e.getMessage();
        }
        ConstantsLoadResult constants = constantsService.refresh();
        // the values saved last run are good enough to carry on with
        if (constants.getOutcome() != ConstantsLoadResult.Outcome.SUCCESS && !constantsService.isLoaded()) {
            return "Could not load the dropdown values: " + constants.getMessage();
        }
        return null;
    }

    // TEMPORARY: swap this for the LocalDatabase Aum's SyncService uses once the services are
    // composed here, so the app opens one SQLite file. Until then the constants get their own
    // copy of it. If the file cannot be opened the dropdowns just are not kept between runs.
    private static ConstantsCache openConstantsCache() {
        try {
            Files.createDirectories(LOCAL_DATABASE.getParent());
            return new CategoryConstantStore(new LocalDatabase(LOCAL_DATABASE));
        } catch (IOException | LocalStoreException e) {
            e.printStackTrace();
            return ConstantsCache.NONE;
        }
    }

    // TEMPORARY: swap this for the real customer search (US2) once Taran's screen exists. Until
    // then it opens a new loan for a customer number, or a saved loan by its number, typed in
    // by hand.
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

        VBox box = new VBox(16, label, customerNumber, newLoan, loanNumber, openLoan, error);
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
