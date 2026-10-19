// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.logic.ConstantsService;
import lavs.logic.LoanDetailsInput;
import lavs.logic.LoanSaveResult;
import lavs.logic.LoanService;
import lavs.model.Customer;
import lavs.model.Loan;
import lavs.validation.FieldError;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// US6: the Details tab of a loan (loan-details.fxml). Creates a new loan or edits an
// existing one. The checks themselves live in LoanService; this class only moves values
// between the form and the service, and shows the errors it gets back.
public class LoanDetailsController {
    private static final String FIELD_ERROR = "field-error";
    private static final String PROBLEM = "problem";
    private static final String SAVED = "saved";
    private static final String EMPTY = "empty";

    @FXML private Hyperlink _backLink;
    @FXML private Label _titleLabel;
    @FXML private Label _statusLabel;
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

    private LoanService _loanService;
    private ConstantsService _constantsService;
    private String _customerNumber;
    // run by the "Back to <customer>" link; may be null
    private Runnable _goBack;
    // null until the loan has been saved for the first time
    private Loan _savedLoan;

    // Loads the screen for a customer. Pass existingLoan to edit it, or null to start a
    // new loan. goBack is run when the MMM clicks "Back to <customer>", so the caller
    // decides where that goes. Call LoanService.checkCanStartLoan before opening a new
    // loan, so the MMM is told what the customer is missing first.
    public static Parent open(LoanService loanService, ConstantsService constantsService,
                              Customer customer, Loan existingLoan, Runnable goBack) throws IOException {
        FXMLLoader loader = new FXMLLoader(LoanDetailsController.class.getResource("loan-details.fxml"));
        Parent root = loader.load();
        LoanDetailsController controller = loader.getController();
        controller.show(loanService, constantsService, customer, existingLoan, goBack);
        return root;
    }

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

        // the estimate follows the payment fields as the MMM types
        _paymentAmount.textProperty().addListener((property, oldValue, newValue) -> showEstimate());
        _paymentFrequency.valueProperty().addListener((property, oldValue, newValue) -> showEstimate());
    }

    public void show(LoanService loanService, ConstantsService constantsService,
                     Customer customer, Loan existingLoan, Runnable goBack) {
        _loanService = loanService;
        _constantsService = constantsService;
        _customerNumber = customer.getNumber();
        _goBack = goBack;
        _savedLoan = existingLoan;
        String name = customer.getFullName();
        _backLink.setText("← Back to " + (name.isEmpty() ? _customerNumber : name));
        showHeader();
        fillFormFromSavedLoan();
    }

    @FXML
    private void onBack() {
        if (_goBack != null) {
            _goBack.run();
        }
    }

    @FXML
    private void onSave() {
        LoanDetailsInput input = readForm();
        clearErrors();

        LoanSaveResult result;
        try {
            if (_savedLoan == null) {
                result = _loanService.createLoan(_customerNumber, input);
            } else {
                result = _loanService.updateLoan(_customerNumber, _savedLoan.getLoanNumber(), input);
            }
        } catch (IOException e) {
            showMessage("Couldn't reach the mainframe. Check your connection and try again.", true);
            return;
        }

        // highlight each bad field; everything the MMM typed stays in the form
        for (FieldError error : result.getValidationErrors().getErrors()) {
            showFieldError(error);
        }
        showMessage(LoanSaveMessage.forResult(result), !result.isSuccessful());

        if (result.isSuccessful()) {
            _savedLoan = result.getLoan();
            showHeader();
            // TEMPORARY: swap this for opening the Repayments tab once US7's screen exists.
        }
    }

    // TEMPORARY: swap this for going back to the customer record once Taran's
    // navigation exists. Until then Cancel puts back the last saved values.
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

    private void showHeader() {
        _savedStateLabel.getStyleClass().remove(SAVED);
        if (_savedLoan == null) {
            _titleLabel.setText("New loan");
            _statusLabel.setText("Status: New (not saved yet)");
            _savedStateLabel.setText("Not saved yet");
        } else {
            _titleLabel.setText("Loan " + _savedLoan.getLoanNumber());
            _statusLabel.setText("Status: " + _savedLoan.getStatus());
            _savedStateLabel.setText("Saved to mainframe");
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
