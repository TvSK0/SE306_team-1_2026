package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.loan.Retrieve;
import mainframe.client.messages.loan.UpdateStatus;

import java.io.IOException;

// TEMPORARY: swap direct Connection use for Scott's gateway once it exists.
//
// US9: change a loan's status to Pending ("submit for verification"), using
// loan.UpdateStatus. Depends on US8 (a loan's co-borrowers/guarantors are in
// place before it's submitted) and on the shared EligibilityChecker.
//
// Everything is looked up from the loan number, so a screen cannot get a
// check wrong by passing in the wrong customer or stale contact details:
// loan.Retrieve gives the main borrower and the current status, and the
// customer's addresses, phone numbers, emails and employers are read fresh.
//
// Guarantor rule (decided by the team, from the Data Schema): a main borrower
// with no employer needs a guarantor on the loan before it can become Pending.
// It is checked here rather than in EligibilityChecker, because US6 uses that
// shared rule to start a loan and a new loan has no guarantors yet.
//
// Not queued while offline: the checks need the mainframe, so a network
// failure is returned for the screen to report.
public final class LoanStatusService {
    // loan statuses, so the screen never has to import the mainframe message classes
    public static final String NEW = UpdateStatus.STATUS_NEW;
    public static final String PENDING = UpdateStatus.STATUS_PENDING;

    // the field names of the errors submitForVerification can return
    public static final String LOAN_NUMBER = "loanNumber";
    public static final String STATUS = "status";
    public static final String MAILING_ADDRESS = "mailingAddress";
    public static final String CONTACT_DETAILS = "contactDetails";
    public static final String GUARANTOR = "guarantor";

    static final long PAGE_SIZE = 50;
    // how customerAddress.list marks a mailing address
    private static final String MAILING = "y";

    private final Connection _connection;
    private final LoanBorrowerService _borrowers;
    private final SyncService _sync;

    // AI-assisted: the checks read the mainframe; the status change itself goes through the
    // upload queue (US11)
    public LoanStatusService(Connection connection, SyncService sync) {
        _connection = connection;
        _sync = sync;
        _borrowers = new LoanBorrowerService(connection, sync);
    }

    // AI-assisted: moves a New loan to Pending once its main borrower passes
    // the shared eligibility check (a mailing address and a phone number or
    // email) and the guarantor rule. Every check runs before anything is
    // reported, so the MMM sees all the problems at once as field errors. A
    // lookup the mainframe refuses (or a network failure) is returned as its
    // Status, and the loan is left as it was.
    public LoanStatusResult submitForVerification(String loanNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(LOAN_NUMBER, loanNumber));
        if (!validation.isValid()) {
            return LoanStatusResult.invalid(validation);
        }

        String customerNumber;
        try {
            Retrieve loan = new Retrieve(loanNumber);
            lookUp(loan);
            customerNumber = loan.getCustomerNumber();
            if (!NEW.equals(loan.getStatus())) {
                validation.add(new FieldError(STATUS,
                        "only a New loan can be submitted for verification; this one is " + loan.getStatus()));
                return LoanStatusResult.invalid(validation);
            }
            checkEligibility(loan.getCustomerNumber(), validation);
            checkGuarantorRule(loanNumber, loan.getCustomerNumber(), validation);
        } catch (LookupFailed e) {
            return LoanStatusResult.fromStatus(e.status);
        }
        if (!validation.isValid()) {
            return LoanStatusResult.invalid(validation);
        }

