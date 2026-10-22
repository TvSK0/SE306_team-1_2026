// AI-assisted: the Documents section of the customer record (Figma 11): the table and an inline add or edit form.
package lavs.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import lavs.logic.ConstantsService;
import lavs.logic.CustomerDocumentService;
import lavs.logic.DocumentInput;
import lavs.logic.DocumentRow;

import java.util.Map;
import java.util.Optional;

// Thin control: the wording and decisions are in DocumentsText and DocumentFormState (unit
// tested) and the work is in CustomerDocumentService. Every call goes through BackgroundTasks so
// the window never freezes. Reads get skeleton rows; a save has no Cancel (the form's own Cancel
// closes the form and is not shown as a way to stop a save). Delete always asks first, through
// the table's ConfirmDialog. The list loads when the section opens and when the record is
// returned to.
public class CustomerDocumentsSection extends VBox implements Refreshable {
    private final SectionContext _context;
    private final CustomerDocumentService _service;
    private final ConstantsService _constants;
    private final BackgroundTasks _tasks;

    private final DocumentFormState _form = new DocumentFormState();
    private final Banner _banner = new Banner();
    private final SkeletonRows _skeleton = new SkeletonRows(3);
    private final RecordTable<DocumentRow> _table = new RecordTable<>();

    private final Label _formTitle = new Label();
    private final ConstantDropdown _type = new ConstantDropdown();
    private final TextField _fileName = new TextField();
    private final ConstantDropdown _fileType = new ConstantDropdown();
    private final TextField _size = new TextField();
    private final FormField _typeField = new FormField("Document type *", _type);
    private final FormField _fileNameField = new FormField("File name *", _fileName);
    private final FormField _fileTypeField = new FormField("File type", _fileType);
    private final FormField _sizeField = new FormField("Size", _size);
    private final FieldErrorPresenter _presenter = new FieldErrorPresenter(Map.of(
            DocumentInput.TYPE, _typeField,
            DocumentInput.FILE_NAME, _fileNameField,
            DocumentInput.FILE_TYPE, _fileTypeField,
            DocumentInput.SIZE, _sizeField));
    private final Button _cancel = new Button("Cancel");
    private final Button _save = new Button(DocumentsText.SAVE);
    private final LoadingView _saving =
            new LoadingView(LoadingState.forSave(DocumentsText.SAVE, DocumentsText.SAVING), _save);
    private final VBox _card = new VBox(16);

    public CustomerDocumentsSection(SectionContext context, CustomerDocumentService service,
                                    ConstantsService constants, BackgroundTasks tasks) {
        super(12);
        _context = context;
        _service = service;
        _constants = constants;
        _tasks = tasks;

        SectionHeading heading = SectionHeading.section(DocumentsText.HEADING, DocumentsText.SUBTITLE);
        heading.setAction(DocumentsText.ADD_BUTTON, this::startAdd);
        _table.column("Document type", DocumentRow::type, 3)
                .column("File name", DocumentRow::fileName, 3)
                .column("File type", row -> Optional.ofNullable(row.fileType()).orElse(""), 2)
                .column("Size", row -> Long.toString(row.size()), 1)
                .rowAction("Edit", this::startEdit)
                .deleteAction("document", this::delete);
        _table.setEmptyText(DocumentsText.EMPTY);
        VBox.setVgrow(_table, Priority.ALWAYS);

        buildForm();
        getChildren().addAll(heading, _banner, _skeleton, _table, _card);
        showSkeleton(false);
        showForm();
        load();
    }

    @Override
    public void onReturn() {
        load();
    }

    // a card under the table: title, the four fields in one row, then Cancel and Save on the right
    private void buildForm() {
        _type.setCategory(DocumentCategories.DOCUMENT_TYPE);
        _fileType.setCategory(DocumentCategories.FILE_TYPE);
        // the mainframe's category names are not confirmed, so both can be typed into
        _type.setEditable(true);
        _fileType.setEditable(true);
        _formTitle.getStyleClass().add("lavs-section-title");
        for (FormField field : new FormField[]{_typeField, _fileNameField, _fileTypeField, _sizeField}) {
            HBox.setHgrow(field, Priority.ALWAYS);
        }
        HBox fields = new HBox(12, _typeField, _fileNameField, _fileTypeField, _sizeField);
        _cancel.getStyleClass().add("lavs-button-secondary");
        _save.getStyleClass().add("lavs-button-primary");
        _cancel.setOnAction(event -> cancelForm());
        _save.setOnAction(event -> save());
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox buttons = new HBox(8, grow, _cancel, _save);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        _card.getStyleClass().add("lavs-card");
        _card.getChildren().addAll(_formTitle, fields, buttons);
    }

