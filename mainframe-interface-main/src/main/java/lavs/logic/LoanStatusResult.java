package lavs.logic;

import lavs.validation.ValidationResult;
import mainframe.client.Status;

// AI-assisted
//
// The outcome of a LoanStatusService call: field errors (missing data, a loan
// that isn't New, or a failed eligibility or guarantor check) caught before
// the status was changed, or the Status the mainframe returned.
public final class LoanStatusResult {
    private final ValidationResult validationErrors;
    private final Status mainframeStatus;
    private final String loanStatus;

    private LoanStatusResult(ValidationResult validationErrors, Status mainframeStatus, String loanStatus) {
        this.validationErrors = validationErrors;
        this.mainframeStatus = mainframeStatus;
        this.loanStatus = loanStatus;
    }

    static LoanStatusResult invalid(ValidationResult validationErrors) {
        return new LoanStatusResult(validationErrors, null, null);
    }

    // a request that did not change the status: a failed lookup or a rejected update
    static LoanStatusResult fromStatus(Status status) {
        return new LoanStatusResult(new ValidationResult(), status, null);
    }

    // the mainframe changed the status; loanStatus is the one it now reports
    static LoanStatusResult changed(Status status, String loanStatus) {
        return new LoanStatusResult(new ValidationResult(), status, loanStatus);
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

    // AI-assisted: the same answer for the screens, which don't use mainframe.client.Status
    public MainframeReply getReply() {
        return MainframeReply.of(getMainframeStatus());
    }

    // the loan's status after a successful change, e.g. "Pending"; null otherwise
    public String getLoanStatus() {
        return loanStatus;
    }
}
