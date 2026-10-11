package lavs.logic;

import lavs.validation.ValidationResult;
import mainframe.client.Status;

// AI-assisted
//
// The outcome of a LoanStatusService call: field errors (missing data or a
// failed eligibility check) caught before the mainframe was contacted, or
// the Status it returned.
public final class LoanStatusResult {
    private final ValidationResult validationErrors;
    private final Status mainframeStatus;

    private LoanStatusResult(ValidationResult validationErrors, Status mainframeStatus) {
        this.validationErrors = validationErrors;
        this.mainframeStatus = mainframeStatus;
    }

    static LoanStatusResult invalid(ValidationResult validationErrors) {
        return new LoanStatusResult(validationErrors, null);
    }

    static LoanStatusResult fromStatus(Status status) {
        return new LoanStatusResult(new ValidationResult(), status);
    }

    // true only when there were no field errors and the mainframe accepted the request
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
