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
class LoanStatusServiceTests {
    private static final String LOAN_NUMBER = "01-0001-0000015-001";
    private static final String CUSTOMER_NUMBER = "CUST-42";

    @Mock
    private Connection connection;

    private LoanStatusService service;

    @BeforeEach
    void setUp() {
        service = new LoanStatusService(connection);
    }

    // --- eligible, with an employer: no guarantor needed ---

    @Test
    void submitsForVerificationWhenEligibleWithAddressAndPhone() throws IOException {
        givenEmployers(1);
        givenStatusUpdateSucceeds();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, false);

        assertTrue(result.isSuccessful());
        verify(connection).send(argThat(request ->
                request != null
                        && "loan.updateStatus".equals(request.getRequestType())
                        && "Pending".equals(request.getValue("status"))
                        && LOAN_NUMBER.equals(request.getValue("loanNumber"))));
        verify(connection).send(argThat(request ->
                request != null
                        && "customerEmployer.list".equals(request.getRequestType())
                        && CUSTOMER_NUMBER.equals(request.getValue("number"))));
    }

    @Test
    void submitsForVerificationWhenEligibleWithAddressAndEmail() throws IOException {
        givenEmployers(1);
        givenStatusUpdateSucceeds();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, false, true);

        assertTrue(result.isSuccessful());
    }

    @Test
    void doesNotLookForAGuarantorWhenTheBorrowerHasAnEmployer() throws IOException {
        givenEmployers(1);
        givenStatusUpdateSucceeds();

        service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, true);

        verify(connection, never()).send(argThat(requestOfType("loanCoborrower.list")));
    }

    // --- guarantor rule: no employer ---

    @Test
    void submitsForVerificationWithNoEmployerWhenAGuarantorIsOnTheLoan() throws IOException {
        givenEmployers(0);
        givenCoborrowerTypes(AddCoborrower.TYPE_COBORROWER, AddCoborrower.TYPE_GUARANTOR);
        givenStatusUpdateSucceeds();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, true);

        assertTrue(result.isSuccessful());
    }

    @Test
    void blocksSubmissionWithNoEmployerAndOnlyACoborrower() throws IOException {
        givenEmployers(0);
        givenCoborrowerTypes(AddCoborrower.TYPE_COBORROWER);

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals(LoanStatusService.GUARANTOR, result.getValidationErrors().getErrors().get(0).getFieldName());
        assertNull(result.getMainframeStatus());
        verify(connection, never()).send(argThat(requestOfType("loan.updateStatus")));
    }

    @Test
    void blocksSubmissionWithNoEmployerAndNobodyElseOnTheLoan() throws IOException {
        givenEmployers(0);
        givenCoborrowerTypes();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(LoanStatusService.GUARANTOR, result.getValidationErrors().getErrors().get(0).getFieldName());
        verify(connection, never()).send(argThat(requestOfType("loan.updateStatus")));
    }

    @Test
    void reportsAFailedEmployerLookupWithoutSubmitting() throws IOException {
        when(connection.send(argThat(requestOfType("customerEmployer.list"))))
                .thenReturn(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), result.getMainframeStatus().getErrorCode());
        verify(connection, never()).send(argThat(requestOfType("loan.updateStatus")));
    }

    @Test
    void reportsAFailedGuarantorLookupWithoutSubmitting() throws IOException {
        givenEmployers(0);
        when(connection.send(argThat(requestOfType("loanCoborrower.list"))))
                .thenReturn(rejectedResponse("no such loan"));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, true);

        assertFalse(result.isSuccessful());
        assertEquals("no such loan", result.getMainframeStatus().getErrorMessage());
        verify(connection, never()).send(argThat(requestOfType("loan.updateStatus")));
    }

    // --- field errors: nothing is sent ---

    @Test
    void rejectsSubmissionWithoutAMailingAddressWithoutContactingTheMainframe() throws IOException {
        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, false, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("eligibility", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void rejectsSubmissionWithoutAnyWayToReachTheCustomer() throws IOException {
        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, false, false);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        verifyNoInteractions(connection);
    }

    @Test
    void rejectsAMissingLoanNumberWithoutContactingTheMainframe() throws IOException {
        LoanStatusResult result = service.submitForVerification("", CUSTOMER_NUMBER, true, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("loanNumber", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void rejectsAMissingCustomerNumberWithoutContactingTheMainframe() throws IOException {
        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, " ", true, true, true);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("customerNumber", result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    @Test
    void reportsEveryFieldErrorWhenNothingIsSatisfied() throws IOException {
        LoanStatusResult result = service.submitForVerification(null, null, false, false, false);

        assertFalse(result.isSuccessful());
        assertEquals(3, result.getValidationErrors().getErrors().size());
        verifyNoInteractions(connection);
    }

    @Test
    void reportsAMainframeRejectionWithoutThrowing() throws IOException {
        givenEmployers(1);
        when(connection.send(argThat(requestOfType("loan.updateStatus"))))
                .thenReturn(rejectedResponse("Loan with number '" + LOAN_NUMBER + "' cannot change from New to Pending"));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER, CUSTOMER_NUMBER, true, true, true);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
    }

    private void givenEmployers(int count) throws IOException {
        HashMap<String, String> values = new HashMap<>();
        values.put("customerNumber", CUSTOMER_NUMBER);
        values.put("total", Integer.toString(count));
        values.put("items", Integer.toString(count));
        for (int i = 1; i <= count; i++) {
            values.put(i + ":id", Integer.toString(i));
            values.put(i + ":name", "Employer " + i);
            values.put(i + ":type", "Full time");
        }
        when(connection.send(argThat(requestOfType("customerEmployer.list"))))
                .thenReturn(new MessageResponse(new Status(1), values));
    }

    private void givenCoborrowerTypes(String... types) throws IOException {
        HashMap<String, String> values = new HashMap<>();
        values.put("loanNumber", LOAN_NUMBER);
        values.put("total", Integer.toString(types.length));
        values.put("items", Integer.toString(types.length));
        for (int i = 1; i <= types.length; i++) {
            values.put(i + ":id", Integer.toString(i));
            values.put(i + ":type", types[i - 1]);
            values.put(i + ":coborrowerNumber", "CUST-" + (50 + i));
            values.put(i + ":coborrowerName", "Person " + i);
        }
        when(connection.send(argThat(requestOfType("loanCoborrower.list"))))
                .thenReturn(new MessageResponse(new Status(1), values));
    }

    private void givenStatusUpdateSucceeds() throws IOException {
        when(connection.send(argThat(requestOfType("loan.updateStatus"))))
                .thenReturn(successResponse(Map.of("loanNumber", LOAN_NUMBER, "status", "Pending")));
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
