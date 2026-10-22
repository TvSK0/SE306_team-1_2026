// AI-assisted.
package lavs.logic;

import lavs.model.PendingChange;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

// The decisions behind the pending-uploads panel, with no JavaFX so they can be unit tested.
// Reads the queue from SyncService and formats each change into a Row the panel can display.
// AI-assisted: lives in logic (not ui) so its tests count toward the coverage gate (US11).
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

    // One row per queued change, in creation order (oldest first). Reads the local database, so
    // call it off the UI thread.
    public List<Row> getRows() {
        return _sync.getChanges().stream()
                .map(c -> new Row(c, descriptionFor(c), statusTextFor(c)))
                .collect(Collectors.toList());
    }

    // AI-assisted: what the MMM does about a row (US11). Each runs on the sync thread; the
    // returned future completes there, so the panel hops back to the JavaFX thread before
    // it shows the queue again.

    // takes a rejected change (and anything rejected because of it) off the list once the MMM has seen why
    public CompletableFuture<Void> dismiss(Row row) {
        return _sync.dismissRejected(row.getId());
    }

    // the new customer is one already on the mainframe: their changes go to that customer instead
    public CompletableFuture<Void> useExistingCustomer(Row row, String existingNumber) {
        return _sync.resolveDuplicate(row.getId(), Objects.requireNonNull(existingNumber, "existingNumber"));
    }

    // the new customer really is new: upload them without searching again
    public CompletableFuture<Void> confirmNewCustomer(Row row) {
        return _sync.resolveDuplicate(row.getId(), null);
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
        private final long id;
        private final PendingChange.Status status;
        private final String description;
        private final String customerNumber;
        private final String statusText;
        private final String reason;
        private final List<String> duplicateMatches;
        private final String savedDetails;

        Row(PendingChange change, String description, String statusText) {
            this.id = change.getId() == null ? 0 : change.getId();
            this.status = change.getStatus();
            this.description = description;
            this.customerNumber = change.getCustomerNumber();
            this.statusText = statusText;
            this.reason = change.getStatusMessage();
            this.duplicateMatches = DuplicateCandidates.forChange(change);
            this.savedDetails = change.getFields().entrySet().stream()
                    .map(entry -> entry.getKey() + ": " + Objects.toString(entry.getValue(), ""))
                    .collect(Collectors.joining("\n"));
        }

        // e.g. "Add employer"
        public String getDescription() { return description; }
        // e.g. "C-1234", or a temporary number for a customer made offline; null when none
        public String getCustomerNumber() { return customerNumber; }
        // e.g. "Waiting to upload"
        public String getStatusText() { return statusText; }

        // AI-assisted: what the "Open customer" button says. A temporary number means nothing to the MMM.
        public String getCustomerLabel() {
            return TemporaryNumbers.isTemporary(customerNumber) ? "new customer" : customerNumber;
        }

        // AI-assisted: why it was paused or rejected, e.g. "Unknown customer"; null while pending
        public String getReason() { return reason; }

        // AI-assisted: the queued values, one "field: value" per line, so the MMM can copy them
        public String getSavedDetails() { return savedDetails; }

        // AI-assisted: the queued change this row shows
        long getId() { return id; }

        // AI-assisted: a rejected change can be taken off the list once the MMM has seen it
        public boolean canDismiss() {
            return status == PendingChange.Status.REJECTED;
        }

        // AI-assisted: only the paused new customer's own row offers the choice, since the pause
        // covers every change for them and one answer resolves them all
        public boolean canResolveDuplicate() {
            return !duplicateMatches.isEmpty();
        }

        // AI-assisted: the existing customers this new one may be, e.g. ["C-10234"]
        public List<String> getDuplicateMatches() { return duplicateMatches; }
    }
}
