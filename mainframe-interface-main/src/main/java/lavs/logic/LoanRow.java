// AI-assisted: one line of a customer's Loans table: the loan and the customer's part in it.
package lavs.logic;

import java.math.BigDecimal;
import java.time.LocalDate;

// role is "Main borrower", "Co-borrower" or "Guarantor" (see CustomerLoansService), or null if
// the customer is not on the loan's borrower list. principal and startDate are null when the
// mainframe sent none (or something unreadable).
public record LoanRow(String loanNumber, String role, String status, BigDecimal principal, LocalDate startDate) {
}
