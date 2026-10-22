package lavs.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import lavs.logic.ConstantsService;
import lavs.logic.ServiceResult;
import lavs.logic.TemporaryNumbers;
import lavs.validation.FieldError;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

// AI-assisted: US4, what the Addresses, Phone numbers and Emails tabs have in common: a
// table of the customer's records with Edit and Remove, and a form beside it that adds a
// new one or saves changes to the one being edited. Same layout and flow as Yu's
// Relationships tab. Every call runs through BackgroundTasks, and the list is read again
// after each change. Each tab's controller supplies its columns, its form and its service
// calls; T is the record (e.g. Address) and I what the form produces (e.g. AddressInput).
public abstract class ContactListController<T, I> {
    private static final String FIELD_ERROR = "field-error";
    private static final String PROBLEM = "problem";

    @FXML private Label _messageLabel;
    @FXML private Label _emptyLabel;
    @FXML private TableView<T> _table;
    @FXML private Button _editButton;
    @FXML private Button _removeButton;
    @FXML private Label _formTitle;
    @FXML private Button _saveButton;

    private ConstantsService _constants;
    private BackgroundTasks _tasks;
    private String _customerNumber;
    // the record being edited, or null while the form adds a new one
    private T _editing;
    // the list load still running, if any; a newer one replaces it
    private BackgroundTasks.Handle _loading;
    // TEMPORARY: swap the Alert for Taran's shared confirm dialog once it exists
    private Predicate<String> _confirm = ContactListController::askToConfirm;

    // --- what each tab supplies ---

    protected abstract ContactText.Kind kind();

    // Runs once the FXML fields are filled in. A tab doesn't declare its own initialize(),
    // which would hide this class's; it sets up its columns and form here instead.
    protected abstract void setUpColumns();

    protected abstract void setUpForm();

    // fills the form from a saved record, or empties it (with any defaults) for a new one
    protected abstract void fillForm(T record, ConstantsService constants);

    protected abstract I readForm();

    // the service's field names mapped to the control to highlight and its error label
    protected abstract Map<String, Control> fields();

    protected abstract Map<String, Label> errorLabels();

    // these run in the background
    protected abstract ServiceResult<List<T>> list(String customerNumber);

    protected abstract ServiceResult<T> add(String customerNumber, I input);

    protected abstract ServiceResult<T> update(String customerNumber, T editing, I input);

    protected abstract ServiceResult<Void> delete(String customerNumber, T record);

    // how the confirm question names a record, e.g. "1 Queen St, Auckland"
    protected abstract String describe(T record);

