package lavs.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.CustomerSearchService;
import lavs.model.Customer;

// AI-assisted: US2 search and select mode share paging, loading, cancellation and errors. The
// Figma 02/09/12 details (heading, field label, columns, count line, empty text, tags, skeleton
// rows and the offline state) come from CustomerSearchText, which is unit tested.
public final class CustomerSearchScreen extends VBox {
    private final CustomerSearchService service;
    private final BackgroundTasks tasks;
    private final Navigator navigator;
    private final CustomerSelection selection;
    private final StatusFeed feed;
    private final Banner offline = new Banner();
    private final SkeletonRows skeleton = new SkeletonRows(4);
    private final FormField queryField;
    private final FieldErrorPresenter presenter;
    private final TextField query = new TextField();
    private final Label message = new Label("Search by customer name or number.");
    private final TableView<Customer> table = new TableView<>();
    private final Button search = new Button("Search");
    private final Button previous = new Button("Previous");
    private final Button next = new Button("Next");
    private final Button cancel = new Button("Cancel search");
    private final Button open;
    private BackgroundTasks.Handle running;
    private String submittedQuery;
    private CustomerSearchService.Page page;
    private boolean busy;

    // with no status feed the screen assumes it is online (tests and previews)
    public CustomerSearchScreen(CustomerSearchService service, BackgroundTasks tasks, Navigator navigator,
                                CustomerSelection selection) {
        this(service, tasks, navigator, selection, new FixedStatusFeed(true, 0));
    }

    // feed says whether the app is online: searching needs a connection (US2-R5)
    public CustomerSearchScreen(CustomerSearchService service, BackgroundTasks tasks, Navigator navigator,
                                CustomerSelection selection, StatusFeed feed) {
        super(16);
        this.service = service;
        this.tasks = tasks;
        this.navigator = navigator;
        this.selection = selection;
        this.feed = feed;
        setPadding(new Insets(24));
        query.setPromptText("Name or customer number");
        queryField = new FormField(CustomerSearchText.FIELD_LABEL, query);
        presenter = new FieldErrorPresenter(java.util.Map.of("search", queryField));
        HBox.setHgrow(queryField, Priority.ALWAYS);
        open = new Button(selection == null ? "Open customer" : "Select customer");
        open.getStyleClass().add("lavs-button-primary");
        search.getStyleClass().add("lavs-button-primary");
        TableColumns.add(table, "Customer no.", Customer::getNumber);
        TableColumns.add(table, "Name", Customer::getFullName);
        TableColumns.add(table, "Date of birth", customer -> CustomerSearchText.dateOfBirth(customer.getDateOfBirth()));
        TableCells.addTag(table, "Status", customer -> CustomerSearchText.statusTag(customer.getStatus()));
        TableCells.addLink(table, selection == null ? "Open record" : "Select", customer -> {
            table.getSelectionModel().select(customer);
            if (!busy) choose();
        });
        table.setPlaceholder(new Label("No customers to display"));
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getSelectionModel().selectedItemProperty().addListener((o, before, after) -> updateButtons());
        table.setRowFactory(view -> {
            TableRow<Customer> row = new TableRow<>();
            row.setOnMouseClicked(event -> { if (event.getClickCount() == 2 && !row.isEmpty() && !busy) choose(); });
            return row;
        });
        search.setOnAction(event -> { submittedQuery = query.getText(); load(1); });
        query.setOnAction(search.getOnAction());
        previous.setOnAction(event -> load(page.number() - 1));
        next.setOnAction(event -> load(page.number() + 1));
        cancel.setOnAction(event -> {
            if (running != null) running.cancel();
            busy = false;
            message.setText("Search cancelled.");
            updateButtons();
        });
        open.setOnAction(event -> choose());
        HBox actions = new HBox(12, previous, next, open);
        SectionHeading heading = SectionHeading.page(
                selection == null ? CustomerSearchText.TITLE : CustomerSearchText.SELECT_TITLE,
                selection == null ? CustomerSearchText.SUBTITLE : null);
        if (selection == null) {
            // stays enabled offline: an offline MMM can still enter a new customer (US2-R5)
            heading.setAction(CustomerSearchText.ADD_NEW, () -> navigator.goTo(Screen.CUSTOMER_RECORD, null));
        } else {
            Button back = new Button("Back without selecting");
            back.setOnAction(event -> { if (running != null) running.cancel(); navigator.back(); });
            actions.getChildren().add(back);
        }
        message.setWrapText(true);
        HBox searchRow = new HBox(12, queryField, search, cancel);
        searchRow.setAlignment(javafx.geometry.Pos.BOTTOM_LEFT);
        getChildren().addAll(heading, offline, searchRow, message, skeleton, table, actions);
        updateButtons();
        feed.onChange(() -> javafx.application.Platform.runLater(this::updateButtons));
    }

    private void load(long number) {
        if (running != null) running.cancel();
        busy = true;
        table.getItems().clear();
        message.setText("Searching...");
        updateButtons();
        String term = submittedQuery;
        running = tasks.run(() -> service.search(term, number), result -> {
            busy = false;
            presenter.present(result.getValidationErrors());
            if (result.isSuccessful()) {
                page = result.getValue();
                table.getItems().setAll(page.customers());
                table.setPlaceholder(new Label(CustomerSearchText.emptyText(term.trim())));
                message.setText(CustomerSearchText.countLine(page.total(), page.number(),
                        CustomerSearchService.PAGE_SIZE));
            } else {
                page = null;
                // a blank query is shown on the field itself (US2-R4)
                message.setText(result.getOutcome() == null ? "" : ResultText.forLoad(result, "Customers"));
            }
            updateButtons();
        }, error -> { busy = false; page = null; message.setText(ResultText.forError(error)); updateButtons(); });
    }

    private void choose() {
        Customer customer = table.getSelectionModel().getSelectedItem();
        if (customer == null) return;
        if (selection == null) navigator.goTo(Screen.CUSTOMER_RECORD, customer.getNumber());
        else { navigator.back(); selection.selected().accept(customer); }
    }

    private void updateButtons() {
        boolean blocked = CustomerSearchText.searchDisabled(feed.isOnline(), busy);
        offline.hide();
        CustomerSearchText.offlineBanner(feed.isOnline()).ifPresent(offline::show);
        skeleton.setVisible(busy);
        skeleton.setManaged(busy);
        search.setDisable(blocked);
        cancel.setDisable(!busy);
        previous.setDisable(blocked || page == null || page.number() == 1);
        next.setDisable(blocked || page == null || !page.hasNext());
        open.setDisable(busy || table.getSelectionModel().getSelectedItem() == null);
    }
}
