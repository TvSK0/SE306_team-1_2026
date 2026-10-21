package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loan.UpdateStatus;

import java.io.IOException;
import java.util.Optional;

// TEMPORARY: lives in lavs.logic until Scott's logic package skeleton is on
// main (see the same note on ConstantsService); move/adapt this once it is.
//
// US9: change a loan's status to Pending ("submit for verification"), using
// loan.UpdateStatus. Depends on US8 (a loan's co-borrowers/guarantors are in
// place before it's submitted) and on the shared EligibilityChecker.
//
// Guarantor rule (decided by the team, from the Data Schema): a main borrower
// with no employer needs a guarantor on the loan before it can become Pending.
// It is checked here rather than in EligibilityChecker, because US6 uses that
// shared rule to start a loan and a new loan has no guarantors yet.
public final class LoanStatusService {
    public static final String GUARANTOR = "guarantor";

    private final Connection _connection;
    private final LoanBorrowerService _borrowers;

    public LoanStatusService(Connection connection) {
        _connection = connection;
        _borrowers = new LoanBorrowerService(connection);
    }

    // Moves a loan to Pending once the customer passes the shared eligibility
    // check (a mailing address and a phone number or email) and the guarantor
    // rule. customerNumber is the loan's main borrower. Returns a field error -
    // without contacting the mainframe - if loanNumber or customerNumber is
    // missing or the customer is not eligible.
    public LoanStatusResult submitForVerification(String loanNumber, String customerNumber,
            boolean hasMailingAddress, boolean hasPhoneNumber, boolean hasEmail) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("loanNumber", loanNumber));
        validation.addIfPresent(Validators.required("customerNumber", customerNumber));
        if (!EligibilityChecker.isEligible(hasMailingAddress, hasPhoneNumber, hasEmail)) {
            validation.add(new FieldError("eligibility",
                    "the customer needs a mailing address and a phone number or email before this loan can be submitted"));
        }
        if (!validation.isValid()) {
            return LoanStatusResult.invalid(validation);
        }

        Optional<LoanStatusResult> guarantorProblem = checkGuarantorRule(loanNumber, customerNumber);
        if (guarantorProblem.isPresent()) {
            return guarantorProblem.get();
        }

        UpdateStatus message = new UpdateStatus(loanNumber, UpdateStatus.STATUS_PENDING);
        return LoanStatusResult.fromStatus(message.send(_connection));
    }

    // AI-assisted: empty when the loan may go ahead - the main borrower has an
    // employer, or there is a guarantor on the loan. Otherwise the result to
    // return: a "guarantor" field error, or the mainframe's status if a lookup
    // failed. Employers are checked first, so a borrower with one costs a
    // single request.
    private Optional<LoanStatusResult> checkGuarantorRule(String loanNumber, String customerNumber) throws IOException {
        // one employer is enough, so a single one-item page answers the question
        mainframe.client.messages.customerEmployer.List employers =
                new mainframe.client.messages.customerEmployer.List(customerNumber, 1, 1);
        Status employerStatus = employers.send(_connection);
        if (!employerStatus.getWasSuccessful()) {
            return Optional.of(LoanStatusResult.fromStatus(employerStatus));
        }
        if (employers.getItems().length > 0) {
            return Optional.empty();
        }

        LoanBorrowerListResult borrowers = _borrowers.listCoborrowers(loanNumber);
        if (!borrowers.isSuccessful()) {
            return Optional.of(LoanStatusResult.fromStatus(borrowers.getMainframeStatus()));
        }
        boolean hasGuarantor = borrowers.getCoborrowers().stream()
                .anyMatch(coborrower -> AddCoborrower.TYPE_GUARANTOR.equals(coborrower.getType()));
        if (hasGuarantor) {
            return Optional.empty();
        }

        ValidationResult validation = new ValidationResult();
        validation.add(new FieldError(GUARANTOR,
                "the customer has no employer, so add a guarantor to this loan before submitting it"));
        return Optional.of(LoanStatusResult.invalid(validation));
    }
}
