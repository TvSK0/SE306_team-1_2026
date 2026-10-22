package lavs.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lavs.logic.CustomerInput;
import lavs.logic.CustomerService;
import lavs.logic.ServiceResult;
import lavs.model.Customer;
import lavs.validation.FieldError;

import java.util.Map;
import java.util.function.Consumer;

// AI-assisted: US3, the Details tab of a customer record (customer-details.fxml). Creates a
// new customer or edits an existing one. The checks live in CustomerService and the
// wording in CustomerText and ResultText; this class only moves values between the form
// and the service. The save runs in the background.
public class CustomerDetailsController {
    private static final String FIELD_ERROR = "field-error";
    private static final String PROBLEM = "problem";

    @FXML private Label _messageLabel;
    @FXML private TextField _title;
    @FXML private TextField _firstName;
    @FXML private TextField _familyName;
    @FXML private TextField _dateOfBirth;
    @FXML private TextField _nationality;
    @FXML private Label _titleError;
    @FXML private Label _firstNameError;
    @FXML private Label _familyNameError;
    @FXML private Label _dateOfBirthError;
    @FXML private Label _nationalityError;
    @FXML private Button _saveButton;

    // CustomerInput's field names mapped to the field to highlight and its error label
    private Map<String, TextField> _fields;
    private Map<String, Label> _errorLabels;

    private CustomerService _service;
    private BackgroundTasks _tasks;
    // told about the customer each time a save works
    private Consumer<Customer> _onSaved;
    // null until the customer has been saved for the first time
    private Customer _saved;

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        _fields = Map.of(CustomerInput.TITLE, _title, CustomerInput.FIRST_NAME, _firstName,
                CustomerInput.FAMILY_NAME, _familyName, CustomerInput.DATE_OF_BIRTH, _dateOfBirth,
                CustomerInput.NATIONALITY, _nationality);
        _errorLabels = Map.of(CustomerInput.TITLE, _titleError, CustomerInput.FIRST_NAME, _firstNameError,
                CustomerInput.FAMILY_NAME, _familyNameError, CustomerInput.DATE_OF_BIRTH, _dateOfBirthError,
                CustomerInput.NATIONALITY, _nationalityError);
    }

    // Pass the saved customer to edit them, or null to add a new one.
    public void show(CustomerService service, BackgroundTasks tasks, Customer customer, Consumer<Customer> onSaved) {
        _service = service;
        _tasks = tasks;
        _onSaved = onSaved;
        _saved = customer;
        fillForm();
    }

    @FXML
    private void onSave() {
        CustomerInput input = readForm();
        // read on the UI thread: the customer must be the one shown when Save was clicked
        Customer before = _saved;
        clearErrors();
        hideMessage();
        LoadingView saving = new LoadingView(LoadingState.forSave(_saveButton.getText(), "Saving..."), _saveButton);
        _tasks.run(
                () -> before == null
                        ? _service.createCustomer(input)
                        : _service.updateCustomer(before.getNumber(), input),
                saving::start,
                result -> {
                    saving.finish();
                    showResult(before, result);
                },
                failure -> {
                    saving.finish();
                    showMessage(ResultText.forError(failure), true);
                });
    }

    // Undo changes puts back what was last saved (or empties the form for a new customer)
    @FXML
    private void onCancel() {
        clearErrors();
        hideMessage();
        fillForm();
    }

    private void showResult(Customer before, ServiceResult<Customer> result) {
        // highlight each bad field; everything the MMM typed stays in the form
        for (FieldError error : result.getValidationErrors().getErrors()) {
            showFieldError(error);
        }
        // AI-assisted: a save kept on the upload queue is saved too (US11)
        String done = result.isSaved() ? CustomerText.saved(before, result.getValue()) : null;
        showMessage(ResultText.forChange(result, done), !result.isSaved());
        if (result.isSaved()) {
            Customer saved = result.getValue();
            // a change saved through the queue doesn't bring the status back, so keep the one shown
            if (saved.getStatus() == null && before != null) {
                saved.setStatus(before.getStatus());
            }
            _saved = saved;
            fillForm();
            _onSaved.accept(_saved);
        }
    }

    private void fillForm() {
        CustomerInput input = _saved == null
                ? new CustomerInput("", "", "", "", "")
                : CustomerInput.fromCustomer(_saved);
        _title.setText(input.getTitle());
        _firstName.setText(input.getFirstName());
        _familyName.setText(input.getFamilyName());
        _dateOfBirth.setText(input.getDateOfBirth());
        _nationality.setText(input.getNationality());
        _saveButton.setText(CustomerText.saveButton(_saved));
    }

    private CustomerInput readForm() {
        return new CustomerInput(_title.getText(), _firstName.getText(), _familyName.getText(),
                _dateOfBirth.getText(), _nationality.getText());
    }

    private void showFieldError(FieldError error) {
        TextField field = _fields.get(error.getFieldName());
        // a customerNumber error has no field on this form; the message above still shows
        if (field == null) {
            return;
        }
        field.getStyleClass().add(FIELD_ERROR);
        _errorLabels.get(error.getFieldName()).setText(ResultText.capitalise(error.getMessage()));
    }

    private void clearErrors() {
        _fields.values().forEach(field -> field.getStyleClass().remove(FIELD_ERROR));
        _errorLabels.values().forEach(label -> label.setText(""));
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
}
