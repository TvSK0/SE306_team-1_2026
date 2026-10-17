// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
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
// TEMPORARY: swap direct Connection use for Scott's gateway (and Aum's sync queue for
// offline saves) once they exist.
public class LoanService {
    public static final String MAILING_ADDRESS = "mailingAddress";
    public static final String CONTACT_DETAILS = "contactDetails";
    public static final String CUSTOMER_NUMBER = "customerNumber";
    public static final String LOAN_NUMBER = "loanNumber";

    // a non-negative number with any number of decimal places, e.g. "6.29" or "5.875"
    private static final Pattern RATE = Pattern.compile("^\\d+(\\.\\d+)?$");

    private final Connection _connection;

    public LoanService(Connection connection) {
        _connection = connection;
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
    // LoanDetailsInput constants.
    public ValidationResult validate(LoanDetailsInput input) {
        ValidationResult result = new ValidationResult();
        result.addIfPresent(firstOf(
                Validators.required(LoanDetailsInput.PRINCIPAL, input.principal()),
                Validators.currency(LoanDetailsInput.PRINCIPAL, input.principal()),
                moreThanZero(LoanDetailsInput.PRINCIPAL, input.principal())));
        result.addIfPresent(Validators.required(LoanDetailsInput.RATE_TYPE, input.rateType()));
        result.addIfPresent(firstOf(
                Validators.required(LoanDetailsInput.RATE_AMOUNT, input.rateAmount()),
                rate(LoanDetailsInput.RATE_AMOUNT, input.rateAmount())));
        result.addIfPresent(Validators.required(LoanDetailsInput.RATE_FREQUENCY, input.rateFrequency()));
        result.addIfPresent(firstOf(
                Validators.required(LoanDetailsInput.PAYMENT_AMOUNT, input.paymentAmount()),
                Validators.currency(LoanDetailsInput.PAYMENT_AMOUNT, input.paymentAmount())));
        result.addIfPresent(Validators.required(LoanDetailsInput.PAYMENT_FREQUENCY, input.paymentFrequency()));
        result.addIfPresent(firstOf(
                Validators.required(LoanDetailsInput.START_DATE, input.startDate()),
                Validators.date(LoanDetailsInput.START_DATE, input.startDate())));
        result.addIfPresent(firstOf(
                Validators.date(LoanDetailsInput.RENEWAL_DATE, input.renewalDate()),
                renewalNotBeforeStart(input.startDate(), input.renewalDate())));
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

        var message = new mainframe.client.messages.loan.Create(
                customerNumber,
                Double.parseDouble(input.paymentAmount()), input.paymentFrequency(),
                Double.parseDouble(input.principal()),
                Double.parseDouble(input.rateAmount()), input.rateFrequency(), input.rateType(),
                blankToNull(input.renewalDate()), input.startDate());
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

    // Saves changes to an existing loan with loan.Update.
    public LoanSaveResult updateLoan(String customerNumber, String loanNumber, LoanDetailsInput input) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        validation.addIfPresent(Validators.required(LOAN_NUMBER, loanNumber));
        validation.merge(validate(input));
        if (!validation.isValid()) {
            return LoanSaveResult.invalid(validation);
        }

        var message = new mainframe.client.messages.loan.Update(
                customerNumber, loanNumber,
                Double.parseDouble(input.paymentAmount()), input.paymentFrequency(),
                Double.parseDouble(input.principal()),
                Double.parseDouble(input.rateAmount()), input.rateFrequency(), input.rateType(),
                blankToNull(input.renewalDate()), input.startDate());
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

    // Builds the model from the values the mainframe sent back. loan.Create and
    // loan.Update share no type, so both pass their getters in here.
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

    // the first error from a field's checks, so each field reports one problem at a time
    @SafeVarargs
    private static Optional<FieldError> firstOf(Optional<FieldError>... checks) {
        for (Optional<FieldError> check : checks) {
            if (check.isPresent()) return check;
        }
        return Optional.empty();
    }

    // skips values the other checks already reject, so it only judges real numbers
    private static Optional<FieldError> moreThanZero(String fieldName, String value) {
        if (value == null || !RATE.matcher(value).matches()) return Optional.empty();
        if (new BigDecimal(value).signum() > 0) return Optional.empty();
        return Optional.of(new FieldError(fieldName, "must be more than 0"));
    }

    // a blank value is left to Validators.required, which firstOf reports first
    private static Optional<FieldError> rate(String fieldName, String value) {
        if (value == null || RATE.matcher(value).matches()) return Optional.empty();
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
