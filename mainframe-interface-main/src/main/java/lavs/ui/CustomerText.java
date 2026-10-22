package lavs.ui;

import lavs.model.Customer;

// AI-assisted: how the customer record names a customer and its details tab's buttons.
// No JavaFX, so it is unit tested.
public final class CustomerText {
    public static final String NEW_CUSTOMER = "New customer";

    private CustomerText() {}

    // "Jane Smith (C-10234)", just the number if there is no name yet, or "New customer"
    public static String title(Customer customer) {
        if (customer == null || customer.getNumber() == null) {
            return NEW_CUSTOMER;
        }
        String name = customer.getFullName();
        return name.isEmpty() ? customer.getNumber() : name + " (" + customer.getNumber() + ")";
    }

    // "Status: Active", or what a customer not yet saved shows
    public static String status(Customer customer) {
        if (customer == null || customer.getNumber() == null) {
            return "Status: New (not saved yet)";
        }
        return "Status: " + (customer.getStatus() == null ? "unknown" : customer.getStatus());
    }

    // the Save button: creating a new customer, or saving an existing one
    public static String saveButton(Customer customer) {
        return customer == null ? "Create customer" : "Save changes";
    }

    // what to say once a save has worked
    public static String saved(Customer before, Customer after) {
        if (before == null) {
            return "Customer created. Their number is " + after.getNumber() + ".";
        }
        return "Changes saved.";
    }
}
