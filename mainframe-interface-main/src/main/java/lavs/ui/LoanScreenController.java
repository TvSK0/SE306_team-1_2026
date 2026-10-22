package lavs.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lavs.logic.LoanStatusResult;
import lavs.logic.TemporaryNumbers;
import lavs.model.Customer;
import lavs.model.Loan;

import java.io.IOException;
import java.io.UncheckedIOException;

// A loan (loan-screen.fxml): the header with the loan number, status and the US9 "Submit for
// verification" button, and the tabs. Details is US6 (LoanDetailsController), Borrowers is US8
// (LoanBorrowersController) and Repayments is US7 (LoanRepaymentsController). Other slices can
// add their tabs (Notes, Documents) to loan-screen.fxml.
public class LoanScreenController {
    private static final String PROBLEM = "problem";

    @FXML private Hyperlink _backLink;
    @FXML private Label _titleLabel;
    @FXML private Label _statusLabel;
    @FXML private Button _submitButton;
    @FXML private Label _submitMessage;
    @FXML private TabPane _tabs;
    @FXML private Tab _borrowersTab;
    @FXML private Tab _repaymentsTab;

    // filled in by FXMLLoader from the fx:include elements
    @FXML private LoanDetailsController _detailsController;
    @FXML private LoanBorrowersController _borrowersController;
    @FXML private LoanRepaymentsController _repaymentsController;

    private LoanScreenServices _services;
    // run by the "Back to <customer>" link; may be null
    private Runnable _goBack;
    // null until the loan has been saved for the first time
    private Loan _loan;
    // true when the Repayments tab shows the loan as it was last saved
    private boolean _repaymentsAreCurrent;
    // true once the Borrowers tab has read the list; after that it keeps itself up to date
    private boolean _borrowersLoaded;
    private LoadingView _submitting;

    // Loads the screen for a customer. Pass existingLoan to edit it, or null to start a
    // new loan. goBack is run when the MMM clicks "Back to <customer>", so the caller
    // decides where that goes. Call LoanService.checkCanStartLoan before opening a new
    // loan, so the MMM is told what the customer is missing first.
    public static Parent open(LoanScreenServices services, Customer customer, Loan existingLoan,
                              Runnable goBack) throws IOException {
        FXMLLoader loader = new FXMLLoader(LoanScreenController.class.getResource("loan-screen.fxml"));
        Parent root = loader.load();
        LoanScreenController controller = loader.getController();
        controller.show(services, customer, existingLoan, goBack);
        return root;
    }

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        // the schedule and the borrowers are fetched the first time their tab is opened, not before
        _repaymentsTab.setOnSelectionChanged(event -> {
            if (_repaymentsTab.isSelected()) {
                showCurrentRepayments();
            }
        });
        _borrowersTab.setOnSelectionChanged(event -> {
            if (_borrowersTab.isSelected()) {
                showBorrowers();
            }
        });
        // a status change is never cancelled half way, like a save
        _submitting = new LoadingView(LoadingState.forSave(_submitButton.getText(), "Submitting..."), _submitButton);
    }

    public void show(LoanScreenServices services, Customer customer, Loan existingLoan, Runnable goBack) {
        _services = services;
        _goBack = goBack;
        _loan = existingLoan;
        String name = customer.getFullName();
        _backLink.setText("← Back to " + (name.isEmpty() ? customer.getNumber() : name));
        _repaymentsController.show(services.repayments(), services.tasks());
        _borrowersController.show(services.borrowers(), services.tasks());
        _detailsController.show(services.loans(), services.constants(), services.tasks(),
                customer.getNumber(), existingLoan, this::onLoanSaved);
        showHeader();
    }

    @FXML
    private void onBack() {
        if (_goBack != null) {
            _goBack.run();
        }
    }

    // AI-assisted: US9. The checks run on the mainframe's current data, in the background, and
    // everything that stops the submission is listed under the header at once.
    @FXML
    private void onSubmit() {
        // read on the UI thread: the loan must be the one shown when the button was clicked
        String loanNumber = _loan.getLoanNumber();
        hideSubmitMessage();
        _services.tasks().run(
                () -> submit(loanNumber),
                _submitting::start,
                result -> {
                    _submitting.finish();
                    // AI-assisted: a submission kept on the upload queue counts too (US11), so the
                    // loan shows as Pending and can't be submitted twice
                    if (result.isSaved()) {
                        _loan.setStatus(result.getLoanStatus());
                    }
                    showHeader();
                    showSubmitMessage(LoanSubmission.messageFor(result), !result.isSaved());
                },
                failure -> {
                    _submitting.finish();
                    showHeader();
                    showSubmitMessage(LoanSubmission.messageForError(failure), true);
                });
    }

    // runs in the background
    private LoanStatusResult submit(String loanNumber) {
        try {
            return _services.statuses().submitForVerification(loanNumber);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // US6: once a loan is saved, its summary and repayments are shown
    private void onLoanSaved(Loan loan) {
        _loan = loan;
        _repaymentsAreCurrent = false;
        // an earlier submission message is about the loan as it was before this save
        hideSubmitMessage();
        showHeader();
        // AI-assisted: a new loan still waiting to upload has no schedule yet (US11)
        if (!isOnTheMainframe()) {
            return;
        }
        if (_repaymentsTab.isSelected()) {
            showCurrentRepayments();
        } else {
            _tabs.getSelectionModel().select(_repaymentsTab);
        }
    }

    // US8: AI-assisted
    private void showBorrowers() {
        if (_loan == null || _borrowersLoaded) {
            return;
        }
        _borrowersController.load(_loan.getLoanNumber());
        _borrowersLoaded = true;
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
        } else if (!isOnTheMainframe()) {
            // AI-assisted: its temporary number means nothing to the MMM, so don't show it (US11)
            _titleLabel.setText("New loan");
            _statusLabel.setText("Status: New (waiting to upload)");
        } else {
            _titleLabel.setText("Loan " + _loan.getLoanNumber());
            _statusLabel.setText("Status: " + _loan.getStatus());
        }
        // a loan has no schedule, and nobody can join it, until the mainframe has it
        _repaymentsTab.setDisable(!isOnTheMainframe());
        _borrowersTab.setDisable(!isOnTheMainframe());
        showSubmitButton();
    }

    // AI-assisted: false for no loan yet, or a new one still waiting on the upload queue (US11)
    private boolean isOnTheMainframe() {
        return _loan != null && !TemporaryNumbers.isTemporary(_loan.getLoanNumber());
    }

    // not managed when hidden, so the header does not leave a gap
    private void showSubmitButton() {
        boolean shown = LoanSubmission.isShown(_loan);
        _submitButton.setVisible(shown);
        _submitButton.setManaged(shown);
        _submitButton.setDisable(!LoanSubmission.isEnabled(_loan));
    }

    private void showSubmitMessage(String text, boolean isProblem) {
        _submitMessage.setText(text);
        _submitMessage.getStyleClass().remove(PROBLEM);
        if (isProblem) {
            _submitMessage.getStyleClass().add(PROBLEM);
        }
        _submitMessage.setManaged(true);
        _submitMessage.setVisible(true);
    }

    private void hideSubmitMessage() {
        _submitMessage.setManaged(false);
        _submitMessage.setVisible(false);
    }
}
