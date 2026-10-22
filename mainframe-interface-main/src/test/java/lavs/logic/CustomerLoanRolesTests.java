// AI-assisted: tests for the customer's role on each loan (the Loans tab's Role column).
package lavs.logic;

import lavs.data.MainframeGateway;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerLoanRolesTests {
    // LT-R5
    @Test
    void theOwnerOfALoanIsItsMainBorrowerWithNoSecondCall() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", FakeConnection.success(loan("C-1")));

        // Act
        ServiceResult<String> role = service(connection).role("C-1", "L-1");

        // Assert
        Assertions.assertEquals(CustomerLoansService.MAIN_BORROWER, role.getValue());
        Assertions.assertEquals(1, connection.getSentRequests().size());
    }

    // LT-R5
    @Test
    void aCoBorrowerOrGuarantorIsFoundOnTheLoansBorrowerList() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", FakeConnection.success(loan("C-9")));
        connection.whenRequested("loanCoborrower.list", Map.of("number", "L-1"),
                FakeConnection.success(borrowers("C-5", "Guarantor", "C-1", "Coborrower")));
        connection.whenRequested("loanCoborrower.list", Map.of("number", "L-2"),
                FakeConnection.success(borrowers("C-1", "Guarantor")));
        CustomerLoansService service = service(connection);

        // Act and Assert
        Assertions.assertEquals(CustomerLoansService.CO_BORROWER, service.role("C-1", "L-1").getValue());
        Assertions.assertEquals(CustomerLoansService.GUARANTOR, service.role("C-1", "L-2").getValue());
    }

    // LT-R5
    @Test
    void anyOtherTypeIsKeptAsSentAndACustomerNotOnTheLoanHasNoRole() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", FakeConnection.success(loan("C-9")));
        connection.whenRequested("loanCoborrower.list", Map.of("number", "L-1"),
                FakeConnection.success(borrowers("C-1", "Witness")));
        connection.whenRequested("loanCoborrower.list", Map.of("number", "L-2"),
                FakeConnection.success(borrowers("C-7", "Coborrower")));
        CustomerLoansService service = service(connection);

        // Act
        ServiceResult<String> other = service.role("C-1", "L-1");
        ServiceResult<String> none = service.role("C-1", "L-2");

        // Assert
        Assertions.assertEquals("Witness", other.getValue());
        Assertions.assertTrue(none.isSuccessful());
        Assertions.assertNull(none.getValue());
    }

    // LT-R5
    @Test
    void failuresAndBlankInputGiveNoRole() {
        // Arrange
        FakeConnection offline = new FakeConnection();
        offline.setIsConnected(false);
        FakeConnection refusedBorrowers = new FakeConnection();
        refusedBorrowers.whenRequested("loan.retrieve", FakeConnection.success(loan("C-9")));
        refusedBorrowers.whenRequested("loanCoborrower.list", FakeConnection.rejected("No such loan"));

        // Act and Assert
        Assertions.assertFalse(service(new FakeConnection()).role("", "L-1").getValidationErrors().isValid());
        Assertions.assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, service(offline).role("C-1", "L-1").getOutcome());
        Assertions.assertEquals(ServiceResult.Outcome.REJECTED,
                service(refusedBorrowers).role("C-1", "L-1").getOutcome());
    }

    // LT-R5 (roles for the whole table; a loan whose role couldn't be read is left out)
    @Test
    void rolesMapsEachLoanAndLeavesOutOnesThatFailedOrHaveNoRole() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", Map.of("number", "L-1"), FakeConnection.success(loan("C-1")));
        connection.whenRequested("loan.retrieve", Map.of("number", "L-2"), FakeConnection.rejected("No such loan"));
        connection.whenRequested("loan.retrieve", Map.of("number", "L-3"), FakeConnection.success(loan("C-9")));
        connection.whenRequested("loanCoborrower.list", FakeConnection.success(borrowers("C-7", "Guarantor")));

        // Act
        Map<String, String> roles = service(connection).roles("C-1", List.of("L-1", "L-2", "L-3"));

        // Assert
        Assertions.assertEquals(Map.of("L-1", CustomerLoansService.MAIN_BORROWER), roles);
    }

    private static CustomerLoansService service(FakeConnection connection) {
        return new CustomerLoansService(new MainframeGateway(connection));
    }

    // a loan.retrieve reply; the starter message needs the three amounts, as a real loan has them
    private static Map<String, String> loan(String customerNumber) {
        Map<String, String> values = new HashMap<>();
        values.put("customerNumber", customerNumber);
        values.put("principal", "1.0");
        values.put("rateAmount", "1.0");
        values.put("paymentAmount", "1.0");
        return values;
    }

    // pairs of (customer number, type)
    private static Map<String, String> borrowers(String... pairs) {
        Map<String, String> values = new HashMap<>();
        int count = pairs.length / 2;
        values.put("total", Integer.toString(count));
        values.put("items", Integer.toString(count));
        for (int i = 0; i < count; i++) {
            values.put((i + 1) + ":id", Integer.toString(i + 1));
            values.put((i + 1) + ":coborrowerNumber", pairs[2 * i]);
            values.put((i + 1) + ":type", pairs[2 * i + 1]);
        }
        return values;
    }
}
