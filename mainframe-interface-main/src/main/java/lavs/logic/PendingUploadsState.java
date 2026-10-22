// AI-assisted.
package lavs.logic;

import lavs.model.PendingChange;

import java.util.List;
import java.util.stream.Collectors;

// The decisions behind the pending-uploads panel, with no JavaFX so they can be unit tested.
// Reads the queue from SyncService and formats each change into a Row the panel can display.
public final class PendingUploadsState {
    private final SyncService _sync;

    public PendingUploadsState(SyncService sync) {
        _sync = sync;
    }

    // True when the queue is completely empty (no changes at all, not just no PENDING ones)
    public boolean isEmpty() {
        return _sync.getChanges().isEmpty();
    }

    // True when at least one change needs the MMM's attention (paused or rejected)
    public boolean hasActionNeeded() {
        return _sync.getChanges().stream()
                .anyMatch(c -> c.getStatus() != PendingChange.Status.PENDING);
    }

    // One row per queued change, in creation order (oldest first)
    public List<Row> getRows() {
        return _sync.getChanges().stream()
                .map(c -> new Row(descriptionFor(c), c.getCustomerNumber(), statusTextFor(c)))
                .collect(Collectors.toList());
    }

    // Human-readable label for the change, e.g. "Add employer" for "customer.addEmployer".
    // Covers every write message the app currently sends; unknown types fall back to the raw name.
    public static String descriptionFor(PendingChange change) {
        return switch (change.getMessageType()) {
            case "customer.create"           -> "Create customer";
            case "customer.update"           -> "Update customer details";
            case "customer.addAddress"       -> "Add address";
            case "customer.updateAddress"    -> "Update address";
            case "customer.deleteAddress"    -> "Delete address";
            case "customer.addPhoneNumber"   -> "Add phone number";
            case "customer.updatePhoneNumber"-> "Update phone number";
            case "customer.deletePhoneNumber"-> "Delete phone number";
            case "customer.addEmail"         -> "Add email";
            case "customer.updateEmail"      -> "Update email";
            case "customer.deleteEmail"      -> "Delete email";
            case "customer.addEmployer"      -> "Add employer";
            case "customer.updateEmployer"   -> "Update employer";
            case "customer.deleteEmployer"   -> "Delete employer";
            case "customer.addNote"          -> "Add customer note";
            case "customer.updateNote"       -> "Update customer note";
            case "customer.deleteNote"       -> "Delete customer note";
            case "customer.addDocument"      -> "Add customer document";
            case "customer.deleteDocument"   -> "Delete customer document";
            case "customer.addRelationship"  -> "Add relationship";
            case "customer.updateRelationship"-> "Update relationship";
            case "customer.deleteRelationship"-> "Delete relationship";
            case "customer.updateStatus"     -> "Update customer status";
            case "loan.create"               -> "Create loan";
            case "loan.update"               -> "Update loan";
            case "loan.addCoborrower"        -> "Add co-borrower";
            case "loan.deleteCoborrower"     -> "Remove co-borrower";
            case "loan.updateStatus"         -> "Submit loan for verification";
            case "loan.addNote"              -> "Add loan note";
            case "loan.updateNote"           -> "Update loan note";
            case "loan.deleteNote"           -> "Delete loan note";
            case "loan.addDocument"          -> "Add loan document";
            case "loan.deleteDocument"       -> "Delete loan document";
            case "loan.addPayment"           -> "Add payment";
            case "loan.deletePayment"        -> "Delete payment";
            default                          -> change.getMessageType();
        };
    }

    // maps status to short labels that describe what the queue is doingw ith this change.
    public static String statusTextFor(PendingChange change) {
        return switch (change.getStatus()) {
            case PENDING  -> "Waiting to upload";
            case PAUSED   -> "Paused — action needed";
            case REJECTED -> "Rejected by mainframe";
        };
    }

    // This is one row in the pending-uploads panel
    public static final class Row {
        private final String description;
        private final String customerNumber;
        private final String statusText;

        Row(String description, String customerNumber, String statusText) {
            this.description = description;
            this.customerNumber = customerNumber;
            this.statusText = statusText;
        }

        // e.g. "Add employer"
        public String getDescription() { return description; }
        // e.g. "C-1234"
        public String getCustomerNumber() { return customerNumber; }
        // e.g. "Waiting to upload"
        public String getStatusText() { return statusText; }
    }
}