    private void load() {
        _tasks.run(() -> _service.list(_context.customerNumber()), () -> {
            _banner.hide();
            showSkeleton(true);
        }, result -> {
            showSkeleton(false);
            DocumentsText.loadFailure(result).ifPresent(_banner::show);
            // a failed read leaves the rows that were showing; a successful one replaces them
            Optional.ofNullable(result.getValue()).ifPresent(_table::setRows);
        }, failure -> {
            showSkeleton(false);
            _banner.show(DocumentsText.unexpected("Couldn't load the documents", failure));
        });
    }

    private void startAdd() {
        _form.startAdd();
        fill(Optional.empty());
    }

    private void startEdit(DocumentRow row) {
        _form.startEdit(row);
        fill(Optional.of(row));
    }

    private void cancelForm() {
        _form.close();
        _presenter.clear();
        _banner.hide();
        showForm();
    }

    private void fill(Optional<DocumentRow> row) {
        _presenter.clear();
        _banner.hide();
        _type.show(_constants, row.map(DocumentRow::type).orElse(null));
        _fileType.show(_constants, row.map(DocumentRow::fileType).orElse(null));
        // show() disables a dropdown with no values; these can still be typed into
        _type.setDisable(false);
        _fileType.setDisable(false);
        _fileName.setText(row.map(DocumentRow::fileName).orElse(""));
        _size.setText(row.map(found -> Long.toString(found.size())).orElse(""));
        showForm();
    }

    private void showForm() {
        _formTitle.setText(_form.title());
        _card.setVisible(_form.isOpen());
        _card.setManaged(_form.isOpen());
    }

    private void save() {
        _presenter.clear();
        _banner.hide();
        DocumentInput input = new DocumentInput(
                DocumentsText.chosen(_type.getEditor().getText(), _type.getValue()), _fileName.getText(),
                DocumentsText.chosen(_fileType.getEditor().getText(), _fileType.getValue()), _size.getText());
        String number = _context.customerNumber();
        _tasks.run(() -> _form.editing()
                        .map(row -> _service.update(number, row.id(), input))
                        .orElseGet(() -> _service.add(number, input)),
                _saving::start, result -> {
                    _saving.finish();
                    DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(result);
                    FieldErrorPresenter.Result shown = _presenter.present(display.fieldErrors());
                    Optional.of(shown).filter(found -> display.validationBanner() && found.hasErrors())
                            .map(BannerContent::validation).or(display::banner).ifPresent(_banner::show);
                    Optional.of(display).filter(DocumentsText.SaveDisplay::saved).ifPresent(done -> {
                        _form.close();
                        showForm();
                        reloadAndRecount();
                    });
                }, failure -> {
                    _saving.finish();
                    _banner.show(DocumentsText.unexpected("Couldn't save the document", failure));
                });
    }

    // called once the user has confirmed the delete in the table's ConfirmDialog
    private void delete(DocumentRow row) {
        _banner.hide();
        _tasks.run(() -> _service.delete(_context.customerNumber(), row.id()), result -> {
            DocumentsText.deleteFailure(result).ifPresent(_banner::show);
            Optional.ofNullable(result.getValue()).ifPresent(id -> reloadAndRecount());
        }, failure -> _banner.show(DocumentsText.unexpected("Couldn't delete the document", failure)));
    }

    private void reloadAndRecount() {
        load();
        _context.refreshCounts().run();
    }

    // reads get skeleton rows (no Cancel: the list loads by itself when the section opens)
    private void showSkeleton(boolean loading) {
        _skeleton.setVisible(loading);
        _skeleton.setManaged(loading);
    }
}
