package lavs.logic;

// AI-assisted: reshaped for the queue path (US11). An employer write is now queued through
// SyncService, so the outcome is SAVED (sent and accepted), WAITING (saved locally, will upload
// once the mainframe is reachable) or REJECTED (the mainframe refused, or the local save failed),
// plus any field errors caught before it was queued. This is the template for other services
// moving onto SyncService; it differs from ServiceResult because, on the queue, being offline is
// WAITING (not a failure).
import lavs.validation.ValidationResult;

public final class EmployerResult {
    // SAVED = sent and accepted; WAITING = queued, will upload (offline, or held behind a paused
    // duplicate customer); REJECTED = the mainframe refused, or the change could not be saved locally.
    public enum Outcome { SAVED, WAITING, REJECTED }

    private final ValidationResult validationErrors;
    private final Outcome outcome;   // null when field errors stopped the call before it was queued
    private final String message;    // the reason to show the MMM; null on SAVED

    private EmployerResult(ValidationResult validationErrors, Outcome outcome, String message) {
        this.validationErrors = validationErrors;
        this.outcome = outcome;
        this.message = message;
    }

    static EmployerResult invalid(ValidationResult validationErrors) {
        return new EmployerResult(validationErrors, null, null);
    }

    static EmployerResult saved() {
        return new EmployerResult(new ValidationResult(), Outcome.SAVED, null);
    }

    static EmployerResult waiting(String message) {
        return new EmployerResult(new ValidationResult(), Outcome.WAITING, message);
    }

    static EmployerResult rejected(String message) {
        return new EmployerResult(new ValidationResult(), Outcome.REJECTED, message);
    }

    // true only when there were no field errors and the change was sent and accepted.
    public boolean isSuccessful() {
        return validationErrors.isValid() && outcome == Outcome.SAVED;
    }

    // true when the change was saved locally and will upload once the mainframe is reachable.
    public boolean isWaitingToUpload() {
        return outcome == Outcome.WAITING;
    }

    public ValidationResult getValidationErrors() {
        return validationErrors;
    }

    // SAVED, WAITING or REJECTED; null when field errors stopped the call before it was queued.
    public Outcome getOutcome() {
        return outcome;
    }

    // why it is waiting or was rejected; null on SAVED or when field errors stopped the call.
    public String getMessage() {
        return message;
    }
}
