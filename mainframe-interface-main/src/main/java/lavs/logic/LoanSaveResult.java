package lavs.logic;

import lavs.model.Loan;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

// The outcome of saving or loading a loan: field errors caught before the mainframe was contacted,
// or the Status it returned, plus the loan when it worked.
// AI-assisted: a save goes through the upload queue (US11), so it can also be waiting to upload.
public final class LoanSaveResult {
    private final ValidationResult _validationErrors;
    private final Status _mainframeStatus;
    private final Loan _loan;
    private final boolean _waiting;

    private LoanSaveResult(ValidationResult validationErrors, Status mainframeStatus, Loan loan, boolean waiting) {
        _validationErrors = validationErrors;
        _mainframeStatus = mainframeStatus;
        _loan = loan;
        _waiting = waiting;
    }

    static LoanSaveResult invalid(ValidationResult validationErrors) {
        return new LoanSaveResult(validationErrors, null, null, false);
    }

    static LoanSaveResult saved(Status status, Loan loan) {
        return new LoanSaveResult(new ValidationResult(), status, loan, false);
    }

    static LoanSaveResult failed(Status status) {
        return new LoanSaveResult(new ValidationResult(), status, null, false);
    }

    // AI-assisted: a save that went through the upload queue; loan is what was saved
    static LoanSaveResult queued(SyncStep step, Loan loan) {
        return new LoanSaveResult(new ValidationResult(), QueuedWrites.statusFor(step),
                QueuedWrites.keptValue(step, loan), QueuedWrites.isWaiting(step));
    }

    // True only when there were no field errors and the mainframe accepted the loan.
    public boolean isSuccessful() {
        return _loan != null && !_waiting;
    }

    // AI-assisted: true when the loan is safe: on the mainframe, or on the upload queue
    public boolean isSaved() {
        return _loan != null;
    }

    // AI-assisted: true when the loan is kept on this laptop to upload once the mainframe can be reached
    public boolean isWaitingToUpload() {
        return _waiting;
    }

    public ValidationResult getValidationErrors() {
        return _validationErrors;
    }

    // null when field errors stopped the save before the mainframe was contacted.
    public Status getMainframeStatus() {
        return _mainframeStatus;
    }

    // The loan as it was saved, or null. A new loan still waiting to upload has a temporary
    // number (see TemporaryNumbers); an update saved through the queue has no status, since the
    // mainframe's answer doesn't come back through it.
    public Loan getLoan() {
        return _loan;
    }
}
