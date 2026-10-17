// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Loan;

import java.math.BigDecimal;
import java.time.LocalDate;

// The loan details exactly as typed on the Loan Details screen. Everything stays a
// String so that LoanService can report a bad field without the screen losing what the
// MMM entered. Dates are yyyy-MM-dd, the format the mainframe uses.
public record LoanDetailsInput(
        String principal,
        String rateType,
        String rateAmount,
        String rateFrequency,
        String paymentAmount,
        String paymentFrequency,
        String startDate,
        String renewalDate) {

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

    private static String text(BigDecimal value) {
        return value == null ? "" : value.toPlainString();
    }

    private static String text(LocalDate value) {
        return value == null ? "" : value.toString();
    }
}
