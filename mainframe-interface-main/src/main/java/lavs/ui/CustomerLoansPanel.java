package lavs.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.CustomerLoansService;
import lavs.model.Customer;
import java.io.IOException;
import java.io.UncheckedIOException;

// AI-assisted: the customer's loans, with the eligibility check before opening a new form. The
// Figma 03a/08 details (role, money and date formats, tags, the blocked-loan banner and its link)
// come from LoanRowFormatter and NewLoanCheck, which are unit tested.
public final class CustomerLoansPanel extends VBox {
    private final CustomerScreenServices services;
    private final Customer customer;
    private final TableView<CustomerLoansService.Row> table = new TableView<>();
    private final Label message = new Label();
    private final HBox actions = new HBox(12);
    private final Button open = new Button("Open loan");
    private final Banner banner = new Banner();
    private final java.util.function.Consumer<NewLoanCheck.Fix> goToFix;
    // the customer's role on each loan, filled in after the list shows
    private java.util.Map<String, String> roles = java.util.Map.of();
    private boolean rolesKnown;
    private BackgroundTasks.Handle loading;

    public CustomerLoansPanel(CustomerScreenServices services, Customer customer) {
        this(services, customer, fix -> { });
    }

    // goToFix opens the record's Addresses or Phone numbers tab from the Figma 08 banner's link
    public CustomerLoansPanel(CustomerScreenServices services, Customer customer,
                              java.util.function.Consumer<NewLoanCheck.Fix> goToFix) {
        super(12);
        this.services = services;
        this.customer = customer;
        this.goToFix = goToFix;
        TableColumns.add(table, "Loan no.", CustomerLoansService.Row::number);
        TableCells.addTag(table, "Role", row -> LoanRowFormatter.roleTag(roles.get(row.number()), rolesKnown));
        TableCells.addTag(table, "Status", row -> LoanRowFormatter.statusTag(row.status()));
        TableColumns.add(table, "Principal", row -> LoanRowFormatter.money(row.principal()));
        TableColumns.add(table, "Start date", row -> LoanRowFormatter.date(row.startDate()));
        table.setPlaceholder(new Label("No loans yet."));
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
        getChildren().addAll(banner, message, table, actions);
        refresh();
    }

    public void refresh() {
        if (loading != null) loading.cancel();
        banner.hide();
        message.setText("Loading loans...");
        actions.setDisable(true);
        loading = services.tasks().run(() -> services.customerLoans().list(customer.getNumber()), result -> {
            actions.setDisable(false);
            if (result.isSuccessful()) {
                table.getItems().setAll(result.getValue());
                message.setText("");
                loadRoles(result.getValue());
            }
            else { table.getItems().clear(); message.setText(ResultText.forLoad(result, "Loans")); }
        }, this::failed);
    }

    // a second background pass: the table shows straight away, the Role column follows
    private void loadRoles(java.util.List<CustomerLoansService.Row> rows) {
        roles = java.util.Map.of();
        rolesKnown = false;
        table.refresh();
        var numbers = rows.stream().map(CustomerLoansService.Row::number).toList();
        services.tasks().run(() -> services.customerLoans().roles(customer.getNumber(), numbers), found -> {
            roles = found;
            rolesKnown = true;
            table.refresh();
        }, error -> {
            rolesKnown = true;
            table.refresh();
        });
    }

    private void startLoan() {
        banner.hide();
        actions.setDisable(true);
        message.setText("Checking customer details...");
        services.tasks().run(() -> services.eligibility().check(customer.getNumber()), result -> {
            actions.setDisable(false);
            if (result.isSuccessful() || (result.getOutcome() == lavs.logic.ServiceResult.Outcome.NETWORK_FAILURE
                    && services.loans().canSaveLocally())) {
                message.setText("");
                services.navigator().goTo(Screen.LOAN, LoanScreenRequest.forNewLoan(customer));
            } else if (!result.getValidationErrors().isValid()) {
                // Figma 08: what is missing, with a link to the tab that fixes it (US6 criterion 1)
                message.setText("");
                NewLoanCheck.blocked(customer.getFullName(), result.getValidationErrors()).ifPresentOrElse(
                        blocked -> banner.showWithLink(blocked.banner(), blocked.linkText(),
                                () -> goToFix.accept(blocked.fix())),
                        () -> message.setText(result.getValidationErrors().getErrors().stream()
                                .map(error -> ResultText.capitalise(error.getMessage()))
                                .collect(java.util.stream.Collectors.joining("; "))));
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
