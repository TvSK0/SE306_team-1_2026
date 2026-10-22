package lavs.logic;

// AI-assisted: service contract consumed by the application.
public interface LoanOperations {
    boolean canSaveLocally();
    LoanSaveResult createLoan(String customerNumber, LoanDetailsInput input) throws java.io.IOException;
    LoanSaveResult updateLoan(String customerNumber, String loanNumber, LoanDetailsInput input) throws java.io.IOException;
    LoanSaveResult loadLoan(String loanNumber) throws java.io.IOException;
    lavs.validation.ValidationResult validate(LoanDetailsInput input);
    lavs.validation.ValidationResult checkCanStartLoan(boolean mailing, boolean phone, boolean email);
}
