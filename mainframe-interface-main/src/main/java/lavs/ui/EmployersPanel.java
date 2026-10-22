package lavs.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.EmployerResult;
import lavs.logic.EmployerService;
import lavs.model.Employer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

// AI-assisted: US5 lists, edits and removes employers using the shared form/error controls.
public final class EmployersPanel extends VBox {
    private final EmployerService service;
    private final BackgroundTasks tasks;
    private final String customerNumber;
    private final TableView<Employer> table = new TableView<>();
    private final TextField name = new TextField();
    private final TextField position = new TextField();
    private final TextField salary = new TextField();
    private final ComboBox<String> type = new ComboBox<>();
    private final Label message = new Label();
    private final VBox form = new VBox(12);
    private final HBox actions = new HBox(12);
    private final FieldErrorPresenter errors;
    private Employer editing;

    public EmployersPanel(EmployerService service, BackgroundTasks tasks, String customerNumber, lavs.logic.CategoryValues constants) {
        super(12);
        this.service = service;
        this.tasks = tasks;
        this.customerNumber = customerNumber;
        TableColumns.add(table, "Employer", Employer::getName);
        TableColumns.add(table, "Type", Employer::getType);
        TableColumns.add(table, "Position", Employer::getPosition);
        TableColumns.add(table, "Salary", Employer::getSalary);
        table.setPlaceholder(new Label("No employers to display"));
        HBox.setHgrow(table, Priority.ALWAYS);
        type.getItems().setAll(constants.getValues(lavs.logic.ConstantCategories.CUSTOMER_EMPLOYER_TYPE));
        var nameField = new FormField("Employer name *", name);
        var typeField = new FormField("Type *", type);
        var salaryField = new FormField("Annual salary", salary);
        errors = new FieldErrorPresenter(Map.of("name", nameField, "type", typeField, "salary", salaryField));
        Button save = new Button("Save employer");
        save.getStyleClass().add("lavs-button-primary");
        save.setOnAction(event -> save());
        Button clear = new Button("New / clear");
        clear.setOnAction(event -> fill(null));
        form.getChildren().addAll(nameField, typeField, new FormField("Position", position), salaryField,
                new HBox(12, save, clear));
        Button edit = new Button("Edit");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        edit.setOnAction(event -> fill(table.getSelectionModel().getSelectedItem()));
        Button remove = new Button("Remove");
        remove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        remove.setOnAction(event -> {
            Employer selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            ConfirmDialog.ask(getScene().getWindow(), new ConfirmContent("Remove employer?",
                    "Remove " + selected.getName() + "?", "Remove"), () -> change(() ->
                    service.deleteEmployer(selected.getId(), customerNumber), "Employer removed."));
        });
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> load());
        actions.getChildren().addAll(edit, remove, refresh);
        HBox body = new HBox(24, table, form);
        VBox.setVgrow(body, Priority.ALWAYS);
        message.setWrapText(true);
        getChildren().addAll(message, body, actions);
        fill(null);
        load();
    }

    private void fill(Employer employer) {
        editing = employer;
        name.setText(employer == null ? "" : employer.getName());
        position.setText(employer == null ? "" : employer.getPosition());
        salary.setText(employer == null ? "" : (employer.getSalary() == null ? "" : employer.getSalary().toPlainString()));
        type.setValue(employer == null ? null : employer.getType());
        errors.clear();
    }

    private void load() {
        busy(true);
        message.setText("Loading employers...");
        tasks.run(() -> service.listEmployers(customerNumber), result -> {
            busy(false);
            if (result.isSuccessful()) { table.getItems().setAll(result.getValue()); message.setText(""); }
            else { table.getItems().clear(); message.setText(ResultText.forLoad(result, "Employers")); }
        }, this::failed);
    }

    private void save() {
        Employer selected = editing;
        String employerName = name.getText(), job = position.getText(), pay = salary.getText(), kind = type.getValue();
        change(() -> selected == null ? service.addEmployer(customerNumber, employerName, job, pay, kind)
                : service.updateEmployer(selected.getId(), customerNumber, employerName, job, pay, kind), "Employer saved.");
    }

    private void change(Change action, String done) {
        errors.clear();
        busy(true);
        message.setText("Saving...");
        tasks.run(() -> {
            try { return action.run(); } catch (IOException error) { throw new UncheckedIOException(error); }
        }, result -> {
            busy(false);
            errors.present(result.getValidationErrors());
            if (result.isSuccessful()) { fill(null); load(); }
            else if (result.getReply().isQueued()) { fill(null); message.setText(result.getReply().getQueuedMessage()); }
            else if (!result.getValidationErrors().isValid()) message.setText(ResultText.FIX_FIELDS);
            else message.setText(result.getReply().getErrorMessage());
        }, this::failed);
    }

    private void busy(boolean value) { form.setDisable(value); actions.setDisable(value); table.setDisable(value); }
    private void failed(Throwable error) { busy(false); message.setText(ResultText.forError(error)); }
    @FunctionalInterface private interface Change { EmployerResult run() throws IOException; }
}
