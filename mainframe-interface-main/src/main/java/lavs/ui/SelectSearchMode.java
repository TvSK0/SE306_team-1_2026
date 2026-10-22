// AI-assisted: the customer search in select mode (Figma 10): pick a customer for the caller.
package lavs.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import lavs.logic.CustomerContactService;
import lavs.model.Customer;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

// Thin control: the wording comes from the caller's CustomerSelectRequest and the answers in
// the Contact details column from ContactDetailsColumn (unit tested). The search results show
// straight away; a second background pass then fills the column row by row, "Checking…"
// until each answer arrives. It runs after the first page and after every "Show more".
public class SelectSearchMode implements CustomerSearchMode {
    private final CustomerSelectRequest _request;
    private final CustomerContactService _contacts;
    private final BackgroundTasks _tasks;
    private final Consumer<Runnable> _uiThread;
    private final Runnable _back;
    private final ContactDetailsColumn _column = new ContactDetailsColumn();

    // back is navigator::back: both Select and Cancel return to the caller
    public SelectSearchMode(CustomerSelectRequest request, CustomerContactService contacts, BackgroundTasks tasks,
                            Consumer<Runnable> uiThread, Runnable back) {
        _request = request;
        _contacts = contacts;
        _tasks = tasks;
        _uiThread = uiThread;
        _back = back;
    }

    @Override
    public String title() {
        return _request.title();
    }

    @Override
    public String subtitle() {
        return _request.subtitle();
    }

    // a secondary button, and only when the caller can add a customer
    @Override
    public void configureHeading(SectionHeading heading) {
        _request.addNewHandler().ifPresent(addNew -> heading.setSecondaryAction("+ Add New Customer", addNew));
    }

    @Override
    public Optional<Node> extraControls() {
        return _request.extraControlsNode();
    }

    @Override
    public void configureTable(RecordTable<Customer> table) {
        table.column("Customer no.", Customer::getNumber, 2)
                .column("Name", Customer::getFullName, 3)
                .column("Date of birth", customer -> CustomerSearchText.dateOfBirth(customer.getDateOfBirth()), 2)
                .nodeColumn("Contact details", customer -> {
                    ContactDetailsColumn.ContactTag tag = _column.tag(customer.getNumber());
                    return new Tag(tag.variant(), tag.text());
                }, 3)
                .rowAction("Select", this::select);
    }

    // the Cancel button, right-aligned under the table
    @Override
    public Optional<Node> footer() {
        Button cancel = new Button("Cancel");
        cancel.getStyleClass().add("lavs-button-secondary");
        cancel.setOnAction(event -> {
            _request.cancelHandler().ifPresent(Runnable::run);
            _back.run();
        });
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);
        HBox row = new HBox(grow, cancel);
        row.setAlignment(Pos.CENTER_RIGHT);
        return Optional.of(row);
    }

    @Override
    public void onRowsReplaced(List<Customer> rows, Runnable redraw) {
        _column.reset();
        redraw.run();
        check(rows, redraw);
    }

    @Override
    public void onRowsAdded(List<Customer> added, Runnable redraw) {
        check(added, redraw);
    }

    private void select(Customer customer) {
        _request.selectedHandler().ifPresent(selected -> selected.accept(customer));
        _back.run();
    }

    private void check(List<Customer> rows, Runnable redraw) {
        int generation = _column.generation();
        List<String> numbers = rows.stream().map(Customer::getNumber).toList();
        _tasks.run(() -> {
            _contacts.checkAll(numbers, (number, check) -> _uiThread.accept(() -> {
                _column.finish(generation, number, check);
                redraw.run();
            }));
            return null;
        }, done -> { }, failure -> {
            _column.failUnfinished(generation, numbers);
            redraw.run();
        });
    }
}
