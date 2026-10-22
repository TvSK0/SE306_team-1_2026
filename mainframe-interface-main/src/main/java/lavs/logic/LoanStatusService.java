package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import lavs.data.MainframeGateway;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.messages.loan.Retrieve;
import mainframe.client.messages.loan.UpdateStatus;

import lavs.model.LoanCoborrower;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    // a co-borrower who needs updating; the message names them
    public static final String BORROWER = "borrower";

    static final long PAGE_SIZE = 50;
    // how customerAddress.list marks a mailing address
    private static final String MAILING = "y";

    private final MainframeGateway _gateway;
    private final LoanBorrowerService _borrowers;

    // AI-assisted: all application traffic uses the shared gateway.
    public LoanStatusService(MainframeGateway gateway) {
        _gateway = gateway;
        _borrowers = new LoanBorrowerService(gateway);
    }

    public LoanStatusService(Connection connection) {
        _gateway = new MainframeGateway(connection);
        _borrowers = new LoanBorrowerService(connection);
    }

    // AI-assisted: moves a New loan to Pending once its main borrower and every
    // co-borrower pass the shared eligibility check (a mailing address and a
    // phone number or email) and the guarantor rule holds. Every check runs before anything is
    // reported, so the MMM sees all the problems at once as field errors. A
    // lookup the mainframe refuses (or a network failure) is returned as its
    // Status, and the loan is left as it was.
    public LoanStatusResult submitForVerification(String loanNumber) throws IOException {
        LoanStatusResult checked = checkForVerification(loanNumber);
        if (!checked.isSuccessful()) return checked;
        UpdateStatus message = new UpdateStatus(loanNumber, PENDING);
        Status status = _gateway.send(message).getStatus();
        if (!status.getWasSuccessful()) return LoanStatusResult.fromStatus(status);
        return LoanStatusResult.changed(status, message.getStatus());
    }

    // Shared by initial submission and the retry worker; performs no writes.
    LoanStatusResult checkForVerification(String loanNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(LOAN_NUMBER, loanNumber));
        if (!validation.isValid()) {
            return LoanStatusResult.invalid(validation);
        }

        try {
            Retrieve loan = new Retrieve(loanNumber);
            lookUp(loan);
            if (!NEW.equals(loan.getStatus())) {
                validation.add(new FieldError(STATUS,
                        "only a New loan can be submitted for verification; this one is " + loan.getStatus()));
                return LoanStatusResult.invalid(validation);
            }
            checkEligibility(loan.getCustomerNumber(), validation);
            List<LoanCoborrower> others = listOthersOnTheLoan(loanNumber);
            checkCoborrowers(others, validation);
            checkGuarantorRule(loan.getCustomerNumber(), others, validation);
        } catch (LookupFailed e) {
            return LoanStatusResult.fromStatus(e.status);
        }
        if (!validation.isValid()) {
            return LoanStatusResult.invalid(validation);
        }

        return LoanStatusResult.fromStatus(new Status(0));
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

    // AI-assisted: everyone else on the loan (co-borrowers and guarantors), read once for both
    // the co-borrower check and the guarantor rule.
    private List<LoanCoborrower> listOthersOnTheLoan(String loanNumber) throws IOException, LookupFailed {
        LoanBorrowerListResult borrowers = _borrowers.listCoborrowers(loanNumber);
        if (!borrowers.isSuccessful()) {
            throw new LookupFailed(borrowers.getMainframeStatus());
        }
        return borrowers.getCoborrowers();
    }

    // AI-assisted: US9, every co-borrower also needs a mailing address and a phone number or
    // email. Each one missing something gets their own error naming them, so the MMM knows
    // whose record to update. Guarantors aren't borrowers, so they aren't checked.
    private void checkCoborrowers(List<LoanCoborrower> others, ValidationResult validation)
            throws IOException, LookupFailed {
        for (LoanCoborrower other : others) {
            if (!LoanBorrowerService.COBORROWER.equals(other.getType())) {
                continue;
            }
            List<String> missing = missingContactDetails(other.getCoborrowerNumber());
            if (!missing.isEmpty()) {
                validation.add(new FieldError(BORROWER,
                        "co-borrower " + describe(other) + " needs " + String.join(" and ", missing)));
            }
        }
    }

    // what a customer still needs before they can be on a submitted loan; empty when nothing
    private List<String> missingContactDetails(String customerNumber) throws IOException, LookupFailed {
        List<String> missing = new ArrayList<>();
        if (!hasMailingAddress(customerNumber)) {
            missing.add("a mailing address");
        }
        if (!hasPhoneNumber(customerNumber) && !hasEmail(customerNumber)) {
            missing.add("a phone number or email");
        }
        return missing;
    }

    // "Mere Brown (C-2)", or just the number when the mainframe gave no name
    private static String describe(LoanCoborrower borrower) {
        String name = borrower.getCoborrowerName();
        return name == null || name.isBlank()
                ? borrower.getCoborrowerNumber()
                : name.trim() + " (" + borrower.getCoborrowerNumber() + ")";
    }

    // A main borrower with an employer is fine; otherwise the loan needs a guarantor.
    private void checkGuarantorRule(String customerNumber, List<LoanCoborrower> others, ValidationResult validation)
            throws IOException, LookupFailed {
        if (hasEmployer(customerNumber)) {
            return;
        }
        boolean hasGuarantor = others.stream()
                .anyMatch(other -> LoanBorrowerService.GUARANTOR.equals(other.getType()));
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
        Status status = _gateway.send(message).getStatus();
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
