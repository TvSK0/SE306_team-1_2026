package lavs.logic;

import lavs.model.LoanCoborrower;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

import java.util.List;

// AI-assisted
//
// The outcome of LoanBorrowerService.listCoborrowers: either field errors
// caught before the mainframe was contacted, or the Status it returned along
// with every co-borrower and guarantor on the loan (empty unless successful).
public final class LoanBorrowerListResult {
    private final ValidationResult validationErrors;
    private final Status mainframeStatus;
    private final List<LoanCoborrower> coborrowers;

    private LoanBorrowerListResult(ValidationResult validationErrors, Status mainframeStatus,
                                   List<LoanCoborrower> coborrowers) {
        this.validationErrors = validationErrors;
        this.mainframeStatus = mainframeStatus;
        this.coborrowers = List.copyOf(coborrowers);
    }

    static LoanBorrowerListResult invalid(ValidationResult validationErrors) {
        return new LoanBorrowerListResult(validationErrors, null, List.of());
    }

    static LoanBorrowerListResult fromStatus(Status status, List<LoanCoborrower> coborrowers) {
        return new LoanBorrowerListResult(new ValidationResult(), status, coborrowers);
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

    // in the order the mainframe returned them; cannot be modified
    public List<LoanCoborrower> getCoborrowers() {
        return coborrowers;
    }
}
