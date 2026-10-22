package lavs.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
import lavs.logic.LoanBorrowerListResult;
import lavs.logic.LoanBorrowerResult;
import lavs.logic.LoanBorrowerService;
import lavs.model.LoanCoborrower;
import lavs.validation.FieldError;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

// AI-assisted: US8, the Borrowers tab of a loan (loan-borrowers.fxml), shown inside
// LoanScreenController. Lists everyone else on the loan and adds, re-types or removes them.
// The rules live in LoanBorrowerService and the wording in BorrowersText; this class only
// moves values between the screen and the service. Every call runs through BackgroundTasks,
// and after each change the list is read again, so it always shows what the mainframe has.
public class LoanBorrowersController {
    private static final String PROBLEM = "problem";
    private static final String FIELD_ERROR = "field-error";

    @FXML private Button _refreshButton;
    @FXML private Label _messageLabel;
    @FXML private TableView<LoanCoborrower> _table;
    @FXML private TableColumn<LoanCoborrower, String> _nameColumn;
    @FXML private TableColumn<LoanCoborrower, String> _numberColumn;
    @FXML private TableColumn<LoanCoborrower, String> _typeColumn;
    @FXML private Label _emptyLabel;
    @FXML private Button _changeTypeButton;
    @FXML private Button _removeButton;

    @FXML private TextField _customerNumber;
    @FXML private ComboBox<String> _type;
    @FXML private Label _customerNumberError;
    @FXML private Label _typeError;
    @FXML private Button _addButton;

    // LoanBorrowerService's field names mapped to the control to highlight and the label
    // that shows the message
    private Map<String, Control> _fields;
    private Map<String, Label> _errorLabels;

    private LoanBorrowerService _service;
    private BackgroundTasks _tasks;
    private LoadingView _refreshing;
    // the list load still running, if any; a newer one replaces it
    private BackgroundTasks.Handle _running;
    // null until load is called, i.e. until the loan has been saved
    private String _loanNumber;
    // true while an add, change or remove is running, so no second one can start
    private boolean _changing;
    private Predicate<String> _confirm = this::askToConfirm;

    // called by FXMLLoader once the @FXML fields are filled in
    @FXML
    private void initialize() {
        showColumn(_nameColumn, BorrowersText::name);
        showColumn(_numberColumn, LoanCoborrower::getCoborrowerNumber);
        showColumn(_typeColumn, borrower -> BorrowersText.typeLabel(borrower.getType()));

        _type.getItems().setAll(LoanBorrowerService.COBORROWER, LoanBorrowerService.GUARANTOR);
        _type.setConverter(new StringConverter<>() {
            @Override
            public String toString(String type) {
                return BorrowersText.typeLabel(type);
            }

            @Override
            public String fromString(String text) {
                // the box is not editable, so text is never typed in
                return null;
            }
        });

        _fields = Map.of(LoanBorrowerService.CUSTOMER_NUMBER, _customerNumber, LoanBorrowerService.TYPE, _type);
        _errorLabels = Map.of(LoanBorrowerService.CUSTOMER_NUMBER, _customerNumberError,
                LoanBorrowerService.TYPE, _typeError);

        _refreshing = new LoadingView(LoadingState.forRead(_refreshButton.getText(), BorrowersText.LOADING), _refreshButton);
        _table.getSelectionModel().selectedItemProperty().addListener((property, oldValue, newValue) -> showButtons());
        showButtons();
    }

    public void show(LoanBorrowerService service, BackgroundTasks tasks) {
        _service = service;
        _tasks = tasks;
    }

    // AI-assisted: pick a verified search result instead of typing an unchecked number.
    private java.util.function.Consumer<java.util.function.Consumer<lavs.model.Customer>> selectCustomer;

    public void setSelectCustomer(java.util.function.Consumer<java.util.function.Consumer<lavs.model.Customer>> select) {
        selectCustomer = select;
        _customerNumber.setEditable(false);
    }

    @FXML private void onSelectCustomer() {
        if (!_changing && selectCustomer != null) selectCustomer.accept(customer -> _customerNumber.setText(customer.getNumber()));
    }

    // for checks that can't answer an Alert; true means the MMM confirmed
    void setConfirmation(Predicate<String> confirm) {
        _confirm = confirm;
    }

