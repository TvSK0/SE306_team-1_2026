// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import lavs.logic.ConstantsService;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.RelationshipInput;
import lavs.logic.RelationshipListResult;
import lavs.logic.RelationshipResult;
import lavs.model.CustomerRelationship;
import lavs.validation.FieldError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

// Optional component: the Relationships tab of a customer record (customer-relationships.fxml).
// Lists the customer's relationships and adds, edits and removes them. The checks live in
// CustomerRelationshipService and the wording in RelationshipsText; this class only moves
// values between the screen and the service. Every call runs in the background.
public class RelationshipsController {
    private static final String FIELD_ERROR = "field-error";
    private static final String PROBLEM = "problem";
    private static final String ADD_TEXT = "Add relationship";
    private static final String EDIT_TEXT = "Save changes";
    private static final String NO_CONNECTION = "Couldn't reach the mainframe. Check your connection and try again.";

    @FXML private Label _messageLabel;
    @FXML private Label _emptyLabel;
    @FXML private Button _editButton;
    @FXML private Button _removeButton;
    @FXML private TableView<CustomerRelationship> _table;
    @FXML private TableColumn<CustomerRelationship, String> _typeColumn;
    @FXML private TableColumn<CustomerRelationship, String> _customerColumn;

    @FXML private Label _formTitle;
    @FXML private TextField _otherCustomerNumber;
    @FXML private ConstantDropdown _type;
    @FXML private VBox _otherTypeBox;
    @FXML private TextField _otherType;
    @FXML private Label _otherCustomerNumberError;
    @FXML private Label _typeError;
    @FXML private Label _otherTypeError;
    @FXML private Button _saveButton;

    // RelationshipInput's field names mapped to the control to highlight and its error label
    private final Map<String, Control> _fields = new HashMap<>();
    private final Map<String, Label> _errorLabels = new HashMap<>();

    private CustomerRelationshipService _service;
    private ConstantsService _constantsService;
    private BackgroundTasks _tasks;
    private String _customerNumber;
    // the relationship being edited, or null while the form adds a new one
    private CustomerRelationship _editing;
    // the list load still running, if any
    private BackgroundTasks.Handle _loading;

    // Loads the view on its own, for a screen that does not include it with fx:include.
    public static Parent open(CustomerRelationshipService service, ConstantsService constantsService,
                              BackgroundTasks tasks, String customerNumber) throws IOException {
        FXMLLoader loader = new FXMLLoader(RelationshipsController.class.getResource("customer-relationships.fxml"));
        Parent root = loader.load();
        RelationshipsController controller = loader.getController();
        controller.show(service, constantsService, tasks, customerNumber);
        return root;
    }

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        _fields.put(RelationshipInput.OTHER_CUSTOMER_NUMBER, _otherCustomerNumber);
        _fields.put(RelationshipInput.TYPE, _type);
        _fields.put(RelationshipInput.OTHER_TYPE, _otherType);
        _errorLabels.put(RelationshipInput.OTHER_CUSTOMER_NUMBER, _otherCustomerNumberError);
        _errorLabels.put(RelationshipInput.TYPE, _typeError);
        _errorLabels.put(RelationshipInput.OTHER_TYPE, _otherTypeError);

        showColumn(_typeColumn, RelationshipsText::type);
        showColumn(_customerColumn, RelationshipsText::otherCustomer);

