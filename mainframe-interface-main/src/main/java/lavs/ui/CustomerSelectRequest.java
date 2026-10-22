// AI-assisted: what a screen passes to navigator.goTo(Screen.CUSTOMER_SELECT, ...).
package lavs.ui;

import javafx.scene.Node;
import lavs.model.Customer;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

// Select mode (Figma 10): the customer search with the caller's own wording, an optional extra
// control above the search field (Gabriel's "Add as: Co-borrower / Guarantor" choice), a
// "Select" action on each row and a Cancel button. Select and Cancel both go back to the caller
// with navigator.back(); onSelected gets the chosen customer first.
//
// topBarTitle and subtitle may be null. extraControls, onSelected, onCancel and onAddNew may be
// null: with no onAddNew the "+ Add New Customer" button is left out.
public record CustomerSelectRequest(String topBarTitle, String title, String subtitle, Node extraControls,
                                    Consumer<Customer> onSelected, Runnable onCancel, Runnable onAddNew)
        implements TitledParameter {
    public CustomerSelectRequest {
        Objects.requireNonNull(title, "select mode needs a heading");
    }

    // The navigation parameter as a request. Anything else is a programming mistake by the
    // screen that opened select mode, so it fails loudly and the Navigator stays where it is.
    public static CustomerSelectRequest from(Object parameter) {
        if (parameter instanceof CustomerSelectRequest request) {
            return request;
        }
        throw new IllegalArgumentException("Screen.CUSTOMER_SELECT needs a CustomerSelectRequest, got: " + parameter);
    }

    public Optional<Node> extraControlsNode() {
        return Optional.ofNullable(extraControls);
    }

    public Optional<Consumer<Customer>> selectedHandler() {
        return Optional.ofNullable(onSelected);
    }

    public Optional<Runnable> cancelHandler() {
        return Optional.ofNullable(onCancel);
    }

    public Optional<Runnable> addNewHandler() {
        return Optional.ofNullable(onAddNew);
    }
}
