package lavs.ui;

import lavs.model.Customer;
import lavs.model.Loan;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoanScreenRequestTests {
    @Test
    public void newLoanRequestHasTheCustomerAndNoLoan() {
        // Arrange
        Customer customer = new Customer();

        // Act
        LoanScreenRequest request = LoanScreenRequest.forNewLoan(customer);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(customer, request.customer()),
                () -> Assertions.assertNull(request.loan())
        );
    }

    @Test
    public void existingLoanRequestHasTheCustomerAndTheLoan() {
        // Arrange
        Customer customer = new Customer();
        Loan loan = new Loan();

        // Act
        LoanScreenRequest request = LoanScreenRequest.forLoan(customer, loan);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(customer, request.customer()),
                () -> Assertions.assertSame(loan, request.loan())
        );
    }

    @Test
    public void requestWithoutACustomerIsRejected() {
        // Act + Assert
        Assertions.assertThrows(NullPointerException.class, () -> LoanScreenRequest.forNewLoan(null));
    }

    @Test
    public void existingLoanRequestWithoutALoanIsRejected() {
        // Act + Assert
        Assertions.assertThrows(NullPointerException.class,
                () -> LoanScreenRequest.forLoan(new Customer(), null));
    }

    @Test
    public void fromReturnsTheRequestItWasGiven() {
        // Arrange
        LoanScreenRequest request = LoanScreenRequest.forNewLoan(new Customer());

        // Act
        LoanScreenRequest result = LoanScreenRequest.from(request);

        // Assert
        Assertions.assertSame(request, result);
    }

    @Test
    public void fromRejectsAParameterThatIsNotARequest() {
        // Act
        IllegalArgumentException error = Assertions.assertThrows(IllegalArgumentException.class,
                () -> LoanScreenRequest.from("C-10234"));

        // Assert
        Assertions.assertTrue(error.getMessage().contains("LoanScreenRequest"));
    }

    @Test
    public void fromRejectsNoParameter() {
        // Act + Assert
        Assertions.assertThrows(IllegalArgumentException.class, () -> LoanScreenRequest.from(null));
    }
}
