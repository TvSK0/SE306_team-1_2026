// AI-assisted: what a customer record section is given when its view is built.
package lavs.ui;

import lavs.model.Customer;

import java.util.function.Consumer;
import java.util.function.Supplier;

// customerNumber is null for a new customer that has not been saved. customer() is the
// customer as loaded for the header and is null until it has loaded (or if loading failed), so
// a section should work from the number and use the customer only for wording such as the
// name. goTo selects another section of the same record (the Loans banner's "Go to Addresses
// →" link). refreshCounts asks the sidebar to count again after the section added or deleted
// something.
public record SectionContext(String customerNumber, Supplier<Customer> customer, Consumer<Section> goTo,
                             Runnable refreshCounts) {
}
