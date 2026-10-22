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

// AI-assisted: customer record and its independently loaded tabs. Shows
// the header and a tab per slice: Details (US3, CustomerDetailsController), Addresses, Phone
// numbers and Emails (US4) and Relationships (Yu's RelationshipsController). It loads the
// customer in the background, and each tab that reads from the mainframe only does so the
// first time it is opened.
public class CustomerRecordController {
    @FXML private Label _titleLabel;
    @FXML private Label _statusLabel;
    @FXML private Label _detailsLabel;
    @FXML private Banner _loadBanner;
    @FXML private TabPane _tabs;
    @FXML private Tab _loansTab;
    @FXML private Tab _notesTab;
    @FXML private Tab _documentsTab;
    @FXML private Tab _employersTab;
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
    // what the header and the top bar show (Figma 03a), decided in a plain tested class
    private CustomerHeaderState _header;
    // topBar::setTitle, for "Customer: Jane Smith (C-10234)"
    private java.util.function.Consumer<String> _setTopBarTitle = title -> { };

    // customerNumber is the customer to open, or null to add a new one
    public static Parent open(CustomerScreenServices services, String customerNumber, Runnable goBack)
            throws IOException {
        return open(services, customerNumber, goBack, title -> { });
    }

    // setTopBarTitle is topBar::setTitle: the record names the customer in the top bar once loaded
    public static Parent open(CustomerScreenServices services, String customerNumber, Runnable goBack,
                              java.util.function.Consumer<String> setTopBarTitle) throws IOException {
        FXMLLoader loader = new FXMLLoader(CustomerRecordController.class.getResource("customer-record.fxml"));
        Parent root = loader.load();
        CustomerRecordController controller = loader.getController();
        controller._setTopBarTitle = setTopBarTitle;
        controller.show(services, customerNumber, goBack);
        // Refresh the selected list when returning from a loan.
        class RecordView extends javafx.scene.layout.StackPane implements Refreshable {
            RecordView() { super(root); }
            public void onReturn() {
                // the Navigator puts the default title back; this names the customer again
                controller._setTopBarTitle.accept(controller._header.topBarTitle());
                controller._opened.remove(controller._tabs.getSelectionModel().getSelectedItem());
                controller.openTab(controller._tabs.getSelectionModel().getSelectedItem());
            }
        }
        return new RecordView();
    }

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        _openers.put(_documentsTab, () -> _documentsTab.setContent(new DocumentsPanel(_services.documents(),
                lavs.logic.RecordOwner.CUSTOMER, _customer.getNumber(), _services.constants(), _services.tasks())));
        _openers.put(_notesTab, () -> _notesTab.setContent(new NotesPanel(_services.notes(),
                lavs.logic.RecordOwner.CUSTOMER, _customer.getNumber(), _services.constants(), _services.tasks())));
        // the Figma 08 banner's "Go to Addresses →" / "Go to Phone numbers →" link opens that tab
        _openers.put(_loansTab, () -> _loansTab.setContent(new CustomerLoansPanel(_services, _customer,
                fix -> _tabs.getSelectionModel().select(fix == NewLoanCheck.Fix.ADDRESSES ? _addressesTab : _phoneNumbersTab))));
        _openers.put(_employersTab, () -> _employersTab.setContent(new EmployersPanel(_services.employers(),
                _services.tasks(), _customer.getNumber(), _services.constants())));
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
        _header = new CustomerHeaderState(customerNumber);
        if (customerNumber == null) {
            showCustomer(null);
            return;
        }
        showHeader();
        _detailsTab.setDisable(true);
        _openers.keySet().forEach(tab -> tab.setDisable(true));
        services.tasks().run(
                () -> services.customers().loadCustomer(customerNumber),
                result -> {
                    _header.applyLoad(result).ifPresent(_loadBanner::show);
                    if (result.isSuccessful()) {
                        showCustomer(result.getValue());
                    } else {
                        showHeader();
                    }
                },
                failure -> {
                    _loadBanner.show(new BannerContent(BannerVariant.ERROR, "Couldn't load the customer",
                            ResultText.forError(failure)));
                    showHeader();
                });
    }

    @FXML
    private void onBack() {
        if (_goBack != null) {
            _goBack.run();
        }
    }

    private void showCustomer(Customer customer) {
        _customer = customer;
        if (customer != null) {
            _header.setCustomer(customer);
        }
        _detailsTab.setDisable(false);
        _detailsController.show(_services.customers(), _services.tasks(), customer, this::onSaved);
        showHeader();
    }

    // US3: once a new customer is saved, the rest of their record opens up
    private void onSaved(Customer saved) {
        _customer = saved;
        _header.setCustomer(saved);
        showHeader();
    }

    private void openTab(Tab tab) {
        if (_customer == null || !_openers.containsKey(tab) || _opened.contains(tab)) {
            return;
        }
        _openers.get(tab).run();
        _opened.add(tab);
    }

    private void showHeader() {
        _titleLabel.setText(_header.name());
        _detailsLabel.setText(_header.detailsLine());
        _header.statusTag().ifPresentOrElse(tag -> {
            _statusLabel.setText(tag.text());
            _statusLabel.getStyleClass().setAll("label", "lavs-tag", tag.variant().getStyleClass());
            _statusLabel.setVisible(true);
        }, () -> _statusLabel.setVisible(false));
        _setTopBarTitle.accept(_header.topBarTitle());
        // a customer the mainframe doesn't have yet can't have addresses, contacts or relationships
        _openers.keySet().forEach(tab -> tab.setDisable(_customer == null));
    }
}
