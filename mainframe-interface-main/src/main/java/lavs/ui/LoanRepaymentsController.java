// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lavs.logic.LoanRepaymentsResult;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanSummary;
import lavs.logic.RepaymentRow;

import java.io.IOException;
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

    private LoanRepaymentsService _repaymentsService;
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
    }

    public void show(LoanRepaymentsService repaymentsService) {
        _repaymentsService = repaymentsService;
    }

    // Fetches the summary and schedule for the loan and shows them. On failure the
    // previous values are cleared, so nothing out of date is left on screen.
    // TEMPORARY: swap this for running on Taran's background-task helper once it exists;
    // until then the window waits while the pages load.
    public void load(String loanNumber) {
        _loanNumber = loanNumber;
        LoanRepaymentsResult result;
        try {
            result = _repaymentsService.load(loanNumber);
        } catch (IOException e) {
            showFailure("Couldn't reach the mainframe. Check your connection and click Refresh.");
            return;
        }
        if (!result.isSuccessful()) {
            showFailure(RepaymentsText.forFailure(result.getMainframeStatus()));
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
