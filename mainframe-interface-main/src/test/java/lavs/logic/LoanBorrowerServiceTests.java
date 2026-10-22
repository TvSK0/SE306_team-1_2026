package lavs.logic;

import lavs.model.LoanCoborrower;
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

        LoanBorrowerResult result = service.updateCoborrower(existingCoborrower(), CUSTOMER_NUMBER, AddCoborrower.TYPE_GUARANTOR);

        assertTrue(result.isSuccessful());
        InOrder order = inOrder(connection);
        order.verify(connection).send(argThat(request -> request != null
                && "loan.deleteCoborrower".equals(request.getRequestType())
                && "7".equals(request.getValue("id"))));
        order.verify(connection).send(argThat(addOf(AddCoborrower.TYPE_GUARANTOR)));
        verifyNoMoreInteractions(connection);
    }

    @Test
    void updateCoborrowerDoesNotReAddWhenTheRemovalFails() throws IOException {
        when(connection.send(argThat(requestOfType("loan.deleteCoborrower"))))
                .thenReturn(rejectedResponse("no such co-borrower record"));

        LoanBorrowerResult result = service.updateCoborrower(existingCoborrower(), CUSTOMER_NUMBER, AddCoborrower.TYPE_GUARANTOR);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
        verify(connection, times(1)).send(any(MessageRequest.class));
        verify(connection, never()).send(argThat(requestOfType("loan.addCoborrower")));
    }

    @Test
    void updateCoborrowerPutsTheOriginalBackWhenTheReplacementIsRefused() throws IOException {
        when(connection.send(argThat(requestOfType("loan.deleteCoborrower"))))
                .thenReturn(successResponse(Map.of("loanNumber", LOAN_NUMBER, "id", "7")));
        when(connection.send(argThat(addOf(AddCoborrower.TYPE_GUARANTOR))))
                .thenReturn(rejectedResponse("guarantor must be over 18"));
        when(connection.send(argThat(addOf(AddCoborrower.TYPE_COBORROWER))))
                .thenReturn(successResponse(Map.of("loanNumber", LOAN_NUMBER, "id", "9")));

        LoanBorrowerResult result = service.updateCoborrower(existingCoborrower(), CUSTOMER_NUMBER, AddCoborrower.TYPE_GUARANTOR);

        // the refusal is what the MMM is told about, even though the original went back
        assertFalse(result.isSuccessful());
        assertEquals("guarantor must be over 18", result.getMainframeStatus().getErrorMessage());
        InOrder order = inOrder(connection);
        order.verify(connection).send(argThat(requestOfType("loan.deleteCoborrower")));
        order.verify(connection).send(argThat(addOf(AddCoborrower.TYPE_GUARANTOR)));
        order.verify(connection).send(argThat(addOf(AddCoborrower.TYPE_COBORROWER)));
    }

    @Test
    void updateCoborrowerFailsValidationWithoutContactingTheMainframe() throws IOException {
        LoanBorrowerResult result = service.updateCoborrower(existingCoborrower(), CUSTOMER_NUMBER, "Cosigner");

        assertFalse(result.isSuccessful());
        assertEquals(LoanBorrowerService.TYPE, result.getValidationErrors().getErrors().get(0).getFieldName());
        verifyNoInteractions(connection);
    }

    // record 7 on the loan: CUSTOMER_NUMBER as a co-borrower, as listCoborrowers reads it
    private static LoanCoborrower existingCoborrower() {
        LoanCoborrower existing = new LoanCoborrower(LOAN_NUMBER, CUSTOMER_NUMBER, AddCoborrower.TYPE_COBORROWER);
        existing.setId(7L);
        return existing;
    }

    private static org.mockito.ArgumentMatcher<MessageRequest> addOf(String type) {
        return request -> request != null
                && "loan.addCoborrower".equals(request.getRequestType())
                && LOAN_NUMBER.equals(request.getValue("loanNumber"))
                && CUSTOMER_NUMBER.equals(request.getValue("coborrowerNumber"))
                && type.equals(request.getValue("type"));
    }

    // --- listCoborrowers ---

    @Test
    void listCoborrowersReturnsEveryCoborrowerAndGuarantorOnTheLoan() throws IOException {
        when(connection.send(argThat(listPage(1))))
                .thenReturn(listResponse(2,
                        row(7, AddCoborrower.TYPE_COBORROWER, "CUST-43", "Mere Brown"),
                        row(8, AddCoborrower.TYPE_GUARANTOR, "CUST-44", "Aroha Smith")));

        LoanBorrowerListResult result = service.listCoborrowers(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
        assertEquals(2, result.getCoborrowers().size());
        LoanCoborrower first = result.getCoborrowers().get(0);
        assertAll(
                () -> assertEquals(7L, first.getId()),
                () -> assertEquals(LOAN_NUMBER, first.getLoanNumber()),
                () -> assertEquals("CUST-43", first.getCoborrowerNumber()),
                () -> assertEquals("Mere Brown", first.getCoborrowerName()),
                () -> assertEquals(AddCoborrower.TYPE_COBORROWER, first.getType()),
                () -> assertEquals(AddCoborrower.TYPE_GUARANTOR, result.getCoborrowers().get(1).getType()));
        verify(connection).send(argThat(request ->
                LOAN_NUMBER.equals(request.getValue("number"))
                        && Long.toString(LoanBorrowerService.PAGE_SIZE).equals(request.getValue("size"))));
    }

    @Test
    void listCoborrowersReturnsAnEmptyListForALoanWithNone() throws IOException {
        when(connection.send(argThat(listPage(1)))).thenReturn(listResponse(0));

        LoanBorrowerListResult result = service.listCoborrowers(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
        assertTrue(result.getCoborrowers().isEmpty());
    }

    @Test
    void listCoborrowersKeepsRequestingPagesUntilItHasThemAll() throws IOException {
        when(connection.send(argThat(listPage(1))))
                .thenReturn(listResponse(3,
                        row(7, AddCoborrower.TYPE_COBORROWER, "CUST-43", "Mere Brown"),
                        row(8, AddCoborrower.TYPE_GUARANTOR, "CUST-44", "Aroha Smith")));
        when(connection.send(argThat(listPage(2))))
                .thenReturn(listResponse(3, row(9, AddCoborrower.TYPE_GUARANTOR, "CUST-45", "Hemi Walker")));

        LoanBorrowerListResult result = service.listCoborrowers(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
        assertEquals(3, result.getCoborrowers().size());
        assertEquals("CUST-45", result.getCoborrowers().get(2).getCoborrowerNumber());
        verify(connection, times(2)).send(any(MessageRequest.class));
    }

    @Test
    void listCoborrowersStopsOnAnEmptyPageEvenIfTheTotalSaysThereAreMore() throws IOException {
        when(connection.send(argThat(listPage(1))))
                .thenReturn(listResponse(5, row(7, AddCoborrower.TYPE_COBORROWER, "CUST-43", "Mere Brown")));
        when(connection.send(argThat(listPage(2)))).thenReturn(listResponse(5));

        LoanBorrowerListResult result = service.listCoborrowers(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
        assertEquals(1, result.getCoborrowers().size());
        verify(connection, times(2)).send(any(MessageRequest.class));
    }

    @Test
    void listCoborrowersReportsAMainframeRejectionWithoutThrowing() throws IOException {
        when(connection.send(argThat(listPage(1)))).thenReturn(rejectedResponse("no such loan"));

        LoanBorrowerListResult result = service.listCoborrowers(LOAN_NUMBER);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
        assertTrue(result.getCoborrowers().isEmpty());
    }

    @Test
    void listCoborrowersFailsValidationWithoutContactingTheMainframe() throws IOException {
        LoanBorrowerListResult result = service.listCoborrowers(" ");

        assertFalse(result.isSuccessful());
        assertEquals("loanNumber", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertNull(result.getMainframeStatus());
        assertTrue(result.getCoborrowers().isEmpty());
        verifyNoInteractions(connection);
    }

    private static org.mockito.ArgumentMatcher<MessageRequest> listPage(long page) {
        return request -> request != null
                && "loanCoborrower.list".equals(request.getRequestType())
                && Long.toString(page).equals(request.getValue("page"));
    }

    // one row of a loanCoborrower.list response: id, type, coborrowerNumber, coborrowerName
    private static String[] row(long id, String type, String coborrowerNumber, String coborrowerName) {
        return new String[] {Long.toString(id), type, coborrowerNumber, coborrowerName};
    }

    private static MessageResponse listResponse(long total, String[]... rows) {
        HashMap<String, String> values = new HashMap<>();
        values.put("loanNumber", LOAN_NUMBER);
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(rows.length));
        for (int i = 1; i <= rows.length; i++) {
            values.put(i + ":id", rows[i - 1][0]);
            values.put(i + ":type", rows[i - 1][1]);
            values.put(i + ":coborrowerNumber", rows[i - 1][2]);
            values.put(i + ":coborrowerName", rows[i - 1][3]);
        }
        return new MessageResponse(new Status(1), values);
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