    // --- the shared flow ---

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        // Edit and Remove act on the selected row
        _editButton.disableProperty().bind(_table.getSelectionModel().selectedItemProperty().isNull());
        _removeButton.disableProperty().bind(_table.getSelectionModel().selectedItemProperty().isNull());
        setUpColumns();
        setUpForm();
    }

    // Shows the customer's records and an empty form for adding one.
    protected void start(ConstantsService constants, BackgroundTasks tasks, String customerNumber) {
        _constants = constants;
        _tasks = tasks;
        _customerNumber = customerNumber;
        startAdding();
        load();
    }

    // for checks that can't answer an Alert; true means the MMM confirmed
    void setConfirmation(Predicate<String> confirm) {
        _confirm = confirm;
    }

    protected TableView<T> table() {
        return _table;
    }

    protected void showColumn(TableColumn<T, String> column, Function<T, String> text) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
    }

    private void load() {
        if (_loading != null) {
            _loading.cancel();
        }
        String customerNumber = _customerNumber;
        // AI-assisted: a customer made offline isn't on the mainframe yet, so there is nothing to read (US11)
        if (TemporaryNumbers.isTemporary(customerNumber)) {
            _table.getItems().clear();
            _emptyLabel.setText(ResultText.NOT_UPLOADED_YET);
            return;
        }
        _emptyLabel.setText(ContactText.loading(kind()));
        _loading = _tasks.run(
                () -> list(customerNumber),
                this::showList,
                failure -> showListFailure(ResultText.forError(failure)));
    }

    private void showList(ServiceResult<List<T>> result) {
        if (!result.isSuccessful()) {
            showListFailure(ResultText.forLoad(result, ContactText.whatCouldNotLoad(kind())));
            return;
        }
        _emptyLabel.setText(ContactText.noneYet(kind()));
        _table.getItems().setAll(result.getValue());
    }

    private void showListFailure(String text) {
        _emptyLabel.setText(ContactText.notLoaded(kind()));
        _table.getItems().clear();
        showMessage(text, true);
    }

    @FXML
    private void onEdit() {
        T selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        _editing = selected;
        clearErrors();
        hideMessage();
        showFormMode();
        fillForm(selected, _constants);
    }

    @FXML
    private void onRemove() {
        T selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null || !_confirm.test(ContactText.confirmRemove(kind(), describe(selected)))) {
            return;
        }
        String customerNumber = _customerNumber;
        hideMessage();
        // the button follows the selection until now; while removing, the loading state drives it
        _removeButton.disableProperty().unbind();
        LoadingView removing = new LoadingView(LoadingState.forSave("Remove", "Removing..."), _removeButton);
        _tasks.run(
                () -> delete(customerNumber, selected),
                removing::start,
                result -> {
                    removing.finish();
                    bindRemove();
                    // AI-assisted: a removal kept on the upload queue is saved too, not a problem (US11)
                    showMessage(ResultText.forChange(result, ContactText.removed(kind())), !result.isSaved());
                    if (result.isSaved()) {
                        // the form may still hold the record that was just removed
                        if (_editing == selected) {
                            startAdding();
                        }
                        reloadIfUploaded(result);
                    }
                },
                failure -> {
                    removing.finish();
                    bindRemove();
                    showMessage(ResultText.forError(failure), true);
                });
    }

    // The save runs in the background; the button reads "Saving..." and cannot be pressed
    // again until the mainframe has answered.
    @FXML
    private void onSave() {
        I input = readForm();
        // read on the UI thread: the record and customer must be the ones shown when Save was clicked
        T editing = _editing;
        String customerNumber = _customerNumber;
        clearErrors();
        hideMessage();
        LoadingView saving = new LoadingView(LoadingState.forSave(_saveButton.getText(), "Saving..."), _saveButton);
        _tasks.run(
                () -> editing == null ? add(customerNumber, input) : update(customerNumber, editing, input),
                saving::start,
                result -> {
                    saving.finish();
                    showSaveResult(result, ContactText.saved(kind(), editing != null));
                },
                failure -> {
                    saving.finish();
                    showMessage(ResultText.forError(failure), true);
                });
    }

    // Clear empties the form and goes back to adding a new record
    @FXML
    private void onCancel() {
        hideMessage();
        startAdding();
    }

    private void showSaveResult(ServiceResult<T> result, String doneText) {
        // highlight each bad field; everything the MMM typed stays in the form
        for (FieldError error : result.getValidationErrors().getErrors()) {
            showFieldError(error);
        }
        // AI-assisted: a save kept on the upload queue is saved too, not a problem (US11)
        showMessage(ResultText.forChange(result, doneText), !result.isSaved());
        if (result.isSaved()) {
            startAdding();
            reloadIfUploaded(result);
        }
    }

    // AI-assisted: after a change that reached the mainframe the list is read again, so it shows
    // new ids. A change still waiting to upload means the mainframe can't be reached, so reading
    // the list would only fail and empty the table; it stays as it is until the next load.
    private void reloadIfUploaded(ServiceResult<?> result) {
        if (result.isSuccessful()) {
            load();
        }
    }

    private void startAdding() {
        _editing = null;
        clearErrors();
        showFormMode();
        fillForm(null, _constants);
    }

    private void showFormMode() {
        boolean editing = _editing != null;
        _formTitle.setText(ContactText.formTitle(kind(), editing));
        _saveButton.setText(ContactText.saveButton(kind(), editing));
    }

    private void bindRemove() {
        _removeButton.setText("Remove");
        _removeButton.disableProperty().bind(_table.getSelectionModel().selectedItemProperty().isNull());
    }

    private void showFieldError(FieldError error) {
        Control control = fields().get(error.getFieldName());
        // a customerNumber error has no field on this tab; the message above covers it
        if (control == null) {
            return;
        }
        control.getStyleClass().add(FIELD_ERROR);
        errorLabels().get(error.getFieldName()).setText(ResultText.capitalise(error.getMessage()));
    }

    private void clearErrors() {
        fields().values().forEach(control -> control.getStyleClass().remove(FIELD_ERROR));
        errorLabels().values().forEach(label -> label.setText(""));
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

    private static boolean askToConfirm(String question) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, question, ButtonType.OK, ButtonType.CANCEL);
        alert.setHeaderText(null);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }
}