    // Reads everyone on the loan in the background and shows them. On failure the list is
    // cleared, so nothing out of date is left on screen.
    public void load(String loanNumber) {
        _loanNumber = loanNumber;
        if (_running != null) {
            _running.cancel();
        }
        _emptyLabel.setText(BorrowersText.LOADING);
        _running = _tasks.run(
                () -> {
                    try {
                        return _service.listCoborrowers(loanNumber);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                _refreshing::start,
                result -> {
                    _refreshing.finish();
                    showList(result);
                },
                failure -> {
                    _refreshing.finish();
                    showListFailure(BorrowersText.forError(failure));
                });
        showButtons();
    }

    @FXML
    private void onRefresh() {
        if (_loanNumber != null) {
            hideMessage();
            load(_loanNumber);
        }
    }

    @FXML
    private void onAdd() {
        // read on the UI thread: the values must be the ones shown when Add was clicked
        String loanNumber = _loanNumber;
        String customerNumber = _customerNumber.getText() == null ? "" : _customerNumber.getText().trim();
        String type = _type.getValue();
        clearFieldErrors();
        change(_addButton, "Adding...",
                () -> _service.addCoborrower(loanNumber, customerNumber, type),
                BorrowersText.added(customerNumber, type),
                () -> {
                    _customerNumber.clear();
                    _type.getSelectionModel().clearSelection();
                });
    }

    @FXML
    private void onChangeType() {
        LoanCoborrower selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        String newType = BorrowersText.otherType(selected.getType());
        change(_changeTypeButton, "Changing...",
                () -> _service.updateCoborrower(selected, selected.getCoborrowerNumber(), newType),
                BorrowersText.changed(selected, newType),
                () -> { });
    }

    @FXML
    private void onRemove() {
        LoanCoborrower selected = _table.getSelectionModel().getSelectedItem();
        if (selected == null || !_confirm.test(BorrowersText.confirmRemove(selected))) {
            return;
        }
        change(_removeButton, "Removing...",
                () -> _service.removeCoborrower(selected.getId(), selected.getLoanNumber()),
                BorrowersText.removed(selected),
                () -> { });
    }

    // A change to the loan's borrowers, run in the background. While it runs, the clicked
    // button shows busyText and no other change can start. Once the mainframe has answered,
    // the list is read again, because even a refused re-type may have changed what it holds.
    private void change(Button clicked, String busyText, ServiceCall call, String successText, Runnable onSuccess) {
        hideMessage();
        _tasks.run(
                () -> {
                    try {
                        return call.run();
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                },
                () -> {
                    _changing = true;
                    showButtons();
                    clicked.setText(busyText);
                },
                result -> {
                    _changing = false;
                    showButtons();
                    showChangeResult(result, successText, onSuccess);
                },
                failure -> {
                    _changing = false;
                    showButtons();
                    showMessage(BorrowersText.forError(failure), true);
                });
    }

    private void showChangeResult(LoanBorrowerResult result, String successText, Runnable onSuccess) {
        for (FieldError error : result.getValidationErrors().getErrors()) {
            showFieldError(error);
        }
        showMessage(BorrowersText.forChange(result, successText), !result.isSuccessful() && !lavs.logic.SaveStatus.isQueued(result.getMainframeStatus()));
        if (result.isSuccessful() || lavs.logic.SaveStatus.isQueued(result.getMainframeStatus())) {
            onSuccess.run();
        }
        // only the field checks stop a change before the mainframe is contacted
        if (result.getMainframeStatus() != null && !lavs.logic.SaveStatus.isQueued(result.getMainframeStatus())) {
            load(_loanNumber);
        }
    }

    private void showList(LoanBorrowerListResult result) {
        if (!result.isSuccessful()) {
            showListFailure(BorrowersText.forListFailure(result.getMainframeStatus()));
            return;
        }
        _emptyLabel.setText(BorrowersText.NOBODY_YET);
        _table.getItems().setAll(result.getCoborrowers());
    }

    private void showListFailure(String text) {
        _emptyLabel.setText(BorrowersText.NOT_LOADED);
        _table.getItems().clear();
        showMessage(text, true);
    }

    // Copies the current state onto the buttons; their idle text too, so a busy text is undone.
    private void showButtons() {
        LoanCoborrower selected = _table.getSelectionModel().getSelectedItem();
        boolean nothingSelected = selected == null;
        _changeTypeButton.setText(BorrowersText.changeTypeButton(selected));
        _removeButton.setText("Remove");
        _addButton.setText("Add to loan");
        _changeTypeButton.setDisable(_changing || nothingSelected);
        _removeButton.setDisable(_changing || nothingSelected);
        _addButton.setDisable(_changing || _loanNumber == null);
    }

    private void showFieldError(FieldError error) {
        Control control = _fields.get(error.getFieldName());
        // a loanNumber error has no field on this tab; the message above still reports it
        if (control == null) {
            return;
        }
        control.getStyleClass().add(FIELD_ERROR);
        String message = error.getMessage();
        _errorLabels.get(error.getFieldName()).setText(message.substring(0, 1).toUpperCase() + message.substring(1));
    }

    private void clearFieldErrors() {
        _fields.values().forEach(control -> control.getStyleClass().remove(FIELD_ERROR));
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

    private static void showColumn(TableColumn<LoanCoborrower, String> column, Function<LoanCoborrower, String> text) {
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(text.apply(cell.getValue())));
    }

    private boolean askToConfirm(String question) {
        return ConfirmDialog.confirm(_table.getScene().getWindow(),
                new ConfirmContent("Remove record?", question, "Remove"));
    }

    // a LoanBorrowerService call that may fail to reach the mainframe
    private interface ServiceCall {
        LoanBorrowerResult run() throws IOException;
    }
}