        // Edit and Remove act on the selected row
        _editButton.disableProperty().bind(_table.getSelectionModel().selectedItemProperty().isNull());
        _removeButton.disableProperty().bind(_table.getSelectionModel().selectedItemProperty().isNull());
        // the description is only asked for when the type is "Other"
        _type.valueProperty().addListener((property, oldValue, newValue) -> showOtherTypeBox());
    }

    public void show(CustomerRelationshipService service, ConstantsService constantsService,
                     BackgroundTasks tasks, String customerNumber) {
        _service = service;
        _constantsService = constantsService;
        _tasks = tasks;
        _customerNumber = customerNumber;
        startAdding();
        load();
    }

    // Fetches the list again. A newer load replaces one still running, whose result is dropped.
    private void load() {
        if (_loading != null) {
            _loading.cancel();
        }
        _emptyLabel.setText("Loading relationships...");
        _loading = _tasks.run(
                () -> {
                    try {
                        return _service.listRelationships(_customerNumber);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                this::showList,
                failure -> showListFailure(failure instanceof UncheckedIOException
                        ? NO_CONNECTION
                        : "The relationships could not be loaded: " + failure.getMessage()));
    }

    private void showList(RelationshipListResult result) {
        if (!result.isSuccessful()) {
            showListFailure(RelationshipsText.forLoadFailure(result.getMainframeStatus()));
            return;
        }
        _emptyLabel.setText("No relationships yet.");
        _table.getItems().setAll(result.getRelationships());
    }

    private void showListFailure(String text) {
        _emptyLabel.setText("No relationships loaded.");
        _table.getItems().clear();
        showMessage(text, true);
    }

    @FXML
    private void onEdit() {
        CustomerRelationship selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        _editing = selected;
        clearErrors();
        hideMessage();
        _formTitle.setText("Edit relationship");
        _saveButton.setText(EDIT_TEXT);
        _otherCustomerNumber.setText(selected.getOtherCustomerNumber());
        _type.show(_constantsService, selected.getType());
        _otherType.setText(selected.getOtherType() == null ? "" : selected.getOtherType());
        showOtherTypeBox();
    }

    @FXML
    private void onRemove() {
        CustomerRelationship selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Remove the " + RelationshipsText.type(selected).toLowerCase() + " relationship with "
                        + RelationshipsText.otherCustomer(selected) + "?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Remove relationship");
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;

        hideMessage();
        _tasks.run(
                () -> {
                    try {
                        return _service.deleteRelationship(_customerNumber, selected.getId());
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                result -> {
                    showMessage(RelationshipsText.forResult(result, "Relationship removed."), !result.isSuccessful());
                    if (result.isSuccessful()) {
                        // the form may still hold the relationship that was just removed
                        if (_editing == selected) startAdding();
                        load();
                    }
                },
                failure -> showMessage(NO_CONNECTION, true));
    }

    // The save runs in the background; the button reads "Saving..." and cannot be pressed
    // again until the mainframe has answered.
    @FXML
    private void onSave() {
        RelationshipInput input = new RelationshipInput(
                _otherCustomerNumber.getText(), _type.getValue(), _otherType.getText());
        // read on the UI thread: the relationship must be the one shown when Save was clicked
        CustomerRelationship editing = _editing;
        clearErrors();
        hideMessage();
        LoadingView saving = new LoadingView(LoadingState.forSave(_saveButton.getText(), "Saving..."), _saveButton);
        _tasks.run(
                () -> save(editing, input),
                saving::start,
                result -> {
                    saving.finish();
                    showSaveResult(result, editing == null ? "Relationship added." : "Relationship saved.");
                },
                failure -> {
                    saving.finish();
                    showMessage(failure instanceof UncheckedIOException
                            ? NO_CONNECTION
                            : "The relationship could not be saved: " + failure.getMessage(), true);
                });
    }

    // runs in the background
    private RelationshipResult save(CustomerRelationship editing, RelationshipInput input) {
        try {
            if (editing == null) {
                return _service.addRelationship(_customerNumber, input);
            }
            return _service.updateRelationship(_customerNumber, editing.getId(), input);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void showSaveResult(RelationshipResult result, String doneText) {
        // highlight each bad field; everything the MMM typed stays in the form
        for (FieldError error : result.getValidationErrors().getErrors()) {
            showFieldError(error);
        }
        showMessage(RelationshipsText.forResult(result, doneText), !result.isSuccessful());
        if (result.isSuccessful()) {
            startAdding();
            load();
        }
    }

    // Clear empties the form and goes back to adding a new relationship
    @FXML
    private void onCancel() {
        hideMessage();
        startAdding();
    }

    private void startAdding() {
        _editing = null;
        clearErrors();
        _formTitle.setText("Add a relationship");
        _saveButton.setText(ADD_TEXT);
        _otherCustomerNumber.setText("");
        _type.show(_constantsService, null);
        _otherType.setText("");
        showOtherTypeBox();
    }

    private void showOtherTypeBox() {
        boolean isOther = RelationshipInput.TYPE_OTHER.equals(_type.getValue());
        _otherTypeBox.setVisible(isOther);
        _otherTypeBox.setManaged(isOther);
    }

    private void showFieldError(FieldError error) {
        Control control = _fields.get(error.getFieldName());
        // a customerNumber error has no field on this screen; the message above covers it
        if (control == null) return;
        control.getStyleClass().add(FIELD_ERROR);
        _errorLabels.get(error.getFieldName()).setText(capitalise(error.getMessage()));
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

    private static void showColumn(TableColumn<CustomerRelationship, String> column,
                                   Function<CustomerRelationship, String> text) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
    }
}
