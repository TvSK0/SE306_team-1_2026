package lavs.ui;

import lavs.logic.TemporaryNumbers;
import lavs.model.Customer;

// AI-assisted: how the customer record names a customer and its details tab's buttons.
// No JavaFX, so it is unit tested.
public final class CustomerText {
    public static final String NEW_CUSTOMER = "New customer";
    // AI-assisted: shown instead of the temporary number of a customer made offline (US11)
    public static final String WAITING_TO_UPLOAD = "waiting to upload";

    private CustomerText() {}

    // "Jane Smith (C-10234)", just the number if there is no name yet, or "New customer"
    public static String title(Customer customer) {
        if (customer == null || customer.getNumber() == null) {
            return NEW_CUSTOMER;
        }
        String name = customer.getFullName();
        // AI-assisted: a temporary number means nothing to the MMM, so it isn't shown (US11)
        String number = TemporaryNumbers.isTemporary(customer.getNumber()) ? WAITING_TO_UPLOAD : customer.getNumber();
        return name.isEmpty() ? number : name + " (" + number + ")";
    }

    // "Status: Active", or what a customer not yet saved shows
    public static String status(Customer customer) {
        if (customer == null || customer.getNumber() == null) {
            return "Status: New (not saved yet)";
        }
        if (TemporaryNumbers.isTemporary(customer.getNumber())) {
            return "Status: New (" + WAITING_TO_UPLOAD + ")";
        }
        return "Status: " + (customer.getStatus() == null ? "unknown" : customer.getStatus());
    }

    // the Save button: creating a new customer, or saving an existing one
    public static String saveButton(Customer customer) {
        return customer == null ? "Create customer" : "Save changes";
    }

    // what to say once a save has worked
    public static String saved(Customer before, Customer after) {
        // AI-assisted: a customer still waiting to upload has no number to give yet (US11)
        if (before == null && TemporaryNumbers.isTemporary(after.getNumber())) {
            return "Customer created.";
        }
        if (before == null) {
            return "Customer created. Their number is " + after.getNumber() + ".";
        }
        return "Changes saved.";
    }
}
