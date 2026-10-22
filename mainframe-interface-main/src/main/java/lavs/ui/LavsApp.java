package lavs.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import lavs.data.CategoryConstantsLoader;
import lavs.data.CategoryConstantStore;
import lavs.data.ConstantsCache;
import lavs.data.LocalDatabase;
import lavs.data.LocalStoreException;
import lavs.logic.AuthService;
import lavs.logic.ConstantsService;
import lavs.logic.CustomerContactService;
import lavs.logic.CustomerCountsService;
import lavs.logic.CustomerRecordService;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.CustomerSearchService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanSaveResult;
import lavs.logic.LoanService;
import lavs.logic.LoanStatusService;
import lavs.logic.LoginFlow;
import lavs.model.Customer;
import mainframe.client.Connection;
import mainframe.client.HttpConnection;

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
        StatusFeed statusFeed = new FixedStatusFeed(true, 0);
        TopBar topBar = new TopBar(statusFeed, Platform::runLater);
        MainWindow window = new MainWindow(navigator, topBar);

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
        LoanService loanService = new LoanService(connection);
        CustomerSearchService searchService = new CustomerSearchService(connection);
        navigator.register(Screen.CUSTOMER_SEARCH, parameter -> new CustomerSearchScreen(searchService, tasks,
                statusFeed, Platform::runLater,
                new NormalSearchMode(number -> navigator.goTo(Screen.CUSTOMER_RECORD, number),
                        () -> navigator.goTo(Screen.CUSTOMER_RECORD, null))).create());
        // select mode: a screen asks for a customer with navigator.goTo(Screen.CUSTOMER_SELECT,
        // new CustomerSelectRequest(...)); Select and Cancel both come back with navigator.back()
        CustomerContactService contactService = new CustomerContactService(connection);
        navigator.register(Screen.CUSTOMER_SELECT, parameter -> new CustomerSearchScreen(searchService, tasks,
                statusFeed, Platform::runLater,
                new SelectSearchMode(CustomerSelectRequest.from(parameter), contactService, tasks,
                        Platform::runLater, navigator::back)).create());
        new LoanScreens(new LoanScreenServices(loanService, new LoanRepaymentsService(connection),
                constantsService, new LoanStatusService(connection), tasks))
                .register(navigator);
        CustomerRelationshipService relationshipService = new CustomerRelationshipService(connection);
        // Teammates register their customer record sections here, like their screens:
        // recordSections.register(Section.NOTES, context -> view). A section nobody has
        // built yet shows a placeholder that names its owner (see Section).
        SectionRegistry<Node> recordSections = new SectionRegistry<>(CustomerRecordScreen::placeholder);
        recordSections.register(Section.RELATIONSHIPS, context ->
                relationshipsSection(relationshipService, constantsService, tasks, context.customerNumber()));
        // TEMPORARY: swap this for the Loans tab (feature/customer-loans-tab)
        recordSections.register(Section.LOANS, context ->
                temporaryLoansSection(navigator, loanService, tasks, context.customerNumber()));
        CustomerRecordService recordService = new CustomerRecordService(connection);
        CustomerCountsService countsService = new CustomerCountsService(connection);
        navigator.register(Screen.CUSTOMER_RECORD, parameter -> new CustomerRecordScreen((String) parameter,
                recordService, countsService, recordSections, tasks, statusFeed, Platform::runLater,
                navigator::back, topBar::setTitle));
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

    // Yu's Relationships view as the Relationships section of the customer record.
    private static Node relationshipsSection(CustomerRelationshipService relationshipService,
                                             ConstantsService constantsService, BackgroundTasks tasks,
                                             String customerNumber) {
        try {
            return RelationshipsController.open(relationshipService, constantsService, tasks, customerNumber);
        } catch (IOException e) {
            // a missing or broken FXML file: the section stays unbuilt and the error goes to stderr
            throw new UncheckedIOException(e);
        }
    }

    // TEMPORARY: swap this for the Loans tab (feature/customer-loans-tab). Until then the Loans
    // section holds Yu's way into the loan screens: start a loan for this customer, or open a
    // saved loan by its number typed in by hand.
    private static Node temporaryLoansSection(Navigator navigator, LoanService loanService, BackgroundTasks tasks,
                                              String customerNumber) {
        Label title = new Label("Loans: built in the customer loans tab");
        title.getStyleClass().add("lavs-subtitle");
        return new VBox(8, title, loanStandIns(navigator, loanService, tasks, customerNumber));
    }

    // TEMPORARY: swap this for the Loans tab (feature/customer-loans-tab).
    private static Node loanStandIns(Navigator navigator, LoanService loanService, BackgroundTasks tasks,
                                     String customerNumber) {
        Button newLoan = new Button("New loan (temporary)");
        newLoan.getStyleClass().add("lavs-button-primary");
        newLoan.setOnAction(event -> {
            Customer customer = new Customer();
            customer.setNumber(customerNumber);
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

        HBox loanRow = new HBox(8, newLoan, loanNumber, openLoan);
        loanRow.setAlignment(Pos.CENTER_LEFT);
        return new VBox(4, loanRow, error);
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
