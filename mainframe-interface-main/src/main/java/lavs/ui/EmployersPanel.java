package lavs.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.EmployerResult;
import lavs.logic.EmployerService;
import lavs.model.Employer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

// AI-assisted: US5 lists, edits and removes employers using the shared form/error controls.
// Laid out like the Addresses tab (customer-addresses.fxml): a heading with Edit and Remove over
// the table on the left, the add/edit form in a card on the right, and a message that only shows
// when there's something to say.
public final class EmployersPanel extends VBox {
    private static final String PROBLEM = "problem";

    private final EmployerService service;
    private final BackgroundTasks tasks;
    private final String customerNumber;
    private final TableView<Employer> table = new TableView<>();
    private final TextField name = new TextField();
    private final TextField position = new TextField();
    private final TextField salary = new TextField();
    private final ComboBox<String> type = new ComboBox<>();
    private final Label message = new Label();
    private final Label formTitle = new Label();
    private final Button save = new Button();
    private final VBox form = new VBox(12);
    private final HBox actions = new HBox(8);
    private final FieldErrorPresenter errors;
    private Employer editing;

    public EmployersPanel(EmployerService service, BackgroundTasks tasks, String customerNumber, lavs.logic.CategoryValues constants) {
        super(16);
        this.service = service;
        this.tasks = tasks;
        this.customerNumber = customerNumber;
        // the same look as the other customer record tabs
        getStylesheets().add(EmployersPanel.class.getResource("loan.css").toExternalForm());
        getStyleClass().add("loan");
        setPadding(new Insets(20, 0, 0, 0));
        TableColumns.add(table, "Employer", Employer::getName);
        TableColumns.add(table, "Type", Employer::getType);
        TableColumns.add(table, "Position", Employer::getPosition);
        TableColumns.add(table, "Salary", Employer::getSalary);
        table.getStyleClass().add("schedule");
        table.setPlaceholder(new Label("No employers yet."));
        VBox.setVgrow(table, Priority.ALWAYS);
        type.getItems().setAll(constants.getValues(lavs.logic.ConstantCategories.CUSTOMER_EMPLOYER_TYPE));
        type.setMaxWidth(Double.MAX_VALUE);
        var nameField = new FormField("Employer name *", name);
        var typeField = new FormField("Type *", type);
        var salaryField = new FormField("Annual salary", salary);
        errors = new FieldErrorPresenter(Map.of("name", nameField, "type", typeField, "salary", salaryField));
        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        message.getStyleClass().add("message");
        hideMessage();
        HBox body = new HBox(24, listColumn(), formCard(nameField, typeField, salaryField));
        body.setAlignment(Pos.TOP_LEFT);
        VBox.setVgrow(body, Priority.ALWAYS);
        getChildren().addAll(message, body);
        fill(null);
        load(null);
    }

    // the heading, with Edit, Remove and Refresh beside it, over the table
    private VBox listColumn() {
        Label title = new Label("Employers");
        title.getStyleClass().add("section");
        Label note = new Label("A customer with no employer needs a guarantor before a loan can be submitted.");
        note.getStyleClass().add("note");
        note.setWrapText(true);
        Button edit = new Button("Edit");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        edit.setOnAction(event -> fill(table.getSelectionModel().getSelectedItem()));
        Button remove = new Button("Remove");
        remove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        remove.setOnAction(event -> confirmRemove());
        Button refresh = new Button("Refresh");
        refresh.setOnAction(event -> load(null));
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        actions.getChildren().addAll(edit, remove, refresh);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox heading = new HBox(8, new VBox(2, title, note), grow, actions);
        heading.setAlignment(Pos.CENTER_LEFT);
        VBox column = new VBox(12, heading, table);
        HBox.setHgrow(column, Priority.ALWAYS);
        return column;
    }

    // the add/edit form, in a card on the right like the Addresses tab
    private VBox formCard(FormField nameField, FormField typeField, FormField salaryField) {
        formTitle.getStyleClass().add("section");
        save.getStyleClass().add("lavs-button-primary");
        save.setDefaultButton(true);
        save.setOnAction(event -> save());
        Button clear = new Button("Clear");
        clear.setOnAction(event -> { hideMessage(); fill(null); });
        HBox buttons = new HBox(8, clear, save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        form.getChildren().addAll(formTitle, nameField, typeField, new FormField("Position", position), salaryField, buttons);
        form.getStyleClass().add("card");
        form.setPrefWidth(340);
        form.setMinWidth(280);
        form.setMaxWidth(380);
        return form;
    }

    private void confirmRemove() {
        Employer selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        ConfirmDialog.ask(getScene().getWindow(), new ConfirmContent("Remove employer?",
                "Remove " + selected.getName() + "?", "Remove"), () -> change(() ->
                service.deleteEmployer(selected.getId(), customerNumber), "Employer removed."));
    }

    private void fill(Employer employer) {
        editing = employer;
        name.setText(employer == null ? "" : employer.getName());
        position.setText(employer == null ? "" : employer.getPosition());
        salary.setText(employer == null ? "" : (employer.getSalary() == null ? "" : employer.getSalary().toPlainString()));
        type.setValue(employer == null ? null : employer.getType());
        formTitle.setText(employer == null ? "Add an employer" : "Edit employer");
        save.setText(employer == null ? "Add employer" : "Save changes");
        errors.clear();
    }

    // done is what to say once the list is back, e.g. "Employer saved."; null says nothing
    private void load(String done) {
        busy(true);
        showMessage("Loading employers...", false);
        tasks.run(() -> service.listEmployers(customerNumber), result -> {
            busy(false);
            if (result.isSuccessful()) {
                table.getItems().setAll(result.getValue());
                if (done == null) hideMessage(); else showMessage(done, false);
            }
            else { table.getItems().clear(); showMessage(ResultText.forLoad(result, "Employers"), true); }
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
        showMessage("Saving...", false);
        tasks.run(() -> {
            try { return action.run(); } catch (IOException error) { throw new UncheckedIOException(error); }
        }, result -> {
            busy(false);
            errors.present(result.getValidationErrors());
            if (result.isSuccessful()) { fill(null); load(done); }
            else if (result.getReply().isQueued()) { fill(null); showMessage(result.getReply().getQueuedMessage(), false); }
            else if (!result.getValidationErrors().isValid()) showMessage(ResultText.FIX_FIELDS, true);
            else showMessage(result.getReply().getErrorMessage(), true);
        }, this::failed);
    }

    private void showMessage(String text, boolean isProblem) {
        message.setText(text);
        message.getStyleClass().remove(PROBLEM);
        if (isProblem) message.getStyleClass().add(PROBLEM);
        message.setManaged(true);
        message.setVisible(true);
    }

    private void hideMessage() {
        message.setManaged(false);
        message.setVisible(false);
    }

    private void busy(boolean value) { form.setDisable(value); actions.setDisable(value); table.setDisable(value); }
    private void failed(Throwable error) { busy(false); showMessage(ResultText.forError(error), true); }
    @FunctionalInterface private interface Change { EmployerResult run() throws IOException; }
}
