package lavs.ui;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lavs.model.Customer;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

// AI-assisted: TEMPORARY customer record (customer-record.fxml) until Taran's exists. Shows
// the header and a tab per slice: Details (US3, CustomerDetailsController), Addresses, Phone
// numbers and Emails (US4) and Relationships (Yu's RelationshipsController). It loads the
// customer in the background, and each tab that reads from the mainframe only does so the
// first time it is opened.
public class CustomerRecordController {
    @FXML private Label _titleLabel;
    @FXML private Label _statusLabel;
    @FXML private Label _loadMessage;
    @FXML private TabPane _tabs;
    @FXML private Tab _detailsTab;
    @FXML private Tab _addressesTab;
    @FXML private Tab _phoneNumbersTab;
    @FXML private Tab _emailsTab;
    @FXML private Tab _relationshipsTab;

    // filled in by FXMLLoader from the fx:include elements
    @FXML private CustomerDetailsController _detailsController;
    @FXML private AddressesController _addressesController;
    @FXML private PhoneNumbersController _phoneNumbersController;
    @FXML private EmailsController _emailsController;
    @FXML private RelationshipsController _relationshipsController;

    // what each of the customer's other tabs does the first time it is opened
    private final Map<Tab, Runnable> _openers = new LinkedHashMap<>();
    private final Set<Tab> _opened = new HashSet<>();

    private CustomerScreenServices _services;
    // run by the Back link; may be null
    private Runnable _goBack;
    // null while adding a new customer, until they are saved
    private Customer _customer;

    // customerNumber is the customer to open, or null to add a new one
    public static Parent open(CustomerScreenServices services, String customerNumber, Runnable goBack)
            throws IOException {
        FXMLLoader loader = new FXMLLoader(CustomerRecordController.class.getResource("customer-record.fxml"));
        Parent root = loader.load();
        CustomerRecordController controller = loader.getController();
        controller.show(services, customerNumber, goBack);
        return root;
    }

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        _openers.put(_addressesTab, () -> _addressesController.show(_services.addresses(), _services.constants(),
                _services.tasks(), _customer.getNumber()));
        _openers.put(_phoneNumbersTab, () -> _phoneNumbersController.show(_services.phoneNumbers(),
                _services.constants(), _services.tasks(), _customer.getNumber()));
        _openers.put(_emailsTab, () -> _emailsController.show(_services.emails(), _services.constants(),
                _services.tasks(), _customer.getNumber()));
        _openers.put(_relationshipsTab, () -> _relationshipsController.show(_services.relationships(),
                _services.constants(), _services.tasks(), _customer.getNumber()));
        for (Tab tab : _openers.keySet()) {
            tab.setOnSelectionChanged(event -> {
                if (tab.isSelected()) {
                    openTab(tab);
                }
            });
        }
    }

    public void show(CustomerScreenServices services, String customerNumber, Runnable goBack) {
        _services = services;
        _goBack = goBack;
        if (customerNumber == null) {
            showCustomer(null);
            return;
        }
        _titleLabel.setText("Customer " + customerNumber);
        _statusLabel.setText("Loading...");
        _detailsTab.setDisable(true);
        _openers.keySet().forEach(tab -> tab.setDisable(true));
        services.tasks().run(
                () -> services.customers().loadCustomer(customerNumber),
                result -> {
                    if (result.isSuccessful()) {
                        showCustomer(result.getValue());
                    } else {
                        showLoadFailure(ResultText.forLoad(result, "The customer"));
                    }
                },
                failure -> showLoadFailure(ResultText.forError(failure)));
    }

    @FXML
    private void onBack() {
        if (_goBack != null) {
            _goBack.run();
        }
    }

    private void showCustomer(Customer customer) {
        _customer = customer;
        _detailsTab.setDisable(false);
        _detailsController.show(_services.customers(), _services.tasks(), customer, this::onSaved);
        showHeader();
    }

    // US3: once a new customer is saved, the rest of their record opens up
    private void onSaved(Customer saved) {
        _customer = saved;
        showHeader();
    }

    private void openTab(Tab tab) {
        if (_customer == null || _opened.contains(tab)) {
            return;
        }
        _openers.get(tab).run();
        _opened.add(tab);
    }

    private void showHeader() {
        _titleLabel.setText(CustomerText.title(_customer));
        _statusLabel.setText(CustomerText.status(_customer));
        // a customer the mainframe doesn't have yet can't have addresses, contacts or relationships
        _openers.keySet().forEach(tab -> tab.setDisable(_customer == null));
    }

    private void showLoadFailure(String text) {
        _statusLabel.setText("Not loaded");
        _loadMessage.setText(text);
        _loadMessage.setManaged(true);
        _loadMessage.setVisible(true);
    }
}
