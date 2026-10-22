// AI-assisted: tests for CustomerRecordService, one per customer record requirement.
package lavs.logic;

import lavs.model.Customer;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerRecordServiceTests {
    private static final String RETRIEVE = "customer.retrieve";

    // CR-R3
    @Test
    void loadsTheCustomerForTheHeader() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(RETRIEVE, Map.of("number", "C-10234"), FakeConnection.success(customer("1988-03-14")));
        CustomerRecordService service = new CustomerRecordService(connection);

        // Act
        CustomerRecordResult result = service.load("  C-10234 ");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(CustomerRecordResult.Outcome.SUCCESS, result.getOutcome());
        Customer customer = result.getCustomer();
        Assertions.assertEquals("C-10234", customer.getNumber());
        Assertions.assertEquals("Ms", customer.getTitle());
        Assertions.assertEquals("Jane Smith", customer.getFullName());
        Assertions.assertEquals(LocalDate.of(1988, 3, 14), customer.getDateOfBirth());
        Assertions.assertEquals("New Zealand", customer.getNationality());
        Assertions.assertEquals("Active", customer.getStatus());
        Assertions.assertNull(result.getMessage());
    }

    // CR-R3
    @Test
    void anUnknownDateOfBirthIsLeftOut() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(RETRIEVE, FakeConnection.success(customer(null)));

        // Act
        CustomerRecordResult result = new CustomerRecordService(connection).load("C-10234");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertNull(result.getCustomer().getDateOfBirth());
    }

    // CR-R3
    @Test
    void aBlankNumberSendsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act
        CustomerRecordResult result = new CustomerRecordService(connection).load("  ");

        // Assert
        Assertions.assertEquals(CustomerRecordResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertNull(result.getMessage());
        Assertions.assertNull(result.getCustomer());
        Assertions.assertEquals(List.of(CustomerRecordService.CUSTOMER_NUMBER),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // CR-R3
    @Test
    void anUnreachableMainframeIsANetworkFailure() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        CustomerRecordResult result = new CustomerRecordService(connection).load("C-10234");

        // Assert
        Assertions.assertEquals(CustomerRecordResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        Assertions.assertEquals(FakeConnection.networkDown().getStatus().getErrorMessage(), result.getMessage());
    }

    // CR-R3
    @Test
    void reportsTheMainframesReasonWhenTheLoadIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(RETRIEVE, FakeConnection.rejected("No such customer"));

        // Act
        CustomerRecordResult result = new CustomerRecordService(connection).load("C-9");

        // Assert
        Assertions.assertEquals(CustomerRecordResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("No such customer", result.getMessage());
        Assertions.assertNull(result.getCustomer());
    }

    private static Map<String, String> customer(String dateOfBirth) {
        Map<String, String> values = new HashMap<>();
        values.put("number", "C-10234");
        values.put("title", "Ms");
        values.put("firstName", "Jane");
        values.put("familyName", "Smith");
        values.put("nationality", "New Zealand");
        values.put("status", "Active");
        if (dateOfBirth != null) {
            values.put("dateOfBirth", dateOfBirth);
        }
        return values;
    }
}
