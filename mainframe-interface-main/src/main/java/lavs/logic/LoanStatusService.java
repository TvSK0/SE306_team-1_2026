package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.messages.loan.UpdateStatus;

import java.io.IOException;

// TEMPORARY: lives in lavs.logic until Scott's logic package skeleton is on
// main (see the same note on ConstantsService); move/adapt this once it is.
//
// US9: change a loan's status to Pending ("submit for verification"), using
// loan.UpdateStatus. Depends on US8 (a loan's co-borrowers/guarantors are in
// place before it's submitted) and on the shared EligibilityChecker.
//
// OPEN QUESTION, raised with James on Ed, NOT implemented here: whether a
// main borrower with no employer must also have a guarantor before a loan
// can become Pending. The project notes suggest blocking submission on it,
// but that has not been confirmed, so submitForVerification() only checks
// the shared eligibility rule (mailing address + phone or email) below.
// Do not add a guarantor/employer check here until that question is answered.
public final class LoanStatusService {
    private final Connection _connection;

    public LoanStatusService(Connection connection) {
        _connection = connection;
    }

    // Moves a loan to Pending once the customer passes the shared
    // eligibility check (a mailing address and a phone number or email).
    // Returns a field error - without contacting the mainframe - if
    // loanNumber is missing or the customer is not eligible.
    public LoanStatusResult submitForVerification(
            String loanNumber, boolean hasMailingAddress, boolean hasPhoneNumber, boolean hasEmail) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("loanNumber", loanNumber));
        if (!EligibilityChecker.isEligible(hasMailingAddress, hasPhoneNumber, hasEmail)) {
            validation.add(new FieldError("eligibility",
                    "the customer needs a mailing address and a phone number or email before this loan can be submitted"));
        }
        if (!validation.isValid()) {
            return LoanStatusResult.invalid(validation);
        }

        UpdateStatus message = new UpdateStatus(loanNumber, UpdateStatus.STATUS_PENDING);
        return LoanStatusResult.fromStatus(message.send(_connection));
    }
}
