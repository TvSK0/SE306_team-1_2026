// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import java.math.BigDecimal;
import java.time.LocalDate;

// US7: one payment in a loan's repayment schedule (loanRepaymentSchedule.List).
public final class RepaymentRow {
    private final long _paymentNumber;
    private final LocalDate _date;
    private final BigDecimal _amountPaid;
    private final BigDecimal _principalAmount;
    private final BigDecimal _interestAmount;
    private final BigDecimal _amountRemaining;

    public RepaymentRow(long paymentNumber, LocalDate date, BigDecimal amountPaid,
                        BigDecimal principalAmount, BigDecimal interestAmount, BigDecimal amountRemaining) {
        _paymentNumber = paymentNumber;
        _date = date;
        _amountPaid = amountPaid;
        _principalAmount = principalAmount;
        _interestAmount = interestAmount;
        _amountRemaining = amountRemaining;
    }

    public long getPaymentNumber() {
        return _paymentNumber;
    }

    public LocalDate getDate() {
        return _date;
    }

    public BigDecimal getAmountPaid() {
        return _amountPaid;
    }

    public BigDecimal getPrincipalAmount() {
        return _principalAmount;
    }

    public BigDecimal getInterestAmount() {
        return _interestAmount;
    }

    public BigDecimal getAmountRemaining() {
        return _amountRemaining;
    }
}
