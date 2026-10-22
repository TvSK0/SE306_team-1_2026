package lavs.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.logic.CategoryValues;
import lavs.logic.LoanDetailsInput;
import lavs.logic.LoanSaveResult;
import lavs.logic.LoanService;
import lavs.logic.LoanOperations;
import lavs.model.Loan;
import lavs.validation.FieldError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// US6: the Details tab of a loan (loan-details.fxml), shown inside LoanScreenController,
// which owns the header and the tabs. Creates a new loan or edits an existing one. The checks themselves live in LoanService; this class only moves values
// between the form and the service, and shows the errors it gets back.
public class LoanDetailsController {
    private static final String FIELD_ERROR = "field-error";
    private static final String PROBLEM = "problem";
    private static final String SAVED = "saved";
    private static final String EMPTY = "empty";

    @FXML private Label _messageLabel;
    @FXML private Label _estimateLabel;
    @FXML private Label _savedStateLabel;

    @FXML private TextField _principal;
    @FXML private ConstantDropdown _rateType;
    @FXML private TextField _rateAmount;
    @FXML private ConstantDropdown _rateFrequency;
    @FXML private TextField _paymentAmount;
    @FXML private ConstantDropdown _paymentFrequency;
    @FXML private TextField _startDate;
    @FXML private TextField _renewalDate;

    @FXML private Label _principalError;
    @FXML private Label _rateTypeError;
    @FXML private Label _rateAmountError;
    @FXML private Label _rateFrequencyError;
    @FXML private Label _paymentAmountError;
    @FXML private Label _paymentFrequencyError;
    @FXML private Label _startDateError;
    @FXML private Label _renewalDateError;

    // LoanService's field names (LoanDetailsInput constants) mapped to the control to
    // highlight and the label that shows the message
    private final Map<String, Control> _fields = new HashMap<>();
    private final Map<String, Label> _errorLabels = new HashMap<>();

    @FXML private Button _saveButton;

    private BackgroundTasks _tasks;
    private LoadingView _saving;
    private LoanOperations _loanService;
    private CategoryValues _constantsService;
    private String _customerNumber;
    // told about the loan each time a save works
    private Consumer<Loan> _onSaved;
    // null until the loan has been saved for the first time
    private Loan _savedLoan;

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        addField(LoanDetailsInput.PRINCIPAL, _principal, _principalError);
        addField(LoanDetailsInput.RATE_TYPE, _rateType, _rateTypeError);
        addField(LoanDetailsInput.RATE_AMOUNT, _rateAmount, _rateAmountError);
        addField(LoanDetailsInput.RATE_FREQUENCY, _rateFrequency, _rateFrequencyError);
        addField(LoanDetailsInput.PAYMENT_AMOUNT, _paymentAmount, _paymentAmountError);
        addField(LoanDetailsInput.PAYMENT_FREQUENCY, _paymentFrequency, _paymentFrequencyError);
        addField(LoanDetailsInput.START_DATE, _startDate, _startDateError);
        addField(LoanDetailsInput.RENEWAL_DATE, _renewalDate, _renewalDateError);
        _saving = new LoadingView(LoadingState.forSave(_saveButton.getText(), "Saving..."), _saveButton);

