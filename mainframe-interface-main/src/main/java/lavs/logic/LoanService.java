package lavs.logic;

import lavs.model.Loan;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Status;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

// US6: start, create and edit a loan.
// AI-assisted: opening a loan reads the mainframe; creating and editing go through the upload
// queue (SyncService), so they work offline (US11). A loan created offline gets a temporary
// number, which the queue swaps for the real one in later changes once it uploads.
// TEMPORARY: the read still uses the Connection directly rather than the MainframeGateway.
public class LoanService {
    public static final String MAILING_ADDRESS = "mailingAddress";
    public static final String CONTACT_DETAILS = "contactDetails";
    public static final String CUSTOMER_NUMBER = "customerNumber";
    public static final String LOAN_NUMBER = "loanNumber";

    // a non-negative number with any number of decimal places, e.g. "6.29" or "5.875"
    private static final Pattern RATE = Pattern.compile("^\\d+(\\.\\d+)?$");

    private final Connection _connection;
    private final SyncService _sync;

    public LoanService(Connection connection, SyncService sync) {
        _connection = connection;
        _sync = sync;
    }

    // Whether the customer may start a loan: they need a mailing address and a phone
    // number or email. Each missing piece is its own FieldError so the screen can say
    // exactly what to add.
    public ValidationResult checkCanStartLoan(boolean hasMailingAddress, boolean hasPhoneNumber, boolean hasEmail) {
        ValidationResult result = new ValidationResult();
        if (EligibilityChecker.isEligible(hasMailingAddress, hasPhoneNumber, hasEmail)) {
            return result;
        }
        if (!hasMailingAddress) {
            result.add(new FieldError(MAILING_ADDRESS, "the customer needs a mailing address"));
        }
        if (!hasPhoneNumber && !hasEmail) {
            result.add(new FieldError(CONTACT_DETAILS, "the customer needs a phone number or email"));
        }
        return result;
    }

    // Checks every field and returns one FieldError per bad field, named with the
    // LoanDetailsInput constants. Each field's checks run in order and stop at the
    // first problem, so a field never reports two errors at once.
    public ValidationResult validate(LoanDetailsInput input) {
        ValidationResult result = new ValidationResult();

        // principal: filled in, a money amount, and more than 0
        Optional<FieldError> principalError = Validators.required(LoanDetailsInput.PRINCIPAL, input.getPrincipal());
        if (principalError.isEmpty()) {
            principalError = Validators.currency(LoanDetailsInput.PRINCIPAL, input.getPrincipal());
        }
        if (principalError.isEmpty()) {
            principalError = moreThanZero(LoanDetailsInput.PRINCIPAL, input.getPrincipal());
        }
        result.addIfPresent(principalError);

        result.addIfPresent(Validators.required(LoanDetailsInput.RATE_TYPE, input.getRateType()));

        // rate amount: filled in and a number
        Optional<FieldError> rateError = Validators.required(LoanDetailsInput.RATE_AMOUNT, input.getRateAmount());
        if (rateError.isEmpty()) {
            rateError = rate(LoanDetailsInput.RATE_AMOUNT, input.getRateAmount());
        }
        result.addIfPresent(rateError);

        result.addIfPresent(Validators.required(LoanDetailsInput.RATE_FREQUENCY, input.getRateFrequency()));

        // payment amount: filled in and a money amount
        Optional<FieldError> paymentError = Validators.required(LoanDetailsInput.PAYMENT_AMOUNT, input.getPaymentAmount());
        if (paymentError.isEmpty()) {
            paymentError = Validators.currency(LoanDetailsInput.PAYMENT_AMOUNT, input.getPaymentAmount());
        }
        result.addIfPresent(paymentError);

        result.addIfPresent(Validators.required(LoanDetailsInput.PAYMENT_FREQUENCY, input.getPaymentFrequency()));

        // start date: filled in and a real date
        Optional<FieldError> startError = Validators.required(LoanDetailsInput.START_DATE, input.getStartDate());
        if (startError.isEmpty()) {
            startError = Validators.date(LoanDetailsInput.START_DATE, input.getStartDate());
        }
        result.addIfPresent(startError);

        // renewal date: optional, but a real date and not before the start date
        Optional<FieldError> renewalError = Validators.date(LoanDetailsInput.RENEWAL_DATE, input.getRenewalDate());
        if (renewalError.isEmpty()) {
            renewalError = renewalNotBeforeStart(input.getStartDate(), input.getRenewalDate());
        }
        result.addIfPresent(renewalError);

        return result;
    }

    // Creates a new loan for the customer with loan.Create. The mainframe gives it a
    // loan number and the status New.
    public LoanSaveResult createLoan(String customerNumber, LoanDetailsInput input) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        validation.merge(validate(input));
        if (!validation.isValid()) {
            return LoanSaveResult.invalid(validation);
        }

