package lavs.logic;

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
import static org.mockito.Mockito.*;

// AI-assisted
@ExtendWith(MockitoExtension.class)
class LoanStatusServiceTests {
    private static final String LOAN_NUMBER = "01-0001-0000015-001";

    @Mock
    private Connection connection;

    private LoanStatusService service;

    @BeforeEach
    void setUp() {
        service = new LoanStatusService(connection);
    }

    @Test
    void submitsForVerificationWhenEligibleWithAddressAndPhone() throws IOException {
        when(connection.send(any(MessageRequest.class)))
                .thenReturn(successResponse(Map.of("loanNumber", LOAN_NUMBER, "status", "Pending")));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, true, true, false);

        assertTrue(result.isSuccessful());
        verify(connection).send(argThat(request ->
                "loan.updateStatus".equals(request.getRequestType())
                        && "Pending".equals(request.getValue("status"))
                        && LOAN_NUMBER.equals(request.getValue("loanNumber"))));
    }

    @Test
    void submitsForVerificationWhenEligibleWithAddressAndEmail() throws IOException {
        when(connection.send(any(MessageRequest.class)))
                .thenReturn(successResponse(Map.of("loanNumber", LOAN_NUMBER, "status", "Pending")));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, true, false, true);

        assertTrue(result.isSuccessful());
    }

    @Test
    void rejectsSubmissionWithoutAMailingAddressWithoutContactingTheMainframe() throws IOException {
        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, false, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("eligibility", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void rejectsSubmissionWithoutAnyWayToReachTheCustomer() throws IOException {
        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, true, false, false);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        verifyNoInteractions(connection);
    }

    @Test
    void rejectsAMissingLoanNumberWithoutContactingTheMainframe() throws IOException {
        LoanStatusResult result = service.submitForVerification("", true, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("loanNumber", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void reportsBothFieldErrorsWhenNeitherIsSatisfied() throws IOException {
        LoanStatusResult result = service.submitForVerification(null, false, false, false);

        assertFalse(result.isSuccessful());
        assertEquals(2, result.getValidationErrors().getErrors().size());
        verifyNoInteractions(connection);
    }

    @Test
    void reportsAMainframeRejectionWithoutThrowing() throws IOException {
        when(connection.send(any(MessageRequest.class)))
                .thenReturn(rejectedResponse("Loan with number '" + LOAN_NUMBER + "' cannot change from New to Pending"));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, true, true, true);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
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
