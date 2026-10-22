package lavs.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.*;
import lavs.model.RecordNote;
import java.util.Map;
import java.util.function.Supplier;

// AI-assisted: a shared editor for customer and loan notes.
public final class NotesPanel extends VBox {
    private final RecordNotesService service;
    private final RecordOwner owner;
    private final String number;
    private final BackgroundTasks tasks;
    private final TableView<RecordNote> table = new TableView<>();
    private final ComboBox<String> type = new ComboBox<>();
    private final TextArea lines = new TextArea();
    private final Label message = new Label();
    private final VBox form = new VBox(10);
    private final HBox actions = new HBox(10);
    private final FieldErrorPresenter errors;
    private RecordNote editing;

    public NotesPanel(RecordNotesService service, RecordOwner owner, String number,
                      CategoryValues constants, BackgroundTasks tasks) {
        super(12);
        this.service = service; this.owner = owner; this.number = number; this.tasks = tasks;
        type.getItems().setAll(constants.getValues(owner.category("Note")));
        type.setEditable(type.getItems().isEmpty());
        type.setMaxWidth(Double.MAX_VALUE);
        lines.setWrapText(true);
        lines.setPrefRowCount(6);
        var typeField = new FormField("Note type *", type);
        var linesField = new FormField("Note *", lines);
        errors = new FieldErrorPresenter(Map.of("type", typeField, "lines", linesField));
        TableColumns.add(table, "Type", RecordNote::type);
        TableColumns.add(table, "Author", RecordNote::author);
        TableColumns.add(table, "Added", RecordNote::dateAdded);
        TableColumns.add(table, "Note", RecordNote::lines);
        table.setPlaceholder(new Label("No notes to display"));
        Button edit = new Button("Edit selected");
        Button remove = new Button("Delete selected");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        remove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        edit.setOnAction(event -> fill(table.getSelectionModel().getSelectedItem()));
        remove.setOnAction(event -> {
            RecordNote note = table.getSelectionModel().getSelectedItem();
            ConfirmDialog.ask(getScene().getWindow(), new ConfirmContent("Delete note?", "Delete the selected note?", "Delete"),
                    () -> change(() -> service.delete(owner, number, note.id())));
        });
        Button refresh = new Button("Refresh"); refresh.setOnAction(event -> load());
        actions.getChildren().addAll(edit, remove, refresh);
        Button save = new Button("Save note");
        save.getStyleClass().add("lavs-button-primary");
        save.setOnAction(event -> {
            Long id = editing == null ? null : editing.id();
            String kind = type.isEditable() ? type.getEditor().getText() : type.getValue();
            String text = lines.getText();
            change(() -> service.save(owner, number, id, kind, text));
        });
        Button clear = new Button("New / clear"); clear.setOnAction(event -> fill(null));
        form.getChildren().addAll(typeField, linesField, new HBox(10, save, clear));
        message.setWrapText(true);
        getChildren().addAll(message, table, actions, form);
        VBox.setVgrow(table, Priority.ALWAYS);
        load();
    }
    private void fill(RecordNote note) {
        editing = note; type.setValue(note == null ? null : note.type());
        lines.setText(note == null ? "" : note.lines()); errors.clear();
    }
    private void load() {
        busy(true); message.setText("Loading notes...");
        tasks.run(() -> service.list(owner, number), result -> {
            busy(false);
            if (result.isSuccessful()) { table.getItems().setAll(result.getValue()); message.setText(""); }
            else message.setText(ResultText.forLoad(result, "Notes"));
        }, this::failed);
    }
    private void change(Supplier<ServiceResult<Void>> action) {
        errors.clear(); busy(true); message.setText("Saving...");
        tasks.run(action::get, result -> {
            busy(false); errors.present(result.getValidationErrors());
            if (result.isSuccessful()) { fill(null); load(); }
            else {
                if (result.isQueued()) fill(null);
                message.setText(ResultText.forChange(result, "Note saved."));
            }
        }, this::failed);
    }
    private void busy(boolean busy) { table.setDisable(busy); form.setDisable(busy); actions.setDisable(busy); }
    private void failed(Throwable error) { busy(false); message.setText(ResultText.forError(error)); }
}
