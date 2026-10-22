package lavs.ui;

import lavs.model.Customer;
import lavs.model.Loan;

import java.util.Objects;

// What a screen passes to navigator.goTo(Screen.LOAN, ...): the customer the loan belongs
// to, and the loan to edit, or null to start a new one.
public record LoanScreenRequest(Customer customer, Loan loan) {
    public LoanScreenRequest {
        Objects.requireNonNull(customer, "a loan screen needs the customer the loan belongs to");
    }

    public static LoanScreenRequest forNewLoan(Customer customer) {
        return new LoanScreenRequest(customer, null);
    }

    public static LoanScreenRequest forLoan(Customer customer, Loan loan) {
        Objects.requireNonNull(loan, "use forNewLoan to start a new loan");
        return new LoanScreenRequest(customer, loan);
    }

    // The navigation parameter as a request. Anything else is a programming mistake by the
    // screen that opened the loan, so it fails loudly and the Navigator stays where it is.
    public static LoanScreenRequest from(Object parameter) {
        if (parameter instanceof LoanScreenRequest request) {
            return request;
        }
        throw new IllegalArgumentException("Screen.LOAN needs a LoanScreenRequest, got: " + parameter);
    }
}
