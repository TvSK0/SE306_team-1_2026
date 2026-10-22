package lavs.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.CustomerSearchService;
import lavs.model.Customer;

// AI-assisted: US2 search and select mode share paging, loading, cancellation and errors.
public final class CustomerSearchScreen extends VBox {
    private final CustomerSearchService service;
    private final BackgroundTasks tasks;
    private final Navigator navigator;
    private final CustomerSelection selection;
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

    public CustomerSearchScreen(CustomerSearchService service, BackgroundTasks tasks, Navigator navigator,
                                CustomerSelection selection) {
        super(16);
        this.service = service;
        this.tasks = tasks;
        this.navigator = navigator;
        this.selection = selection;
        setPadding(new Insets(24));
        query.setPromptText("Name or customer number");
        HBox.setHgrow(query, Priority.ALWAYS);
        open = new Button(selection == null ? "Open customer" : "Select customer");
        open.getStyleClass().add("lavs-button-primary");
        search.getStyleClass().add("lavs-button-primary");
        TableColumns.add(table, "Number", Customer::getNumber);
        TableColumns.add(table, "First name", Customer::getFirstName);
        TableColumns.add(table, "Family name", Customer::getFamilyName);
        TableColumns.add(table, "Date of birth", Customer::getDateOfBirth);
        TableColumns.add(table, "Status", Customer::getStatus);
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
        if (selection == null) {
            Button create = new Button("New customer");
            create.setOnAction(event -> navigator.goTo(Screen.CUSTOMER_RECORD, null));
            actions.getChildren().add(create);
        } else {
            Button back = new Button("Back without selecting");
            back.setOnAction(event -> { if (running != null) running.cancel(); navigator.back(); });
            actions.getChildren().add(back);
        }
        message.setWrapText(true);
        getChildren().addAll(new HBox(12, query, search, cancel), message, table, actions);
        updateButtons();
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
            if (result.isSuccessful()) {
                page = result.getValue();
                table.getItems().setAll(page.customers());
                message.setText(page.total() + " customers · Page " + page.number());
            } else {
                page = null;
                message.setText(ResultText.forLoad(result, "Customers"));
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
        search.setDisable(busy);
        cancel.setDisable(!busy);
        previous.setDisable(busy || page == null || page.number() == 1);
        next.setDisable(busy || page == null || !page.hasNext());
        open.setDisable(busy || table.getSelectionModel().getSelectedItem() == null);
    }
}
