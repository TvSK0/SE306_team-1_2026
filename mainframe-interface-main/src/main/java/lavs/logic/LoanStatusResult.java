package lavs.logic;

import lavs.validation.ValidationResult;
import mainframe.client.Status;

// AI-assisted
//
// The outcome of a LoanStatusService call: field errors (missing data, a loan
// that isn't New, or a failed eligibility or guarantor check) caught before
// the status was changed, or the Status the mainframe returned.
//
// AI-assisted: the status change goes through the upload queue (US11), so it can
// also be waiting to upload.
public final class LoanStatusResult {
    private final ValidationResult validationErrors;
    private final Status mainframeStatus;
    private final String loanStatus;
    private final boolean waiting;

    private LoanStatusResult(ValidationResult validationErrors, Status mainframeStatus, String loanStatus,
                             boolean waiting) {
        this.validationErrors = validationErrors;
        this.mainframeStatus = mainframeStatus;
        this.loanStatus = loanStatus;
        this.waiting = waiting;
    }

    static LoanStatusResult invalid(ValidationResult validationErrors) {
        return new LoanStatusResult(validationErrors, null, null, false);
    }

    // a request that did not change the status: a failed lookup or a rejected update
    static LoanStatusResult fromStatus(Status status) {
        return new LoanStatusResult(new ValidationResult(), status, null, false);
    }

    // AI-assisted: the status change went through the upload queue. loanStatus is the status
    // asked for (e.g. "Pending"); it is kept while the change waits to upload and dropped if it
    // was rejected.
    static LoanStatusResult queued(SyncStep step, String loanStatus) {
        return new LoanStatusResult(new ValidationResult(), QueuedWrites.statusFor(step),
                QueuedWrites.keptValue(step, loanStatus), QueuedWrites.isWaiting(step));
    }

    // true only when there were no field errors and the mainframe accepted the request
    public boolean isSuccessful() {
        return validationErrors.isValid() && mainframeStatus.getWasSuccessful();
    }

    // AI-assisted: true when the change is kept on this laptop to upload once the mainframe can be reached
    public boolean isWaitingToUpload() {
        return waiting;
    }

    // AI-assisted: true when the change is safe: on the mainframe, or on the upload queue
    public boolean isSaved() {
        return waiting || isSuccessful();
    }

    public ValidationResult getValidationErrors() {
        return validationErrors;
    }

    // null when field errors stopped the request before the mainframe was contacted
    public Status getMainframeStatus() {
        return mainframeStatus;
    }

    // the loan's status after a change that was saved, e.g. "Pending"; null otherwise
    public String getLoanStatus() {
        return loanStatus;
    }
}
