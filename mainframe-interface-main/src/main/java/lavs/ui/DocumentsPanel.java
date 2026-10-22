package lavs.ui;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import lavs.logic.*;
import lavs.model.RecordDocument;
import java.util.Map;
import java.util.function.Supplier;

// AI-assisted: a shared editor for customer and loan documents.
public final class DocumentsPanel extends VBox {
    private final RecordDocumentsService service;
    private final RecordOwner owner;
    private final String number;
    private final BackgroundTasks tasks;
    private final TableView<RecordDocument> table = new TableView<>();
    private final ComboBox<String> type = new ComboBox<>();
    private final TextField fileName = new TextField();
    private final TextField fileType = new TextField();
    private final TextField size = new TextField();
    private final Label message = new Label();
    private final VBox form = new VBox(10);
    private final HBox actions = new HBox(10);
    private final FieldErrorPresenter errors;
    private RecordDocument editing;

    public DocumentsPanel(RecordDocumentsService service, RecordOwner owner, String number,
                      CategoryValues constants, BackgroundTasks tasks) {
        super(12);
        this.service = service; this.owner = owner; this.number = number; this.tasks = tasks;
        type.getItems().setAll(constants.getValues(owner.category("Document")));
        type.setEditable(type.getItems().isEmpty());
        type.setMaxWidth(Double.MAX_VALUE);
        var typeField = new FormField("Document type *", type);
        var nameField = new FormField("File name *", fileName);
        var formatField = new FormField("File type *", fileType);
        var sizeField = new FormField("Size in bytes *", size);
        errors = new FieldErrorPresenter(Map.of("type", typeField, "fileName", nameField,
                "fileType", formatField, "size", sizeField));
        TableColumns.add(table, "Type", RecordDocument::type);
        TableColumns.add(table, "File name", RecordDocument::fileName);
        TableColumns.add(table, "File type", RecordDocument::fileType);
        TableColumns.add(table, "Bytes", RecordDocument::size);
        table.setPlaceholder(new Label("No documents to display"));
        Button edit = new Button("Edit selected");
        Button remove = new Button("Delete selected");
        edit.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        remove.disableProperty().bind(table.getSelectionModel().selectedItemProperty().isNull());
        edit.setOnAction(event -> fill(table.getSelectionModel().getSelectedItem()));
        remove.setOnAction(event -> {
            RecordDocument note = table.getSelectionModel().getSelectedItem();
            ConfirmDialog.ask(getScene().getWindow(), new ConfirmContent("Delete document metadata?", "Delete the selected document metadata?", "Delete"),
                    () -> change(() -> service.delete(owner, number, note.id())));
        });
        Button refresh = new Button("Refresh"); refresh.setOnAction(event -> load());
        actions.getChildren().addAll(edit, remove, refresh);
        Button save = new Button("Save document");
        save.getStyleClass().add("lavs-button-primary");
        save.setOnAction(event -> {
            Long id = editing == null ? null : editing.id();
            String kind = type.isEditable() ? type.getEditor().getText() : type.getValue();
            String name = fileName.getText(), format = fileType.getText(), bytes = size.getText();
            change(() -> service.save(owner, number, id, kind, name, format, bytes));
        });
        Button clear = new Button("New / clear"); clear.setOnAction(event -> fill(null));
        form.getChildren().addAll(new Label("Metadata only: no file contents are uploaded."), typeField, nameField, formatField, sizeField, new HBox(10, save, clear));
        message.setWrapText(true);
        getChildren().addAll(message, table, actions, form);
        VBox.setVgrow(table, Priority.ALWAYS);
        load();
    }
    private void fill(RecordDocument note) {
        editing = note; type.setValue(note == null ? null : note.type());
        fileName.setText(note == null ? "" : note.fileName());
        fileType.setText(note == null ? "" : note.fileType());
        size.setText(note == null ? "" : Long.toString(note.size())); errors.clear();
    }
    private void load() {
        busy(true); message.setText("Loading documents...");
        tasks.run(() -> service.list(owner, number), result -> {
            busy(false);
            if (result.isSuccessful()) { table.getItems().setAll(result.getValue()); message.setText(""); }
            else message.setText(ResultText.forLoad(result, "Documents"));
        }, this::failed);
    }
    private void change(Supplier<ServiceResult<Void>> action) {
        errors.clear(); busy(true); message.setText("Saving...");
        tasks.run(action::get, result -> {
            busy(false); errors.present(result.getValidationErrors());
            if (result.isSuccessful()) { fill(null); load(); }
            else {
                if (result.isQueued()) fill(null);
                message.setText(ResultText.forChange(result, "Document saved."));
            }
        }, this::failed);
    }
    private void busy(boolean busy) { table.setDisable(busy); form.setDisable(busy); actions.setDisable(busy); }
    private void failed(Throwable error) { busy(false); message.setText(ResultText.forError(error)); }
}
