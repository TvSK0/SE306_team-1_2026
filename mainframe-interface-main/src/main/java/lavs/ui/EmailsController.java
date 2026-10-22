package lavs.ui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lavs.logic.ConstantsService;
import lavs.logic.EmailInput;
import lavs.logic.EmailService;
import lavs.logic.ServiceResult;
import lavs.model.Email;

import java.util.List;
import java.util.Map;

// AI-assisted: US4, the Emails tab of a customer record (customer-emails.fxml). The list,
// edit and remove flow is in ContactListController; the checks are in EmailService.
public class EmailsController extends ContactListController<Email, EmailInput> {
    @FXML private TableColumn<Email, String> _typeColumn;
    @FXML private TableColumn<Email, String> _addressColumn;
    @FXML private TableColumn<Email, String> _flagsColumn;

    @FXML private ConstantDropdown _type;
    @FXML private VBox _otherTypeBox;
    @FXML private TextField _otherType;
    @FXML private TextField _address;
    @FXML private CheckBox _primary;
    @FXML private Label _typeError;
    @FXML private Label _otherTypeError;
    @FXML private Label _addressError;

    private Map<String, Control> _fields;
    private Map<String, Label> _errorLabels;
    private EmailService _service;

    public void show(EmailService service, ConstantsService constants, BackgroundTasks tasks, String customerNumber) {
        _service = service;
        start(constants, tasks, customerNumber);
    }

    @Override
    protected ContactText.Kind kind() {
        return ContactText.Kind.EMAIL;
    }

    @Override
    protected void setUpColumns() {
        showColumn(_typeColumn, email -> ContactText.type(email.getType(), email.getOtherType()));
        showColumn(_addressColumn, Email::getAddress);
        showColumn(_flagsColumn, ContactText::flags);
    }

    @Override
    protected void setUpForm() {
        _fields = Map.of(EmailInput.TYPE, _type, EmailInput.OTHER_TYPE, _otherType, EmailInput.ADDRESS, _address);
        _errorLabels = Map.of(EmailInput.TYPE, _typeError, EmailInput.OTHER_TYPE, _otherTypeError,
                EmailInput.ADDRESS, _addressError);
        // the description is only asked for when the type is "Other"
        _type.valueProperty().addListener((property, oldValue, newValue) -> showOtherTypeBox());
    }

    @Override
    protected void fillForm(Email email, ConstantsService constants) {
        EmailInput input = email == null ? new EmailInput(null, "", "", false) : EmailInput.fromEmail(email);
        _type.show(constants, input.getType());
        _otherType.setText(input.getOtherType());
        _address.setText(input.getAddress());
        _primary.setSelected(input.isPrimary());
        showOtherTypeBox();
    }

    @Override
    protected EmailInput readForm() {
        return new EmailInput(_type.getValue(), _otherType.getText(), _address.getText(), _primary.isSelected());
    }

    @Override
    protected Map<String, Control> fields() {
        return _fields;
    }

    @Override
    protected Map<String, Label> errorLabels() {
        return _errorLabels;
    }

    @Override
    protected ServiceResult<List<Email>> list(String customerNumber) {
        return _service.listEmails(customerNumber);
    }

    @Override
    protected ServiceResult<Email> add(String customerNumber, EmailInput input) {
        return _service.addEmail(customerNumber, input);
    }

    @Override
    protected ServiceResult<Email> update(String customerNumber, Email editing, EmailInput input) {
        return _service.updateEmail(customerNumber, editing.getId(), input);
    }

    @Override
    protected ServiceResult<Void> delete(String customerNumber, Email email) {
        return _service.deleteEmail(customerNumber, email.getId());
    }

    @Override
    protected String describe(Email email) {
        return email.getAddress();
    }

    private void showOtherTypeBox() {
        boolean isOther = EmailInput.TYPE_OTHER.equals(_type.getValue());
        _otherTypeBox.setVisible(isOther);
        _otherTypeBox.setManaged(isOther);
    }
}
