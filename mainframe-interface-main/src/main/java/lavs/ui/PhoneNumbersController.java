package lavs.ui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lavs.logic.CategoryValues;
import lavs.logic.PhoneNumberInput;
import lavs.logic.PhoneNumberService;
import lavs.logic.ServiceResult;
import lavs.model.PhoneNumber;

import java.util.List;
import java.util.Map;

// AI-assisted: US4, the Phone numbers tab of a customer record (customer-phone-numbers.fxml).
// The list, edit and remove flow is in ContactListController; the checks are in
// PhoneNumberService.
public class PhoneNumbersController extends ContactListController<PhoneNumber, PhoneNumberInput> {
    // a new number starts with New Zealand's code
    private static final String NEW_ZEALAND_CODE = "+64";

    @FXML private TableColumn<PhoneNumber, String> _typeColumn;
    @FXML private TableColumn<PhoneNumber, String> _numberColumn;
    @FXML private TableColumn<PhoneNumber, String> _flagsColumn;

    @FXML private ConstantDropdown _type;
    @FXML private VBox _otherTypeBox;
    @FXML private TextField _otherType;
    @FXML private TextField _countryCode;
    @FXML private TextField _number;
    @FXML private CheckBox _primary;
    @FXML private CheckBox _canTxt;
    @FXML private Label _typeError;
    @FXML private Label _otherTypeError;
    @FXML private Label _countryCodeError;
    @FXML private Label _numberError;

    private Map<String, Control> _fields;
    private Map<String, Label> _errorLabels;
    private PhoneNumberService _service;

    public void show(PhoneNumberService service, CategoryValues constants, BackgroundTasks tasks,
                     String customerNumber) {
        _service = service;
        start(constants, tasks, customerNumber);
    }

    @Override
    protected ContactText.Kind kind() {
        return ContactText.Kind.PHONE_NUMBER;
    }

    @Override
    protected void setUpColumns() {
        showColumn(_typeColumn, phone -> ContactText.type(phone.getType(), phone.getOtherType()));
        showColumn(_numberColumn, ContactText::number);
        showColumn(_flagsColumn, ContactText::flags);
    }

    @Override
    protected void setUpForm() {
        _fields = Map.of(PhoneNumberInput.TYPE, _type, PhoneNumberInput.OTHER_TYPE, _otherType,
                PhoneNumberInput.COUNTRY_CODE, _countryCode, PhoneNumberInput.NUMBER, _number);
        _errorLabels = Map.of(PhoneNumberInput.TYPE, _typeError, PhoneNumberInput.OTHER_TYPE, _otherTypeError,
                PhoneNumberInput.COUNTRY_CODE, _countryCodeError, PhoneNumberInput.NUMBER, _numberError);
        // the description is only asked for when the type is "Other"
        _type.valueProperty().addListener((property, oldValue, newValue) -> showOtherTypeBox());
    }

    @Override
    protected void fillForm(PhoneNumber phone, CategoryValues constants) {
        PhoneNumberInput input = phone == null
                ? new PhoneNumberInput(null, "", NEW_ZEALAND_CODE, "", false, false)
                : PhoneNumberInput.fromPhoneNumber(phone);
        _type.show(constants, input.getType());
        _otherType.setText(input.getOtherType());
        _countryCode.setText(input.getCountryCode());
        _number.setText(input.getNumber());
        _primary.setSelected(input.isPrimary());
        _canTxt.setSelected(input.canTxt());
        showOtherTypeBox();
    }

    @Override
    protected PhoneNumberInput readForm() {
        return new PhoneNumberInput(_type.getValue(), _otherType.getText(), _countryCode.getText(), _number.getText(),
                _primary.isSelected(), _canTxt.isSelected());
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
    protected ServiceResult<List<PhoneNumber>> list(String customerNumber) {
        return _service.listPhoneNumbers(customerNumber);
    }

    @Override
    protected ServiceResult<PhoneNumber> add(String customerNumber, PhoneNumberInput input) {
        return _service.addPhoneNumber(customerNumber, input);
    }

    @Override
    protected ServiceResult<PhoneNumber> update(String customerNumber, PhoneNumber editing, PhoneNumberInput input) {
        return _service.updatePhoneNumber(customerNumber, editing.getId(), input);
    }

    @Override
    protected ServiceResult<Void> delete(String customerNumber, PhoneNumber phone) {
        return _service.deletePhoneNumber(customerNumber, phone.getId());
    }

    @Override
    protected String describe(PhoneNumber phone) {
        return ContactText.number(phone);
    }

    private void showOtherTypeBox() {
        boolean isOther = PhoneNumberInput.TYPE_OTHER.equals(_type.getValue());
        _otherTypeBox.setVisible(isOther);
        _otherTypeBox.setManaged(isOther);
    }
}
