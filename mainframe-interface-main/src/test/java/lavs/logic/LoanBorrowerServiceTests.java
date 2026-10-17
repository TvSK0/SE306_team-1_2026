package lavs.logic;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.messages.loan.AddCoborrower;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

// AI-assisted
@ExtendWith(MockitoExtension.class)
class LoanBorrowerServiceTests {
    private static final String LOAN_NUMBER = "01-0001-0000015-001";
    private static final String CUSTOMER_NUMBER = "CUST-42";

    @Mock
    private Connection connection;

    private LoanBorrowerService service;

    @BeforeEach
    void setUp() {
        service = new LoanBorrowerService(connection);
    }

    // --- addCoborrower ---

    @Test
    void addCoborrowerSendsTheRequestAndSucceeds() throws IOException {
        when(connection.send(argThat(requestOfType("loan.addCoborrower"))))
                .thenReturn(successResponse(Map.of(
                        "loanNumber", LOAN_NUMBER,
                        "coborrowerNumber", CUSTOMER_NUMBER,
                        "type", AddCoborrower.TYPE_GUARANTOR,
                        "id", "7")));

        LoanBorrowerResult result = service.addCoborrower(LOAN_NUMBER, CUSTOMER_NUMBER, AddCoborrower.TYPE_GUARANTOR);

        assertTrue(result.isSuccessful());
        assertTrue(result.getValidationErrors().isValid());
        verify(connection, times(1)).send(any(MessageRequest.class));
    }

    @Test
    void addCoborrowerReportsAMainframeRejectionWithoutThrowing() throws IOException {
        when(connection.send(argThat(requestOfType("loan.addCoborrower"))))
                .thenReturn(rejectedResponse("loan is already Active"));

        LoanBorrowerResult result = service.addCoborrower(LOAN_NUMBER, CUSTOMER_NUMBER, AddCoborrower.TYPE_COBORROWER);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
    }

    @Test
    void addCoborrowerFailsValidationWithoutContactingTheMainframe() throws IOException {
        LoanBorrowerResult result = service.addCoborrower(null, "", null);

        assertFalse(result.isSuccessful());
        assertFalse(result.getValidationErrors().isValid());
        assertEquals(3, result.getValidationErrors().getErrors().size());
        verifyNoInteractions(connection);
    }

    @Test
    void addCoborrowerRejectsAnUnknownType() throws IOException {
        LoanBorrowerResult result = service.addCoborrower(LOAN_NUMBER, CUSTOMER_NUMBER, "Cosigner");

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void addCoborrowerTreatsABlankTypeAsMissingRatherThanUnknown() throws IOException {
        LoanBorrowerResult result = service.addCoborrower(LOAN_NUMBER, CUSTOMER_NUMBER, "   ");

        // only the "is required" error - the unknown-type check is skipped for a blank value
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    // --- removeCoborrower ---

    @Test
    void removeCoborrowerSendsTheDeleteRequestAndSucceeds() throws IOException {
        when(connection.send(argThat(requestOfType("loan.deleteCoborrower"))))
                .thenReturn(successResponse(Map.of("loanNumber", LOAN_NUMBER, "id", "7")));

        LoanBorrowerResult result = service.removeCoborrower(7, LOAN_NUMBER);

        assertTrue(result.isSuccessful());
        verify(connection, times(1)).send(any(MessageRequest.class));
    }

    @Test
    void removeCoborrowerFailsValidationWithoutContactingTheMainframe() throws IOException {
        LoanBorrowerResult result = service.removeCoborrower(7, " ");

        assertFalse(result.isSuccessful());
        verifyNoInteractions(connection);
    }

    // --- updateCoborrower: remove then re-add ---

    @Test
    void updateCoborrowerRemovesThenReAddsInOrder() throws IOException {
        when(connection.send(argThat(requestOfType("loan.deleteCoborrower"))))
                .thenReturn(successResponse(Map.of("loanNumber", LOAN_NUMBER, "id", "7")));
        when(connection.send(argThat(requestOfType("loan.addCoborrower"))))
                .thenReturn(successResponse(Map.of(
                        "loanNumber", LOAN_NUMBER,
                        "coborrowerNumber", CUSTOMER_NUMBER,
                        "type", AddCoborrower.TYPE_GUARANTOR,
                        "id", "8")));

        LoanBorrowerResult result = service.updateCoborrower(7, LOAN_NUMBER, CUSTOMER_NUMBER, AddCoborrower.TYPE_GUARANTOR);

        assertTrue(result.isSuccessful());
        InOrder order = inOrder(connection);
        order.verify(connection).send(argThat(requestOfType("loan.deleteCoborrower")));
        order.verify(connection).send(argThat(requestOfType("loan.addCoborrower")));
    }

    @Test
    void updateCoborrowerDoesNotReAddWhenTheRemovalFails() throws IOException {
        when(connection.send(argThat(requestOfType("loan.deleteCoborrower"))))
                .thenReturn(rejectedResponse("no such co-borrower record"));

        LoanBorrowerResult result = service.updateCoborrower(7, LOAN_NUMBER, CUSTOMER_NUMBER, AddCoborrower.TYPE_GUARANTOR);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
        verify(connection, times(1)).send(any(MessageRequest.class));
        verify(connection, never()).send(argThat(requestOfType("loan.addCoborrower")));
    }

    @Test
    void updateCoborrowerFailsValidationWithoutContactingTheMainframe() throws IOException {
        LoanBorrowerResult result = service.updateCoborrower(7, LOAN_NUMBER, CUSTOMER_NUMBER, "Cosigner");

        assertFalse(result.isSuccessful());
        verifyNoInteractions(connection);
    }

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