        // AI-assisted: the checks needed the mainframe, but the change itself is queued, so if the
        // connection drops now it is kept and sent later rather than lost
        UpdateStatus message = new UpdateStatus(loanNumber, PENDING);
        SyncStep step = QueuedWrites.queue(_sync, message, customerNumber, null);
        return LoanStatusResult.queued(step, PENDING);
    }

    // Adds a field error for each missing piece, so the screen can say exactly what to add.
    private void checkEligibility(String customerNumber, ValidationResult validation)
            throws IOException, LookupFailed {
        boolean hasMailingAddress = hasMailingAddress(customerNumber);
        boolean hasPhoneNumber = hasPhoneNumber(customerNumber);
        // either one is enough, so emails are only looked up when there is no phone number
        boolean hasEmail = !hasPhoneNumber && hasEmail(customerNumber);
        if (EligibilityChecker.isEligible(hasMailingAddress, hasPhoneNumber, hasEmail)) {
            return;
        }
        if (!hasMailingAddress) {
            validation.add(new FieldError(MAILING_ADDRESS, "the customer needs a mailing address"));
        }
        if (!hasPhoneNumber && !hasEmail) {
            validation.add(new FieldError(CONTACT_DETAILS, "the customer needs a phone number or email"));
        }
    }

    // A main borrower with an employer is fine; otherwise the loan needs a guarantor.
    // Employers are checked first, so a borrower with one costs no co-borrower lookup.
    private void checkGuarantorRule(String loanNumber, String customerNumber, ValidationResult validation)
            throws IOException, LookupFailed {
        if (hasEmployer(customerNumber)) {
            return;
        }
        LoanBorrowerListResult borrowers = _borrowers.listCoborrowers(loanNumber);
        if (!borrowers.isSuccessful()) {
            throw new LookupFailed(borrowers.getMainframeStatus());
        }
        boolean hasGuarantor = borrowers.getCoborrowers().stream()
                .anyMatch(coborrower -> LoanBorrowerService.GUARANTOR.equals(coborrower.getType()));
        if (!hasGuarantor) {
            validation.add(new FieldError(GUARANTOR, "the customer has no employer, so the loan needs a guarantor"));
        }
    }

    // A customer can have several addresses and only one needs to be for mailing, so this
    // reads pages 1, 2, ... until it finds one, runs out, or gets an empty page.
    private boolean hasMailingAddress(String customerNumber) throws IOException, LookupFailed {
        long seen = 0;
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            mainframe.client.messages.customerAddress.List message =
                    new mainframe.client.messages.customerAddress.List(customerNumber, page, PAGE_SIZE);
            lookUp(message);
            mainframe.client.messages.customerAddress.List.Item[] items = message.getItems();
            for (mainframe.client.messages.customerAddress.List.Item item : items) {
                if (MAILING.equals(item.getIsMailing())) {
                    return true;
                }
            }
            seen += items.length;
            if (items.length == 0 || seen >= message.getTotal()) {
                return false;
            }
            page++;
        }
    }

    // any one is enough, so a single one-item page answers each of these
    private boolean hasPhoneNumber(String customerNumber) throws IOException, LookupFailed {
        mainframe.client.messages.customerPhoneNumber.List message =
                new mainframe.client.messages.customerPhoneNumber.List(customerNumber, 1, 1);
        lookUp(message);
        return message.getItems().length > 0;
    }

    private boolean hasEmail(String customerNumber) throws IOException, LookupFailed {
        mainframe.client.messages.customerEmail.List message =
                new mainframe.client.messages.customerEmail.List(customerNumber, 1, 1);
        lookUp(message);
        return message.getItems().length > 0;
    }

    private boolean hasEmployer(String customerNumber) throws IOException, LookupFailed {
        mainframe.client.messages.customerEmployer.List message =
                new mainframe.client.messages.customerEmployer.List(customerNumber, 1, 1);
        lookUp(message);
        return message.getItems().length > 0;
    }

    private void lookUp(Message message) throws IOException, LookupFailed {
        Status status = message.send(_connection);
        if (!status.getWasSuccessful()) {
            throw new LookupFailed(status);
        }
    }

    // A lookup the mainframe refused, or a network failure: submitForVerification stops and
    // returns this status instead of guessing. Never leaves this class.
    private static final class LookupFailed extends Exception {
        private final transient Status status;

        LookupFailed(Status status) {
            super(null, null, false, false);
            this.status = status;
        }
    }
}
