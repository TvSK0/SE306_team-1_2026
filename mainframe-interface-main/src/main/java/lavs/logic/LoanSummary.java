package lavs.logic;

import java.math.BigDecimal;
import java.time.LocalDate;

// US7: the totals the mainframe works out for a loan (loanSummary.Retrieve). LAVS fetches
// these when they are shown instead of storing them, so they follow every edit.
public final class LoanSummary {
    private final BigDecimal _totalAmountPaid;
    private final BigDecimal _totalInterestPaid;
    private final long _numberOfPayments;
    private final LocalDate _finalPaymentDate;
    private final BigDecimal _finalPaymentAmount;

    public LoanSummary(BigDecimal totalAmountPaid, BigDecimal totalInterestPaid, long numberOfPayments,
                       LocalDate finalPaymentDate, BigDecimal finalPaymentAmount) {
        _totalAmountPaid = totalAmountPaid;
        _totalInterestPaid = totalInterestPaid;
        _numberOfPayments = numberOfPayments;
        _finalPaymentDate = finalPaymentDate;
        _finalPaymentAmount = finalPaymentAmount;
    }

    public BigDecimal getTotalAmountPaid() {
        return _totalAmountPaid;
    }

    public BigDecimal getTotalInterestPaid() {
        return _totalInterestPaid;
    }

    public long getNumberOfPayments() {
        return _numberOfPayments;
    }

    public LocalDate getFinalPaymentDate() {
        return _finalPaymentDate;
    }

    public BigDecimal getFinalPaymentAmount() {
        return _finalPaymentAmount;
    }
}
