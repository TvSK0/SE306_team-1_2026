package lavs.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.CustomerLoansService;
import lavs.model.Customer;
import java.io.IOException;
import java.io.UncheckedIOException;

// AI-assisted: the customer's loans, with the eligibility check before opening a new form.
public final class CustomerLoansPanel extends VBox {
    private final CustomerScreenServices services;
    private final Customer customer;
    private final TableView<CustomerLoansService.Row> table = new TableView<>();
    private final Label message = new Label();
    private final HBox actions = new HBox(12);
    private final Button open = new Button("Open loan");
    private BackgroundTasks.Handle loading;

    public CustomerLoansPanel(CustomerScreenServices services, Customer customer) {
        super(12);
        this.services = services;
        this.customer = customer;
        TableColumns.add(table, "Loan", CustomerLoansService.Row::number);
        TableColumns.add(table, "Principal", CustomerLoansService.Row::principal);
        TableColumns.add(table, "Start date", CustomerLoansService.Row::startDate);
        TableColumns.add(table, "Status", CustomerLoansService.Row::status);
        table.setPlaceholder(new Label("No loans to display"));
        VBox.setVgrow(table, Priority.ALWAYS);
        Button create = new Button("New loan");
        create.getStyleClass().add("lavs-button-primary");
        create.setOnAction(event -> startLoan());
        open.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        open.setOnAction(event -> openLoan());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> refresh());
        actions.getChildren().addAll(create, open, refresh);
        message.setWrapText(true);
        getChildren().addAll(message, table, actions);
        refresh();
    }

    public void refresh() {
        if (loading != null) loading.cancel();
        message.setText("Loading loans...");
        actions.setDisable(true);
        loading = services.tasks().run(() -> services.customerLoans().list(customer.getNumber()), result -> {
            actions.setDisable(false);
            if (result.isSuccessful()) { table.getItems().setAll(result.getValue()); message.setText(""); }
            else { table.getItems().clear(); message.setText(ResultText.forLoad(result, "Loans")); }
        }, this::failed);
    }

    private void startLoan() {
        actions.setDisable(true);
        message.setText("Checking customer details...");
        services.tasks().run(() -> services.eligibility().check(customer.getNumber()), result -> {
            actions.setDisable(false);
            if (result.isSuccessful() || (result.getOutcome() == lavs.logic.ServiceResult.Outcome.NETWORK_FAILURE
                    && services.loans().canSaveLocally())) {
                message.setText("");
                services.navigator().goTo(Screen.LOAN, LoanScreenRequest.forNewLoan(customer));
            } else if (!result.getValidationErrors().isValid()) {
                message.setText(result.getValidationErrors().getErrors().stream()
                        .map(error -> ResultText.capitalise(error.getMessage()))
                        .collect(java.util.stream.Collectors.joining("; ")));
            } else message.setText(ResultText.forLoad(result, "Customer eligibility"));
        }, this::failed);
    }

    private void openLoan() {
        var selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        actions.setDisable(true);
        message.setText("Opening loan...");
        services.tasks().run(() -> {
            try { return services.loans().loadLoan(selected.number()); }
            catch (IOException error) { throw new UncheckedIOException(error); }
        }, result -> {
            actions.setDisable(false);
            if (result.isSuccessful()) {
                message.setText("");
                services.navigator().goTo(Screen.LOAN, LoanScreenRequest.forLoan(customer, result.getLoan()));
            } else message.setText(LoanSaveMessage.forResult(result));
        }, this::failed);
    }

    private void failed(Throwable error) { actions.setDisable(false); message.setText(ResultText.forError(error)); }
}
