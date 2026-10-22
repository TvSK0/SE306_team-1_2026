// AI-assisted: what differs between normal customer search and select mode.
package lavs.ui;

import javafx.scene.Node;
import lavs.model.Customer;

import java.util.List;
import java.util.Optional;

// CustomerSearchScreen is the same search in both: the field, Search and Cancel, loading,
// the count line, the rows and "Show more". A mode supplies the rest: the heading and its
// button, an optional extra control above the field, the table's columns and row action,
// an optional footer, and what to do when rows appear (select mode checks contact details).
public interface CustomerSearchMode {
    String title();

    String subtitle();

    void configureHeading(SectionHeading heading);

    // shown between the heading and the search field
    default Optional<Node> extraControls() {
        return Optional.empty();
    }

    // adds the columns and the row action
    void configureTable(RecordTable<Customer> table);

    // shown under the results
    default Optional<Node> footer() {
        return Optional.empty();
    }

    // A search worked: these are now the only rows. redraw shows the table again after the
    // mode changed something a column reads.
    default void onRowsReplaced(List<Customer> rows, Runnable redraw) {
    }

    // "Show more" worked: these rows were added under the existing ones
    default void onRowsAdded(List<Customer> added, Runnable redraw) {
    }
}
