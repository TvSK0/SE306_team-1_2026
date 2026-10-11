package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.messages.loan.AddCoborrower;
import mainframe.client.messages.loan.DeleteCoborrower;

import java.io.IOException;

// TEMPORARY: lives in lavs.logic until Scott's logic package skeleton is on
// main (see the same note on ConstantsService); move/adapt this once it is.
//
// US8: co-borrowers and guarantors on a loan.
//
// This is the service-logic slice only - no screen yet. US8 depends on US6
// (login -> customer search -> open customer record) to find the customer
// being added, and on customer search running in "select" mode; neither
// exists on main yet, so the Loan Borrowers screen itself is blocked on
// that, and on the team's UI framework choice. See the project notes.
public final class LoanBorrowerService {
    private final Connection _connection;

    public LoanBorrowerService(Connection connection) {
        _connection = connection;
    }

    // Adds a customer as a co-borrower or guarantor on a loan, using
    // loan.AddCoborrower. Returns field errors - without contacting the
    // mainframe - if loanNumber/customerNumber/type are missing, or type
    // isn't AddCoborrower.TYPE_COBORROWER/TYPE_GUARANTOR.
    public LoanBorrowerResult addCoborrower(String loanNumber, String customerNumber, String type) throws IOException {
        ValidationResult validation = validateCoborrower(loanNumber, customerNumber, type);
        if (!validation.isValid()) {
            return LoanBorrowerResult.invalid(validation);
        }
        return LoanBorrowerResult.fromStatus(sendAdd(loanNumber, customerNumber, type));
    }

    // Removes an existing co-borrower/guarantor record from a loan, using
    // loan.DeleteCoborrower.
    public LoanBorrowerResult removeCoborrower(long coborrowerRecordId, String loanNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("loanNumber", loanNumber));
        if (!validation.isValid()) {
            return LoanBorrowerResult.invalid(validation);
        }
        return LoanBorrowerResult.fromStatus(sendDelete(coborrowerRecordId, loanNumber));
    }

    // US8: "updates remove then re-add" - the mainframe has no update
    // message for a co-borrower/guarantor, so changing one removes the
    // existing record (by id) and adds the replacement. If the removal
    // fails, the add is never attempted, so a failed update can't leave
    // two records for the same person.
    public LoanBorrowerResult updateCoborrower(long existingRecordId, String loanNumber, String customerNumber, String type) throws IOException {
        ValidationResult validation = validateCoborrower(loanNumber, customerNumber, type);
        if (!validation.isValid()) {
            return LoanBorrowerResult.invalid(validation);
        }

        Status deleteStatus = sendDelete(existingRecordId, loanNumber);
        if (!deleteStatus.getWasSuccessful()) {
            return LoanBorrowerResult.fromStatus(deleteStatus);
        }
        return LoanBorrowerResult.fromStatus(sendAdd(loanNumber, customerNumber, type));
    }

    private ValidationResult validateCoborrower(String loanNumber, String customerNumber, String type) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("loanNumber", loanNumber));
        validation.addIfPresent(Validators.required("customerNumber", customerNumber));
        validation.addIfPresent(Validators.required("type", type));
        if (type != null && !type.isBlank()
                && !AddCoborrower.TYPE_COBORROWER.equals(type)
                && !AddCoborrower.TYPE_GUARANTOR.equals(type)) {
            validation.add(new FieldError("type",
                    "must be '" + AddCoborrower.TYPE_COBORROWER + "' or '" + AddCoborrower.TYPE_GUARANTOR + "'"));
        }
        return validation;
    }

    private Status sendAdd(String loanNumber, String customerNumber, String type) throws IOException {
        return new AddCoborrower(customerNumber, loanNumber, type).send(_connection);
    }

    private Status sendDelete(long id, String loanNumber) throws IOException {
        return new DeleteCoborrower(id, loanNumber).send(_connection);
    }
}