        mainframe.client.messages.loan.Create message = new mainframe.client.messages.loan.Create(
                customerNumber,
                Double.parseDouble(input.getPaymentAmount()), input.getPaymentFrequency(),
                Double.parseDouble(input.getPrincipal()),
                Double.parseDouble(input.getRateAmount()), input.getRateFrequency(), input.getRateType(),
                blankToNull(input.getRenewalDate()), input.getStartDate());
        // AI-assisted: queued under a temporary number. Sent straight away it comes back with the
        // number the mainframe gave it; still waiting, it keeps the temporary one until it uploads.
        String temporaryNumber = TemporaryNumbers.create();
        SyncStep step = QueuedWrites.queue(_sync, message, customerNumber, temporaryNumber);
        String loanNumber = step.getOutcome() == SyncStep.Outcome.SENT ? step.getMessage() : temporaryNumber;
        return LoanSaveResult.queued(step, toLoan(
                loanNumber, customerNumber, LoanStatusService.NEW,
                message.getPrincipal(), message.getRateType(), message.getRateAmount(), message.getRateFrequency(),
                message.getPaymentAmount(), message.getPaymentFrequency(),
                message.getStartDate(), message.getRenewalDate()));
    }

    // Saves changes to an existing loan with loan.Update.
    public LoanSaveResult updateLoan(String customerNumber, String loanNumber, LoanDetailsInput input) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        validation.addIfPresent(Validators.required(LOAN_NUMBER, loanNumber));
        validation.merge(validate(input));
        if (!validation.isValid()) {
            return LoanSaveResult.invalid(validation);
        }

        mainframe.client.messages.loan.Update message = new mainframe.client.messages.loan.Update(
                customerNumber, loanNumber,
                Double.parseDouble(input.getPaymentAmount()), input.getPaymentFrequency(),
                Double.parseDouble(input.getPrincipal()),
                Double.parseDouble(input.getRateAmount()), input.getRateFrequency(), input.getRateType(),
                blankToNull(input.getRenewalDate()), input.getStartDate());
        // AI-assisted: queued, so the change works offline. The loan's status isn't part of the
        // change and the mainframe's answer doesn't come back through the queue, so it is left
        // empty for the screen to keep the one it shows.
        SyncStep step = QueuedWrites.queue(_sync, message, customerNumber, null);
        return LoanSaveResult.queued(step, toLoan(
                loanNumber, customerNumber, null,
                message.getPrincipal(), message.getRateType(), message.getRateAmount(), message.getRateFrequency(),
                message.getPaymentAmount(), message.getPaymentFrequency(),
                message.getStartDate(), message.getRenewalDate()));
    }

    // Loads a saved loan by its number with loan.Retrieve, e.g. to open it from the
    // customer record for editing.
    public LoanSaveResult loadLoan(String loanNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(LOAN_NUMBER, loanNumber));
        if (!validation.isValid()) {
            return LoanSaveResult.invalid(validation);
        }

        mainframe.client.messages.loan.Retrieve message = new mainframe.client.messages.loan.Retrieve(loanNumber);
        Status status = message.send(_connection);
        if (!status.getWasSuccessful()) {
            return LoanSaveResult.failed(status);
        }
        return LoanSaveResult.saved(status, toLoan(
                message.getLoanNumber(), message.getCustomerNumber(), message.getStatus(),
                message.getPrincipal(), message.getRateType(), message.getRateAmount(), message.getRateFrequency(),
                message.getPaymentAmount(), message.getPaymentFrequency(),
                message.getStartDate(), message.getRenewalDate()));
    }

    // Builds the model from the values the mainframe sent back. loan.Create, loan.Update
    // and loan.Retrieve share no type, so each passes its getters in here.
    private static Loan toLoan(String loanNumber, String customerNumber, String status,
                               Double principal, String rateType, Double rateAmount, String rateFrequency,
                               Double paymentAmount, String paymentFrequency,
                               String startDate, String renewalDate) {
        Loan loan = new Loan(customerNumber, toDecimal(principal), rateType, toDecimal(rateAmount), rateFrequency,
                toDecimal(paymentAmount), paymentFrequency, toDate(startDate), toDate(renewalDate));
        loan.setLoanNumber(loanNumber);
        loan.setStatus(status);
        return loan;
    }

    // only called once Validators.currency has passed, so value is a valid number
    private static Optional<FieldError> moreThanZero(String fieldName, String value) {
        if (new BigDecimal(value).signum() > 0) return Optional.empty();
        return Optional.of(new FieldError(fieldName, "must be more than 0"));
    }

    // only called once Validators.required has passed, so value is not blank
    private static Optional<FieldError> rate(String fieldName, String value) {
        if (RATE.matcher(value).matches()) return Optional.empty();
        return Optional.of(new FieldError(fieldName, "must be a non-negative number, e.g. 6.29"));
    }

    // only judged once both dates are valid; the date checks report anything else
    private static Optional<FieldError> renewalNotBeforeStart(String startDate, String renewalDate) {
        LocalDate start = parseOrNull(startDate);
        LocalDate renewal = parseOrNull(renewalDate);
        if (start == null || renewal == null || !renewal.isBefore(start)) return Optional.empty();
        return Optional.of(new FieldError(LoanDetailsInput.RENEWAL_DATE, "must not be before the start date"));
    }

    private static LocalDate parseOrNull(String value) {
        try {
            return value == null ? null : LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    // the message getters unbox a double, so a successful reply never gives null here
    private static BigDecimal toDecimal(Double value) {
        return BigDecimal.valueOf(value);
    }

    private static LocalDate toDate(String value) {
        String text = blankToNull(value);
        return text == null ? null : LocalDate.parse(text);
    }
}
