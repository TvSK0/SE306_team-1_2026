package lavs.logic;

// AI-assisted.
import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployerServiceTests {
    private static final String CUSTOMER_NUMBER = "CUST-42";

    @Mock
    private Connection connection;

    private EmployerService service;

    @BeforeEach
    void setUp() {
        service = new EmployerService(connection);
    }

    // --- addEmployer ---

    @Test
    void addEmployerSendsTheRequestAndSucceeds() throws IOException {
        when(connection.send(argThat(requestOfType("customer.addEmployer"))))
                .thenReturn(successResponse(Map.of(
                        "customer", CUSTOMER_NUMBER,
                        "name", "Acme Corp",
                        "position", "Engineer",
                        "salary", "80000.00",
                        "type", "Primary",
                        "id", "1")));

        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "80000.00", "Primary");

        assertTrue(result.isSuccessful());
        assertTrue(result.getValidationErrors().isValid());
        verify(connection, times(1)).send(any(MessageRequest.class));
    }

    @Test
    void addEmployerAcceptsABlankSalary() throws IOException {
        when(connection.send(argThat(requestOfType("customer.addEmployer"))))
                .thenReturn(successResponse(Map.of(
                        "customer", CUSTOMER_NUMBER,
                        "name", "Acme Corp",
                        "position", "Engineer",
                        "salary", "0.0",
                        "type", "Secondary",
                        "id", "2")));

        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "", "Secondary");

        assertTrue(result.isSuccessful());
    }

    @Test
    void addEmployerAcceptsANullSalary() throws IOException {
        when(connection.send(argThat(requestOfType("customer.addEmployer"))))
                .thenReturn(successResponse(Map.of(
                        "customer", CUSTOMER_NUMBER,
                        "name", "Acme Corp",
                        "position", "Engineer",
                        "salary", "0.0",
                        "type", "Primary",
                        "id", "3")));

        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", null, "Primary");

        assertTrue(result.isSuccessful());
    }

    @Test
    void addEmployerReportsAMainframeRejectionWithoutThrowing() throws IOException {
        when(connection.send(argThat(requestOfType("customer.addEmployer"))))
                .thenReturn(rejectedResponse("customer not found"));

        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Primary");

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
    }

    @Test
    void addEmployerFailsValidationForMissingRequiredFields() throws IOException {
        EmployerResult result = service.addEmployer(null, "", "Engineer", "50000", null);

        assertFalse(result.isSuccessful());
        assertFalse(result.getValidationErrors().isValid());
        // customerNumber, name, type all missing = 3 errors
        assertEquals(3, result.getValidationErrors().getErrors().size());
        verifyNoInteractions(connection);
    }

    @Test
    void addEmployerRejectsAnUnknownType() throws IOException {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Contractor");

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void addEmployerTreatsABlankTypeAsMissingRatherThanUnknown() throws IOException {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "   ");

        // only the "is required" error - the unknown-type check is skipped for blank
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void addEmployerRejectsAnInvalidSalaryFormat() throws IOException {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "not-a-number", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("salary", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void addEmployerRejectsASalaryWithMoreThanTwoDecimalPlaces() throws IOException {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000.999", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals("salary", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    // --- updateEmployer ---

    @Test
    void updateEmployerSendsTheRequestAndSucceeds() throws IOException {
        when(connection.send(argThat(requestOfType("customer.updateEmployer"))))
                .thenReturn(successResponse(Map.of(
                        "customer", CUSTOMER_NUMBER,
                        "id", "1",
                        "name", "Acme Corp",
                        "position", "Senior Engineer",
                        "salary", "90000.00",
                        "type", "Primary")));

        EmployerResult result = service.updateEmployer(
                1L, CUSTOMER_NUMBER, "Acme Corp", "Senior Engineer", "90000.00", "Primary");

        assertTrue(result.isSuccessful());
        verify(connection, times(1)).send(any(MessageRequest.class));
    }

    @Test
    void updateEmployerReportsAMainframeRejectionWithoutThrowing() throws IOException {
        when(connection.send(argThat(requestOfType("customer.updateEmployer"))))
                .thenReturn(rejectedResponse("no such employer"));

        EmployerResult result = service.updateEmployer(
                99L, CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Primary");

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
    }

    @Test
    void updateEmployerFailsValidationForMissingRequiredFields() throws IOException {
        EmployerResult result = service.updateEmployer(1L, null, "", "Engineer", "50000", null);

        assertFalse(result.isSuccessful());
        assertEquals(3, result.getValidationErrors().getErrors().size());
        verifyNoInteractions(connection);
    }

    @Test
    void updateEmployerRejectsAnUnknownType() throws IOException {
        EmployerResult result = service.updateEmployer(
                1L, CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Freelance");

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void updateEmployerRejectsAnInvalidSalary() throws IOException {
        EmployerResult result = service.updateEmployer(
                1L, CUSTOMER_NUMBER, "Acme Corp", "Engineer", "-500", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals("salary", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    // --- deleteEmployer ---

    @Test
    void deleteEmployerSendsTheDeleteRequestAndSucceeds() throws IOException {
        when(connection.send(argThat(requestOfType("customer.deleteEmployer"))))
                .thenReturn(successResponse(Map.of("customer", CUSTOMER_NUMBER, "id", "1")));

        EmployerResult result = service.deleteEmployer(1L, CUSTOMER_NUMBER);

        assertTrue(result.isSuccessful());
        verify(connection, times(1)).send(any(MessageRequest.class));
    }

    @Test
    void deleteEmployerFailsValidationWithoutContactingTheMainframe() throws IOException {
        EmployerResult result = service.deleteEmployer(1L, " ");

        assertFalse(result.isSuccessful());
        assertFalse(result.getValidationErrors().isValid());
        verifyNoInteractions(connection);
    }

    @Test
    void deleteEmployerReportsAMainframeRejectionWithoutThrowing() throws IOException {
        when(connection.send(argThat(requestOfType("customer.deleteEmployer"))))
                .thenReturn(rejectedResponse("employer not found"));

        EmployerResult result = service.deleteEmployer(99L, CUSTOMER_NUMBER);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
    }

    @Test
    void deleteEmployerFailsValidationForNullCustomerNumber() throws IOException {
        EmployerResult result = service.deleteEmployer(1L, null);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("customerNumber", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    // --- helpers ---

    private static org.mockito.ArgumentMatcher<MessageRequest> requestOfType(String requestType) {
        return request -> request != null && requestType.equals(request.getRequestType());
    }

    private static MessageResponse successResponse(Map<String, String> values) {
        return new MessageResponse(new Status(1), new HashMap<>(values));
    }

    private static MessageResponse rejectedResponse(String message) {
        return new MessageResponse(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), message, 1),
                new HashMap<>());
    }
}
