package lavs.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lavs.logic.LoanRepaymentsResult;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanSummary;
import lavs.logic.RepaymentRow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

// US7: the Repayments tab of a loan (loan-repayments.fxml). Shows the summary and the
// full repayment schedule the mainframe works out from the loan details. Nothing is
// stored: LoanScreenController asks for a fresh load whenever the loan is saved.
public class LoanRepaymentsController {
    private static final String NO_VALUE = "-";

    @FXML private Label _messageLabel;
    @FXML private Label _emptyLabel;
    @FXML private Label _totalAmountPaid;
    @FXML private Label _totalInterestPaid;
    @FXML private Label _numberOfPayments;
    @FXML private Label _finalPaymentDate;
    @FXML private Label _finalPaymentAmount;

    @FXML private TableView<RepaymentRow> _scheduleTable;
    @FXML private TableColumn<RepaymentRow, String> _numberColumn;
    @FXML private TableColumn<RepaymentRow, String> _dateColumn;
    @FXML private TableColumn<RepaymentRow, String> _paymentColumn;
    @FXML private TableColumn<RepaymentRow, String> _principalColumn;
    @FXML private TableColumn<RepaymentRow, String> _interestColumn;
    @FXML private TableColumn<RepaymentRow, String> _remainingColumn;

    @FXML private Button _refreshButton;

    private LoanRepaymentsService _repaymentsService;
    private BackgroundTasks _tasks;
    private LoadingView _loading;
    // the load still running, if any
    private BackgroundTasks.Handle _running;
    private String _loanNumber;

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        showColumn(_numberColumn, row -> Long.toString(row.getPaymentNumber()));
        showColumn(_dateColumn, row -> RepaymentsText.date(row.getDate()));
        showColumn(_paymentColumn, row -> RepaymentsText.money(row.getAmountPaid()));
        showColumn(_principalColumn, row -> RepaymentsText.money(row.getPrincipalAmount()));
        showColumn(_interestColumn, row -> RepaymentsText.money(row.getInterestAmount()));
        showColumn(_remainingColumn, row -> RepaymentsText.money(row.getAmountRemaining()));
        // while a load runs Refresh reads "Loading..." and cannot be pressed again
        _loading = new LoadingView(LoadingState.forRead(_refreshButton.getText(), "Loading..."), _refreshButton);
    }

    public void show(LoanRepaymentsService repaymentsService, BackgroundTasks tasks) {
        _repaymentsService = repaymentsService;
        _tasks = tasks;
    }

    // Fetches the summary and schedule for the loan in the background and shows them.
    // On failure the previous values are cleared, so nothing out of date is left on screen.
    // A newer load (e.g. after another save) replaces one still running, whose result is dropped.
    public void load(String loanNumber) {
        _loanNumber = loanNumber;
        if (_running != null) {
            _running.cancel();
        }
        _running = _tasks.run(
                () -> {
                    try {
                        return _repaymentsService.load(loanNumber);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                _loading::start,
                result -> {
                    _loading.finish();
                    showResult(result);
                },
                failure -> {
                    _loading.finish();
                    if (failure instanceof UncheckedIOException) {
                        showFailure("Couldn't reach the mainframe. Check your connection and click Refresh.");
                    } else {
                        showFailure("The repayments could not be loaded: " + failure.getMessage());
                    }
                });
    }

    private void showResult(LoanRepaymentsResult result) {
        if (!result.isSuccessful()) {
            showFailure(RepaymentsText.forFailure(result.getReply()));
            return;
        }
        hideMessage();
        showSummary(result.getSummary());
        _emptyLabel.setText("The mainframe has no repayments for this loan.");
        _scheduleTable.getItems().setAll(result.getRows());
    }

    @FXML
    private void onRefresh() {
        if (_loanNumber != null) {
            load(_loanNumber);
        }
    }

    private void showSummary(LoanSummary summary) {
        _totalAmountPaid.setText(RepaymentsText.money(summary.getTotalAmountPaid()));
        _totalInterestPaid.setText(RepaymentsText.money(summary.getTotalInterestPaid()));
        _numberOfPayments.setText(Long.toString(summary.getNumberOfPayments()));
        _finalPaymentDate.setText(RepaymentsText.date(summary.getFinalPaymentDate()));
        _finalPaymentAmount.setText(RepaymentsText.money(summary.getFinalPaymentAmount()));
    }

    private void showFailure(String text) {
        _totalAmountPaid.setText(NO_VALUE);
        _totalInterestPaid.setText(NO_VALUE);
        _numberOfPayments.setText(NO_VALUE);
        _finalPaymentDate.setText(NO_VALUE);
        _finalPaymentAmount.setText(NO_VALUE);
        _emptyLabel.setText("No repayments loaded.");
        _scheduleTable.getItems().clear();
        _messageLabel.setText(text);
        _messageLabel.setManaged(true);
        _messageLabel.setVisible(true);
    }

    private void hideMessage() {
        _messageLabel.setManaged(false);
        _messageLabel.setVisible(false);
    }

    private static void showColumn(TableColumn<RepaymentRow, String> column, Function<RepaymentRow, String> text) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
    }
}
