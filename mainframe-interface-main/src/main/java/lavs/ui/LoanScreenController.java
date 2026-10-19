// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lavs.logic.ConstantsService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanService;
import lavs.model.Customer;
import lavs.model.Loan;

import java.io.IOException;

// A loan (loan-screen.fxml): the header with the loan number and status, and the tabs.
// Details is US6 (LoanDetailsController) and Repayments is US7 (LoanRepaymentsController).
// Other slices can add their tabs (Borrowers, Notes, Documents) to loan-screen.fxml.
public class LoanScreenController {
    @FXML private Hyperlink _backLink;
    @FXML private Label _titleLabel;
    @FXML private Label _statusLabel;
    @FXML private TabPane _tabs;
    @FXML private Tab _repaymentsTab;

    // filled in by FXMLLoader from the fx:include elements
    @FXML private LoanDetailsController _detailsController;
    @FXML private LoanRepaymentsController _repaymentsController;

    // run by the "Back to <customer>" link; may be null
    private Runnable _goBack;
    // null until the loan has been saved for the first time
    private Loan _loan;
    // true when the Repayments tab shows the loan as it was last saved
    private boolean _repaymentsAreCurrent;

    // Loads the screen for a customer. Pass existingLoan to edit it, or null to start a
    // new loan. goBack is run when the MMM clicks "Back to <customer>", so the caller
    // decides where that goes. Call LoanService.checkCanStartLoan before opening a new
    // loan, so the MMM is told what the customer is missing first.
    public static Parent open(LoanService loanService, LoanRepaymentsService repaymentsService,
                              ConstantsService constantsService, BackgroundTasks tasks, Customer customer,
                              Loan existingLoan, Runnable goBack) throws IOException {
        FXMLLoader loader = new FXMLLoader(LoanScreenController.class.getResource("loan-screen.fxml"));
        Parent root = loader.load();
        LoanScreenController controller = loader.getController();
        controller.show(loanService, repaymentsService, constantsService, tasks, customer, existingLoan, goBack);
        return root;
    }

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        // the schedule is fetched the first time the tab is opened, not before
        _repaymentsTab.setOnSelectionChanged(event -> {
            if (_repaymentsTab.isSelected()) {
                showCurrentRepayments();
            }
        });
    }

    public void show(LoanService loanService, LoanRepaymentsService repaymentsService,
                     ConstantsService constantsService, BackgroundTasks tasks, Customer customer,
                     Loan existingLoan, Runnable goBack) {
        _goBack = goBack;
        _loan = existingLoan;
        String name = customer.getFullName();
        _backLink.setText("← Back to " + (name.isEmpty() ? customer.getNumber() : name));
        _repaymentsController.show(repaymentsService, tasks);
        _detailsController.show(loanService, constantsService, tasks, customer.getNumber(), existingLoan, this::onLoanSaved);
        showHeader();
    }

    @FXML
    private void onBack() {
        if (_goBack != null) {
            _goBack.run();
        }
    }

    // US6: once a loan is saved, its summary and repayments are shown
    private void onLoanSaved(Loan loan) {
        _loan = loan;
        _repaymentsAreCurrent = false;
        showHeader();
        if (_repaymentsTab.isSelected()) {
            showCurrentRepayments();
        } else {
            _tabs.getSelectionModel().select(_repaymentsTab);
        }
    }

    private void showCurrentRepayments() {
        if (_loan == null || _repaymentsAreCurrent) {
            return;
        }
        _repaymentsController.load(_loan.getLoanNumber());
        _repaymentsAreCurrent = true;
    }

    private void showHeader() {
        if (_loan == null) {
            _titleLabel.setText("New loan");
            _statusLabel.setText("Status: New (not saved yet)");
        } else {
            _titleLabel.setText("Loan " + _loan.getLoanNumber());
            _statusLabel.setText("Status: " + _loan.getStatus());
        }
        // a loan has no schedule until the mainframe has it
        _repaymentsTab.setDisable(_loan == null);
    }
}
