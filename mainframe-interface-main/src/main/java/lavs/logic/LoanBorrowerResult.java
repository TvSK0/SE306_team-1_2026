package lavs.logic;

import lavs.validation.ValidationResult;
import mainframe.client.Status;

// AI-assisted
//
// The outcome of a LoanBorrowerService call: either field errors caught
// before the mainframe was contacted, or the Status it returned.
//
// AI-assisted: a change goes through the upload queue (US11), so it can also be
// waiting to upload.
public final class LoanBorrowerResult {
    private final ValidationResult validationErrors;
    private final Status mainframeStatus;
    private final boolean waiting;

    private LoanBorrowerResult(ValidationResult validationErrors, Status mainframeStatus, boolean waiting) {
        this.validationErrors = validationErrors;
        this.mainframeStatus = mainframeStatus;
        this.waiting = waiting;
    }

    static LoanBorrowerResult invalid(ValidationResult validationErrors) {
        return new LoanBorrowerResult(validationErrors, null, false);
    }

    static LoanBorrowerResult fromStatus(Status status) {
        return new LoanBorrowerResult(new ValidationResult(), status, false);
    }

    // AI-assisted: a change that went through the upload queue
    static LoanBorrowerResult queued(SyncStep step) {
        return new LoanBorrowerResult(new ValidationResult(), QueuedWrites.statusFor(step), QueuedWrites.isWaiting(step));
    }

    // true only when there were no field errors and the mainframe accepted the
    // request. Every way of building one of these other than invalid() sets
    // mainframeStatus, so it is never null when validationErrors is valid.
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
}
