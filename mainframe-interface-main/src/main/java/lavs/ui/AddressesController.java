package lavs.ui;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lavs.logic.AddressInput;
import lavs.logic.AddressService;
import lavs.logic.ConstantsService;
import lavs.logic.ServiceResult;
import lavs.model.Address;

import java.util.List;
import java.util.Map;

// AI-assisted: US4, the Addresses tab of a customer record (customer-addresses.fxml). The
// list, edit and remove flow is in ContactListController; the checks are in AddressService.
public class AddressesController extends ContactListController<Address, AddressInput> {
    @FXML private TableColumn<Address, String> _typeColumn;
    @FXML private TableColumn<Address, String> _addressColumn;
    @FXML private TableColumn<Address, String> _flagsColumn;

    @FXML private ConstantDropdown _type;
    @FXML private VBox _otherTypeBox;
    @FXML private TextField _otherType;
    @FXML private TextField _line1;
    @FXML private TextField _line2;
    @FXML private TextField _suburb;
    @FXML private TextField _city;
    @FXML private TextField _postCode;
    @FXML private TextField _country;
    @FXML private CheckBox _primary;
    @FXML private CheckBox _mailing;
    @FXML private Label _typeError;
    @FXML private Label _otherTypeError;
    @FXML private Label _line1Error;
    @FXML private Label _cityError;
    @FXML private Label _postCodeError;
    @FXML private Label _countryError;

    private Map<String, Control> _fields;
    private Map<String, Label> _errorLabels;
    private AddressService _service;

    public void show(AddressService service, ConstantsService constants, BackgroundTasks tasks, String customerNumber) {
        _service = service;
        start(constants, tasks, customerNumber);
    }

    @Override
    protected ContactText.Kind kind() {
        return ContactText.Kind.ADDRESS;
    }

    @Override
    protected void setUpColumns() {
        showColumn(_typeColumn, address -> ContactText.type(address.getType(), address.getOtherType()));
        showColumn(_addressColumn, ContactText::oneLine);
        showColumn(_flagsColumn, ContactText::flags);
    }

    @Override
    protected void setUpForm() {
        _fields = Map.of(AddressInput.TYPE, _type, AddressInput.OTHER_TYPE, _otherType, AddressInput.LINE1, _line1,
                AddressInput.CITY, _city, AddressInput.POST_CODE, _postCode, AddressInput.COUNTRY, _country);
        _errorLabels = Map.of(AddressInput.TYPE, _typeError, AddressInput.OTHER_TYPE, _otherTypeError,
                AddressInput.LINE1, _line1Error, AddressInput.CITY, _cityError,
                AddressInput.POST_CODE, _postCodeError, AddressInput.COUNTRY, _countryError);
        // the description is only asked for when the type is "Other"
        _type.valueProperty().addListener((property, oldValue, newValue) -> showOtherTypeBox());
    }

    // a new address starts in New Zealand, where most of the bank's customers live
    @Override
    protected void fillForm(Address address, ConstantsService constants) {
        AddressInput input = address == null
                ? new AddressInput(null, "", "", "", "", "", "", AddressInput.NEW_ZEALAND, false, false)
                : AddressInput.fromAddress(address);
        _type.show(constants, input.getType());
        _otherType.setText(input.getOtherType());
        _line1.setText(input.getLine1());
        _line2.setText(input.getLine2());
        _suburb.setText(input.getSuburb());
        _city.setText(input.getCity());
        _postCode.setText(input.getPostCode());
        _country.setText(input.getCountry());
        _primary.setSelected(input.isPrimary());
        _mailing.setSelected(input.isMailing());
        showOtherTypeBox();
    }

    @Override
    protected AddressInput readForm() {
        return new AddressInput(_type.getValue(), _otherType.getText(), _line1.getText(), _line2.getText(),
                _suburb.getText(), _city.getText(), _postCode.getText(), _country.getText(),
                _primary.isSelected(), _mailing.isSelected());
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
    protected ServiceResult<List<Address>> list(String customerNumber) {
        return _service.listAddresses(customerNumber);
    }

    @Override
    protected ServiceResult<Address> add(String customerNumber, AddressInput input) {
        return _service.addAddress(customerNumber, input);
    }

    @Override
    protected ServiceResult<Address> update(String customerNumber, Address editing, AddressInput input) {
        return _service.updateAddress(customerNumber, editing.getId(), input);
    }

    @Override
    protected ServiceResult<Void> delete(String customerNumber, Address address) {
        return _service.deleteAddress(customerNumber, address.getId());
    }

    @Override
    protected String describe(Address address) {
        return ContactText.oneLine(address);
    }

    private void showOtherTypeBox() {
        boolean isOther = AddressInput.TYPE_OTHER.equals(_type.getValue());
        _otherTypeBox.setVisible(isOther);
        _otherTypeBox.setManaged(isOther);
    }
}
