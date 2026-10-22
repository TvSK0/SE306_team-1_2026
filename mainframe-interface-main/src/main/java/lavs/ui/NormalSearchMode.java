// AI-assisted: the customer search screen as a stand-alone page (Figma 02).
package lavs.ui;

import lavs.model.Customer;

import java.util.function.Consumer;

// "Open record" opens the customer's record; "+ Add New Customer" opens a new, empty one.
public class NormalSearchMode implements CustomerSearchMode {
    private final Consumer<String> _onOpenRecord;
    private final Runnable _onAddNew;

    // onOpenRecord gets the customer number
    public NormalSearchMode(Consumer<String> onOpenRecord, Runnable onAddNew) {
        _onOpenRecord = onOpenRecord;
        _onAddNew = onAddNew;
    }

    @Override
    public String title() {
        return "Find a customer";
    }

    @Override
    public String subtitle() {
        return "Search before adding a new customer to avoid creating a duplicate.";
    }

    // stays enabled offline: an offline MMM can still enter a new customer (US2-R5)
    @Override
    public void configureHeading(SectionHeading heading) {
        heading.setAction("+ Add New Customer", _onAddNew);
    }

    @Override
    public void configureTable(RecordTable<Customer> table) {
        table.column("Customer no.", Customer::getNumber, 2)
                .column("Name", Customer::getFullName, 3)
                .column("Date of birth", customer -> CustomerSearchText.dateOfBirth(customer.getDateOfBirth()), 2)
                .nodeColumn("Status", customer -> {
                    CustomerSearchText.StatusTag tag = CustomerSearchText.statusTag(customer.getStatus());
                    return new Tag(tag.variant(), tag.text());
                }, 2)
                .rowAction("Open record", customer -> _onOpenRecord.accept(customer.getNumber()));
    }
}