        // the estimate follows the payment fields as the MMM types
        _paymentAmount.textProperty().addListener((property, oldValue, newValue) -> showEstimate());
        _paymentFrequency.valueProperty().addListener((property, oldValue, newValue) -> showEstimate());
    }

    // Pass existingLoan to edit it, or null to start a new loan.
    public void show(LoanOperations loanService, CategoryValues constantsService, BackgroundTasks tasks,
                     String customerNumber, Loan existingLoan, Consumer<Loan> onSaved) {
        _tasks = tasks;
        _loanService = loanService;
        _constantsService = constantsService;
        _customerNumber = customerNumber;
        _onSaved = onSaved;
        _savedLoan = existingLoan;
        showSavedState();
        fillFormFromSavedLoan();
    }

    // The save runs in the background so the window stays responsive; the button reads
    // "Saving..." and cannot be pressed again until the mainframe has answered.
    @FXML
    private void onSave() {
        LoanDetailsInput input = readForm();
        // read on the UI thread: the loan number must be the one shown when Save was clicked
        String loanNumber = _savedLoan == null ? null : _savedLoan.getLoanNumber();
        clearErrors();
        _tasks.run(
                () -> save(loanNumber, input),
                _saving::start,
                result -> {
                    _saving.finish();
                    showResult(result);
                },
                failure -> {
                    _saving.finish();
                    if (failure instanceof UncheckedIOException) {
                        showMessage("Couldn't reach the mainframe. Check your connection and try again.", true);
                    } else {
                        showMessage("The loan could not be saved: " + failure.getMessage(), true);
                    }
                });
    }

    // runs in the background; a new loan has no number yet
    private LoanSaveResult save(String loanNumber, LoanDetailsInput input) {
        try {
            if (loanNumber == null) {
                return _loanService.createLoan(_customerNumber, input);
            }
            return _loanService.updateLoan(_customerNumber, loanNumber, input);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void showResult(LoanSaveResult result) {
        // highlight each bad field; everything the MMM typed stays in the form
        for (FieldError error : result.getValidationErrors().getErrors()) {
            showFieldError(error);
        }
        showMessage(LoanSaveMessage.forResult(result), !result.isSuccessful() && !result.isQueued());

        if (result.isSuccessful() || result.isQueued()) {
            _savedLoan = result.getLoan();
            showSavedState();
            _onSaved.accept(_savedLoan);
        }
    }

    @FXML
    private void onCancel() {
        clearErrors();
        hideMessage();
        fillFormFromSavedLoan();
    }

    private void addField(String fieldName, Control control, Label errorLabel) {
        _fields.put(fieldName, control);
        _errorLabels.put(fieldName, errorLabel);
    }

    private void showSavedState() {
        _savedStateLabel.getStyleClass().remove(SAVED);
        if (_savedLoan == null) {
            _savedStateLabel.setText("Not saved yet");
        } else {
            _savedStateLabel.setText("Waiting to upload".equals(_savedLoan.getStatus()) ? "Saved locally; waiting to upload" : "Saved to mainframe");
            _savedStateLabel.getStyleClass().add(SAVED);
        }
    }

    private void showEstimate() {
        String text = RepaymentEstimate.describe(_paymentAmount.getText(), _paymentFrequency.getValue());
        _estimateLabel.setText(text);
        // the "enter an amount" prompt is shown smaller and grey
        _estimateLabel.getStyleClass().remove(EMPTY);
        if (text.equals(RepaymentEstimate.NO_AMOUNT)) {
            _estimateLabel.getStyleClass().add(EMPTY);
        }
    }

    private void fillFormFromSavedLoan() {
        LoanDetailsInput input;
        if (_savedLoan == null) {
            input = new LoanDetailsInput("", null, "", null, "", null, "", "");
        } else {
            input = LoanDetailsInput.fromLoan(_savedLoan);
        }
        _principal.setText(input.getPrincipal());
        _rateType.show(_constantsService, input.getRateType());
        _rateAmount.setText(input.getRateAmount());
        _rateFrequency.show(_constantsService, input.getRateFrequency());
        _paymentAmount.setText(input.getPaymentAmount());
        _paymentFrequency.show(_constantsService, input.getPaymentFrequency());
        _startDate.setText(input.getStartDate());
        _renewalDate.setText(input.getRenewalDate());
        // the listeners only fire on a change, so a blank form needs this too
        showEstimate();
    }

    private LoanDetailsInput readForm() {
        return new LoanDetailsInput(
                _principal.getText(),
                _rateType.getValue(),
                _rateAmount.getText(),
                _rateFrequency.getValue(),
                _paymentAmount.getText(),
                _paymentFrequency.getValue(),
                _startDate.getText(),
                _renewalDate.getText());
    }

    private void showFieldError(FieldError error) {
        Control control = _fields.get(error.getFieldName());
        Label label = _errorLabels.get(error.getFieldName());
        // customerNumber and loanNumber errors have no field on this screen
        if (control == null) return;
        control.getStyleClass().add(FIELD_ERROR);
        label.setText(capitalise(error.getMessage()));
    }

    private void clearErrors() {
        for (Control control : _fields.values()) {
            control.getStyleClass().remove(FIELD_ERROR);
        }
        for (Label label : _errorLabels.values()) {
            label.setText("");
        }
    }

    private void showMessage(String text, boolean isProblem) {
        _messageLabel.setText(text);
        _messageLabel.getStyleClass().remove(PROBLEM);
        if (isProblem) {
            _messageLabel.getStyleClass().add(PROBLEM);
        }
        _messageLabel.setManaged(true);
        _messageLabel.setVisible(true);
    }

    private void hideMessage() {
        _messageLabel.setManaged(false);
        _messageLabel.setVisible(false);
    }

    // Validators messages read "is required"; on screen they start a sentence
    private static String capitalise(String message) {
        return message.substring(0, 1).toUpperCase() + message.substring(1);
    }
}
