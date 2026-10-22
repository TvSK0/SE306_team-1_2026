package lavs.logic;

import lavs.model.Loan;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

// The outcome of saving or loading a loan: field errors caught before the mainframe was contacted,
// or the Status it returned, plus the loan when it worked.
public final class LoanSaveResult {
    private final ValidationResult _validationErrors;
    private final Status _mainframeStatus;
    private final Loan _loan;

    private LoanSaveResult(ValidationResult validationErrors, Status mainframeStatus, Loan loan) {
        _validationErrors = validationErrors;
        _mainframeStatus = mainframeStatus;
        _loan = loan;
    }

    static LoanSaveResult invalid(ValidationResult validationErrors) {
        return new LoanSaveResult(validationErrors, null, null);
    }

    static LoanSaveResult saved(Status status, Loan loan) {
        return new LoanSaveResult(new ValidationResult(), status, loan);
    }

    static LoanSaveResult failed(Status status) {
        return new LoanSaveResult(new ValidationResult(), status, null);
    }

    // True only when there were no field errors and the mainframe accepted the loan.
    public boolean isQueued() { return SaveStatus.isQueued(_mainframeStatus); }

    public boolean isSuccessful() {
        return _loan != null && _mainframeStatus.getWasSuccessful();
    }

    public ValidationResult getValidationErrors() {
        return _validationErrors;
    }

    // null when field errors stopped the save before the mainframe was contacted.
    public Status getMainframeStatus() {
        return _mainframeStatus;
    }

    // The loan as the mainframe saved it (with its loan number and status), or null.
    public Loan getLoan() {
        return _loan;
    }
}
