// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Loan;

import java.math.BigDecimal;
import java.time.LocalDate;

// The loan details exactly as typed on the Loan Details screen. Everything stays a
// String so that LoanService can report a bad field without the screen losing what the
// MMM entered. Dates are yyyy-MM-dd, the format the mainframe uses.
public class LoanDetailsInput {
    // The field names LoanService uses in its FieldErrors, so a screen can highlight
    // the matching control.
    public static final String PRINCIPAL = "principal";
    public static final String RATE_TYPE = "rateType";
    public static final String RATE_AMOUNT = "rateAmount";
    public static final String RATE_FREQUENCY = "rateFrequency";
    public static final String PAYMENT_AMOUNT = "paymentAmount";
    public static final String PAYMENT_FREQUENCY = "paymentFrequency";
    public static final String START_DATE = "startDate";
    public static final String RENEWAL_DATE = "renewalDate";

    private final String _principal;
    private final String _rateType;
    private final String _rateAmount;
    private final String _rateFrequency;
    private final String _paymentAmount;
    private final String _paymentFrequency;
    private final String _startDate;
    private final String _renewalDate;

    public LoanDetailsInput(String principal, String rateType, String rateAmount, String rateFrequency,
                            String paymentAmount, String paymentFrequency, String startDate, String renewalDate) {
        _principal = principal;
        _rateType = rateType;
        _rateAmount = rateAmount;
        _rateFrequency = rateFrequency;
        _paymentAmount = paymentAmount;
        _paymentFrequency = paymentFrequency;
        _startDate = startDate;
        _renewalDate = renewalDate;
    }

    // Fills the form from a saved loan, for editing.
    // Missing numbers and dates become empty text; missing dropdown values stay null
    // so nothing is selected.
    public static LoanDetailsInput fromLoan(Loan loan) {
        return new LoanDetailsInput(
                text(loan.getPrincipal()),
                loan.getRateType(),
                text(loan.getRateAmount()),
                loan.getRateFrequency(),
                text(loan.getPaymentAmount()),
                loan.getPaymentFrequency(),
                text(loan.getStartDate()),
                text(loan.getRenewalDate()));
    }

    public String getPrincipal() {
        return _principal;
    }

    public String getRateType() {
        return _rateType;
    }

    public String getRateAmount() {
        return _rateAmount;
    }

    public String getRateFrequency() {
        return _rateFrequency;
    }

    public String getPaymentAmount() {
        return _paymentAmount;
    }

    public String getPaymentFrequency() {
        return _paymentFrequency;
    }

    public String getStartDate() {
        return _startDate;
    }

    public String getRenewalDate() {
        return _renewalDate;
    }

    private static String text(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static String text(LocalDate value) {
        return value == null ? "" : value.toString();
    }
}
