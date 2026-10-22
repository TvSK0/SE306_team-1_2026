// AI-assisted: lists a customer's loans with their role on each, and reads what "+ New loan" needs.
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Loan;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// US7 (the Loans tab of the customer record) and US6 (starting a loan). Reading needs a
// connection, so a network failure is returned for the screen to report; nothing is queued.
// TEMPORARY: uses Connection directly until Scott's gateway lands; swap then.
public class CustomerLoansService {
    public static final String CUSTOMER_NUMBER = "customerNumber";
    public static final String MAIN_BORROWER = "Main borrower";
    public static final String CO_BORROWER = "Co-borrower";
    public static final String GUARANTOR = "Guarantor";

    static final long PAGE_SIZE = 50;
    // how customerAddress.list marks a mailing address (the same as LoanStatusService reads it)
    private static final String MAILING = "y";

    private final Connection _connection;
    private final LoanBorrowerService _borrowers;
    private final LoanService _loans;

    public CustomerLoansService(Connection connection) {
        _connection = connection;
        _borrowers = new LoanBorrowerService(connection);
        _loans = new LoanService(connection);
    }

    // The saved loan for "Open loan", read with LoanService.loadLoan so the loan screen gets
    // exactly what it would get anywhere else.
    public LookupResult<Loan> openLoan(String loanNumber) {
        LoanSaveResult result;
        try {
            result = _loans.loadLoan(loanNumber);
        } catch (IOException ex) {
            return LookupResult.networkFailure(ex.getMessage());
        } catch (RuntimeException ex) {
            return LookupResult.rejected(UNREADABLE);
        }
        if (result.isSuccessful()) {
            return LookupResult.success(result.getLoan());
        }
        if (!result.getValidationErrors().isValid()) {
            return LookupResult.invalid(result.getValidationErrors());
        }
        return Failure.from(result.getMainframeStatus()).toResult();
    }

    // Every loan loan.List returns for the customer, with the customer's role on each. We do not
    // rely on which loans loan.List includes: each loan is retrieved to see its main borrower,
    // and only a loan the customer is not the main borrower of costs a second call, to find them
    // among its co-borrowers and guarantors. Everything runs in the caller's background task.
    public LookupResult<List<LoanRow>> listLoans(String customerNumber) {
        LookupResult<List<LoanRow>> invalid = invalid(customerNumber);
        if (invalid != null) {
            return invalid;
        }
        String number = customerNumber.trim();
        try {
            List<LoanRow> rows = new ArrayList<>();
            long page = 1;
            while (true) {
                // a new message per page, because message objects keep the items from earlier sends
                mainframe.client.messages.loan.List message =
                        new mainframe.client.messages.loan.List(number, page, PAGE_SIZE);
                send(message);
                mainframe.client.messages.loan.List.Item[] items = message.getItems();
                for (mainframe.client.messages.loan.List.Item item : items) {
                    rows.add(new LoanRow(item.getLoanNumber(), roleOn(number, item.getLoanNumber()),
                            item.getStatus(), parseMoney(item.getPrincipal()), parseDate(item.getStartDate())));
                }
                // also stops on an empty page, so a wrong total cannot cause an endless loop
                if (items.length == 0 || rows.size() >= message.getTotal()) {
                    return LookupResult.success(rows);
                }
                page++;
            }
        } catch (Failure failure) {
            return failure.toResult();
        }
    }

    // Whether the customer has a mailing address, a phone number and an email, for
    // LoanService.checkCanStartLoan. The phone and email lists only need their totals.
    public LookupResult<StartLoanFacts> startLoanFacts(String customerNumber) {
        LookupResult<StartLoanFacts> invalid = invalid(customerNumber);
        if (invalid != null) {
            return invalid;
        }
        String number = customerNumber.trim();
        try {
            boolean mailing = hasMailingAddress(number);
            var phones = new mainframe.client.messages.customerPhoneNumber.List(number, 1, 1);
            send(phones);
            var emails = new mainframe.client.messages.customerEmail.List(number, 1, 1);
            send(emails);
            return LookupResult.success(new StartLoanFacts(mailing, phones.getTotal() > 0, emails.getTotal() > 0));
        } catch (Failure failure) {
            return failure.toResult();
        }
    }

    private boolean hasMailingAddress(String number) throws Failure {
        long seen = 0;
        long page = 1;
        while (true) {
            mainframe.client.messages.customerAddress.List message =
                    new mainframe.client.messages.customerAddress.List(number, page, PAGE_SIZE);
            send(message);
            mainframe.client.messages.customerAddress.List.Item[] items = message.getItems();
            for (mainframe.client.messages.customerAddress.List.Item item : items) {
                if (MAILING.equalsIgnoreCase(item.getIsMailing())) {
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

    private String roleOn(String customerNumber, String loanNumber) throws Failure {
        var loan = new mainframe.client.messages.loan.Retrieve(loanNumber);
        send(loan);
        if (customerNumber.equals(loan.getCustomerNumber())) {
            return MAIN_BORROWER;
        }
        LoanBorrowerListResult borrowers;
        try {
            borrowers = _borrowers.listCoborrowers(loanNumber);
        } catch (IOException ex) {
            throw new Failure(true, ex.getMessage());
        } catch (RuntimeException ex) {
            throw new Failure(false, UNREADABLE);
        }
        if (!borrowers.isSuccessful()) {
            throw Failure.from(borrowers.getMainframeStatus());
        }
        return borrowers.getCoborrowers().stream()
                .filter(borrower -> customerNumber.equals(borrower.getCoborrowerNumber()))
                .map(borrower -> roleName(borrower.getType()))
                .findFirst().orElse(null);
    }

    // "Coborrower" is the mainframe's spelling; the screen says Co-borrower
    private static String roleName(String type) {
        if (LoanBorrowerService.COBORROWER.equalsIgnoreCase(type)) {
            return CO_BORROWER;
        }
        if (LoanBorrowerService.GUARANTOR.equalsIgnoreCase(type)) {
            return GUARANTOR;
        }
        return type;
    }

    private static final String UNREADABLE = "The mainframe sent a reply that could not be read.";

    private static <T> LookupResult<T> invalid(String customerNumber) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        return validation.isValid() ? null : LookupResult.invalid(validation);
    }

    // Sends the message, turning anything but success into a Failure. RuntimeException: the
    // starter list messages throw a NullPointerException when an item has no id, and they are
    // not ours to change.
    private void send(Message message) throws Failure {
        Status status;
        try {
            status = message.send(_connection);
        } catch (IOException ex) {
            throw new Failure(true, ex.getMessage());
        } catch (RuntimeException ex) {
            throw new Failure(false, UNREADABLE);
        }
        if (!status.getWasSuccessful()) {
            throw Failure.from(status);
        }
    }

    // the mainframe sends a plain number such as 450000.0, or nothing
    private static BigDecimal parseMoney(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    // Used only inside this class to stop at the first failure; the public methods turn it
    // back into a result, so callers never see it.
    private static final class Failure extends Exception {
        private final boolean _network;

        Failure(boolean network, String message) {
            super(message);
            _network = network;
        }

        static Failure from(Status status) {
            boolean network = StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode());
            return new Failure(network, status.getErrorMessage());
        }

        <T> LookupResult<T> toResult() {
            return _network ? LookupResult.networkFailure(getMessage()) : LookupResult.rejected(getMessage());
        }
    }
}
