// AI-assisted.
package lavs.ui;

import lavs.logic.SyncService;
import lavs.logic.TemporaryNumbers;
import lavs.model.PendingChange;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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
                .map(c -> new Row(c, descriptionFor(c), customerNumberFor(c), statusTextFor(c)))
                .collect(Collectors.toList());
    }

    // AI-assisted: what the MMM does about a row (US11). Each runs on the sync thread; the
    // returned future completes there, so the panel hops back to the JavaFX thread before
    // it shows the queue again.

    // takes a rejected change off the list once the MMM has seen why
    public CompletableFuture<Void> dismiss(Row row) {
        return _sync.dismissRejected(row.getId());
    }

    // the new customer is one already on the mainframe: their changes go to that customer instead
    public CompletableFuture<Void> useExistingCustomer(Row row, String existingNumber) {
        return _sync.useExistingCustomer(row.getTemporaryNumber(), existingNumber);
    }

    // the new customer really is new: upload them without searching again
    public CompletableFuture<Void> confirmNewCustomer(Row row) {
        return _sync.confirmNewCustomer(row.getTemporaryNumber());
    }

    // AI-assisted: a customer made offline has only a temporary number, which means nothing to the MMM
    static String customerNumberFor(PendingChange change) {
        return TemporaryNumbers.isTemporary(change.getCustomerNumber()) ? "New customer" : change.getCustomerNumber();
    }

    // Human-readable label for the change, e.g. "Add employer" for "customer.addEmployer".
    // Covers every write message the app currently sends; unknown types fall back to the raw name.
    static String descriptionFor(PendingChange change) {
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
    static String statusTextFor(PendingChange change) {
        return switch (change.getStatus()) {
            case PENDING  -> "Waiting to upload";
            case PAUSED   -> "Paused — action needed";
            case REJECTED -> "Rejected by mainframe";
        };
    }

    // This is one row in the pending-uploads panel
    public static final class Row {
        private final Long id;
        private final String description;
        private final String customerNumber;
        private final String statusText;
        private final PendingChange.Status status;
        private final String temporaryNumber;
        private final List<String> duplicateMatches;
        private final String reason;

        Row(PendingChange change, String description, String customerNumber, String statusText) {
            this.id = change.getId();
            this.description = description;
            this.customerNumber = customerNumber;
            this.statusText = statusText;
            this.status = change.getStatus();
            this.temporaryNumber = change.getCreatesNumber();
            this.duplicateMatches = change.getDuplicateMatches();
            this.reason = change.getStatusMessage();
        }

        // AI-assisted: why it was paused or rejected, e.g. "Unknown customer"; null while pending
        public String getReason() { return reason; }

        // e.g. "Add employer"
        public String getDescription() { return description; }
        // e.g. "C-1234"
        public String getCustomerNumber() { return customerNumber; }
        // e.g. "Waiting to upload"
        public String getStatusText() { return statusText; }

        // AI-assisted: the queued change this row shows
        Long getId() { return id; }

        // AI-assisted: the temporary number of the customer (or loan) this change creates, or null
        String getTemporaryNumber() { return temporaryNumber; }

        // AI-assisted: a rejected change can be taken off the list once the MMM has seen it
        public boolean canDismiss() {
            return status == PendingChange.Status.REJECTED;
        }

        // AI-assisted: only the new customer's own row offers the choice, since the pause covers
        // every change for them and one answer resolves them all
        public boolean canResolveDuplicate() {
            return status == PendingChange.Status.PAUSED && temporaryNumber != null && !duplicateMatches.isEmpty();
        }

        // AI-assisted: the existing customers this new one may be, e.g. ["C-10234"]
        public List<String> getDuplicateMatches() { return duplicateMatches; }
    }
}
