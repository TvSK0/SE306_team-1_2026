package lavs.logic;

// AI-assisted.
import lavs.validation.ValidationResult;
import mainframe.client.Status;

// The outcome of an EmployerService call: either field errors caught before
// the mainframe was contacted, or the Status it returned.
public final class EmployerResult {
    private final ValidationResult validationErrors;
    private final Status mainframeStatus;

    private EmployerResult(ValidationResult validationErrors, Status mainframeStatus) {
        this.validationErrors = validationErrors;
        this.mainframeStatus = mainframeStatus;
    }

    static EmployerResult invalid(ValidationResult validationErrors) {
        return new EmployerResult(validationErrors, null);
    }

    static EmployerResult fromStatus(Status status) {
        return new EmployerResult(new ValidationResult(), status);
    }

    // true only when there were no field errors and the mainframe accepted the request.
    // invalid() and fromStatus() are the only ways to build one of these, so
    // mainframeStatus is never null when validationErrors is valid.
    public boolean isSuccessful() {
        return validationErrors.isValid() && mainframeStatus.getWasSuccessful();
    }

    public ValidationResult getValidationErrors() {
        return validationErrors;
    }

    // null when field errors stopped the request before the mainframe was contacted
    public Status getMainframeStatus() {
        return mainframeStatus;
    }
}
