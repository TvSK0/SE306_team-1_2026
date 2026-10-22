package lavs.ui;

import lavs.model.Customer;
import java.util.Objects;
import java.util.function.Consumer;

// AI-assisted: selection returns to the existing form before delivering its chosen customer.
public record CustomerSelection(Consumer<Customer> selected) {
    public CustomerSelection { Objects.requireNonNull(selected); }
}
