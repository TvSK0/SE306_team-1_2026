package lavs.data;

import lavs.model.PendingChange;
import mainframe.client.Connection;
import mainframe.client.FakeConnection;
import mainframe.client.Message;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.messages.loan.UpdateStatus;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// AI-assisted
class MainframeGatewayTests {
    private final FakeConnection connection = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(connection);

    // --- send(Message): every answer is one of three results, nothing is thrown ---

    @Test
    void reportsASuccessAndLeavesTheRepliedValuesOnTheMessage() {
        connection.whenRequested("loan.updateStatus",
                FakeConnection.success(Map.of("loanNumber", "L-1", "status", "Pending")));
        UpdateStatus message = new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING);

        GatewayResult result = gateway.send(message);

        assertTrue(result.isSuccess());
        assertNull(result.getMessage());
        assertEquals("Pending", message.getStatus());
    }

    @Test
    void reportsANetworkFailure() {
        connection.setIsConnected(false);

        GatewayResult result = gateway.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING));

        assertEquals(GatewayResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getMessage(), result.getMessage());
    }

    @Test
    void reportsARejectionWithTheMainframesReason() {
        connection.whenRequested("loan.updateStatus", FakeConnection.rejected("cannot change from Active"));

        GatewayResult result = gateway.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING));

        assertEquals(GatewayResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("cannot change from Active", result.getMessage());
        assertFalse(result.getStatus().getWasSuccessful());
    }

    @Test
    void turnsAnIOExceptionIntoANetworkFailure() {
        Message throwing = ignored -> {
            throw new IOException("timed out");
        };

        GatewayResult result = gateway.send(throwing);

        assertEquals(GatewayResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertEquals("timed out", result.getMessage());
    }

    // --- signing in and out goes through the Connection, which keeps the token ---

    @Test
    void signsInThroughTheConnection() throws IOException {
        Connection mock = mock(Connection.class);
        when(mock.Login("jsmith", "secret")).thenReturn(new Status(1));

        GatewayResult result = new MainframeGateway(mock).login("jsmith", "secret");

        assertTrue(result.isSuccess());
        verify(mock).Login("jsmith", "secret");
    }

    @Test
    void reportsRefusedCredentials() throws IOException {
        Connection mock = mock(Connection.class);
        when(mock.Login(anyString(), anyString()))
                .thenReturn(new Status(StatusMessage.SERVER_FAILURE.getCode(), "invalid login", 1));

        GatewayResult result = new MainframeGateway(mock).login("jsmith", "wrong");

        assertEquals(GatewayResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("invalid login", result.getMessage());
    }

    @Test
    void reportsASignInThatCouldNotReachTheMainframe() throws IOException {
        Connection mock = mock(Connection.class);
        when(mock.Login(anyString(), anyString())).thenThrow(new IOException("no route to host"));

        GatewayResult result = new MainframeGateway(mock).login("jsmith", "secret");

        assertEquals(GatewayResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertEquals("no route to host", result.getMessage());
    }

    @Test
    void signsOutThroughTheConnection() {
        Connection mock = mock(Connection.class);

        new MainframeGateway(mock).logout();

        verify(mock).Logout();
    }

    @Test
    void reportsWhetherTheMainframeCanBeReached() {
        assertTrue(gateway.isConnected());
        connection.setIsConnected(false);
        assertFalse(gateway.isConnected());
    }

    // --- send(PendingChange): a queued change goes out as its message ---

    @Test
    void sendsAQueuedChangeAsItsMessageWithItsFields() {
        connection.whenRequested("customer.addEmail", FakeConnection.success(Map.of("id", "4")));
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("customerNumber", "C-1");
        fields.put("address", "jane@example.com");

        SendResult result = gateway.send(new PendingChange("customer.addEmail", "C-1", null, fields));

        assertEquals(SendResult.Outcome.SUCCESS, result.getOutcome());
        assertNull(result.getAssignedNumber(), "only a create assigns a number");
        MessageRequest sent = connection.getSentRequests().get(0);
        assertEquals(MessageRequest.UPDATE, sent.getMessageType());
        assertEquals("customer.addEmail", sent.getRequestType());
        assertEquals("C-1", sent.getValue("customerNumber"));
        assertEquals("jane@example.com", sent.getValue("address"));
    }

    @Test
    void passesOnTheNumberTheMainframeGaveANewCustomer() {
        connection.whenRequested("customer.create", FakeConnection.success(Map.of("number", "C-77")));

        SendResult result = gateway.send(new PendingChange("customer.create", null, "TMP-1", Map.of("firstName", "Jane")));

        assertEquals("C-77", result.getAssignedNumber());
    }

    @Test
    void passesOnTheNumberTheMainframeGaveANewLoan() {
        connection.whenRequested("loan.create", FakeConnection.success(Map.of("loanNumber", "L-9")));

        SendResult result = gateway.send(new PendingChange("loan.create", "C-1", "TMP-2", Map.of("customerNumber", "C-1")));

        assertEquals("L-9", result.getAssignedNumber());
    }

    @Test
    void leavesAQueuedChangeQueuedWhenTheNetworkIsDown() {
        connection.setIsConnected(false);

        SendResult result = gateway.send(new PendingChange("customer.addEmail", "C-1", null, Map.of()));

        assertEquals(SendResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertNotNull(result.getMessage());
    }

    @Test
    void treatsAnUnreadableReplyAsANetworkFailureSoTheChangeIsTriedAgain() {
        connection.whenRequested("customer.addEmail", StatusMessage.UNABLE_TO_PARSE.generateMessageResponse(1));

        SendResult result = gateway.send(new PendingChange("customer.addEmail", "C-1", null, Map.of()));

        assertEquals(SendResult.Outcome.NETWORK_FAILURE, result.getOutcome());
    }

    @Test
    void reportsAQueuedChangeTheMainframeRefused() {
        connection.whenRequested("customer.addEmail", FakeConnection.rejected("email is not valid"));

        SendResult result = gateway.send(new PendingChange("customer.addEmail", "C-1", null, Map.of()));

        assertEquals(SendResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("email is not valid", result.getMessage());
    }

    // --- findExistingCustomers: the duplicate check before a new customer is uploaded ---

    @Test
    void findsACustomerWithTheSameNameAndDateOfBirth() {
        givenSearchPage(1, 2,
                customer("C-10", " jane ", "SMITH", "2001-02-03"),
                customer("C-11", "Jane", "Smith", "1999-01-01"));

        DuplicateCheckResult result = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "2001-02-03"));

        assertEquals(DuplicateCheckResult.Outcome.FOUND, result.getOutcome());
        assertEquals(List.of("C-10"), result.getMatchingNumbers());
        MessageRequest search = connection.getSentRequests().get(0);
        assertEquals("Smith", search.getValue("search"));
        assertEquals(Long.toString(MainframeGateway.SEARCH_PAGE_SIZE), search.getValue("size"));
    }

    @Test
    void findsNobodyWhenOnlyTheNameMatches() {
        givenSearchPage(1, 1, customer("C-11", "Jane", "Smith", "1999-01-01"));

        DuplicateCheckResult result = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "2001-02-03"));

        assertEquals(DuplicateCheckResult.Outcome.NONE, result.getOutcome());
    }

    @Test
    void doesNotMatchALongerFamilyNameTheSearchAlsoFound() {
        givenSearchPage(1, 1, customer("C-12", "Jane", "Smithers", "2001-02-03"));

        DuplicateCheckResult result = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "2001-02-03"));

        assertEquals(DuplicateCheckResult.Outcome.NONE, result.getOutcome());
    }

    @Test
    void neverMatchesOnAMissingValue() {
        // the mainframe left out a date of birth, and the queued customer has no first name
        givenSearchPage(1, 2, customer("C-13", "Jane", "Smith", null), customer("C-14", "Jane", "Smith", "2001-02-03"));

        DuplicateCheckResult noDateOfBirth = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "1990-05-05"));
        DuplicateCheckResult noFirstName = gateway.findExistingCustomers(newCustomer(null, "Smith", "2001-02-03"));

        assertEquals(DuplicateCheckResult.Outcome.NONE, noDateOfBirth.getOutcome());
        assertEquals(DuplicateCheckResult.Outcome.NONE, noFirstName.getOutcome());
    }

    @Test
    void readsEveryPageOfTheSearch() {
        givenSearchPage(1, 3, customer("C-1", "Ana", "Smith", "1980-01-01"), customer("C-2", "Bo", "Smith", "1980-01-01"));
        givenSearchPage(2, 3, customer("C-3", "Jane", "Smith", "2001-02-03"));

        DuplicateCheckResult result = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "2001-02-03"));

        assertEquals(List.of("C-3"), result.getMatchingNumbers());
        assertEquals(2, connection.getSentRequests().size());
    }

    @Test
    void stopsOnAnEmptyPageEvenIfTheTotalSaysThereAreMore() {
        givenSearchPage(1, 9, customer("C-1", "Ana", "Smith", "1980-01-01"));
        givenSearchPage(2, 9);

        DuplicateCheckResult result = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "2001-02-03"));

        assertEquals(DuplicateCheckResult.Outcome.NONE, result.getOutcome());
        assertEquals(2, connection.getSentRequests().size());
    }

    @Test
    void waitsToCheckAgainWhenTheSearchCannotReachTheMainframe() {
        connection.setIsConnected(false);

        DuplicateCheckResult result = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "2001-02-03"));

        assertEquals(DuplicateCheckResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertNotNull(result.getMessage());
    }

    @Test
    void doesNotHoldTheCustomerBackWhenTheMainframeRefusesTheSearch() {
        connection.whenRequested("customer.search", FakeConnection.rejected("search text too short"));

        DuplicateCheckResult result = gateway.findExistingCustomers(newCustomer("Jane", "Smith", "2001-02-03"));

        assertEquals(DuplicateCheckResult.Outcome.NONE, result.getOutcome());
    }

    @Test
    void cannotSearchWithoutAFamilyName() {
        DuplicateCheckResult blank = gateway.findExistingCustomers(newCustomer("Jane", " ", "2001-02-03"));
        DuplicateCheckResult missing = gateway.findExistingCustomers(
                new PendingChange("customer.create", null, "TMP-1", Map.of("firstName", "Jane")));

        assertEquals(DuplicateCheckResult.Outcome.NONE, blank.getOutcome());
        assertEquals(DuplicateCheckResult.Outcome.NONE, missing.getOutcome());
        assertTrue(connection.getSentRequests().isEmpty());
    }

    // --- a stale transaction number: another copy of the app signed in as the same user ---

    private final Connection server = mock(Connection.class);
    private final MainframeGateway shared = new MainframeGateway(server);

    @Test
    void signsInAgainAndResendsWhenTheTransactionNumberIsStale() throws IOException {
        signIn();
        when(server.send(any())).thenReturn(stale(),
                FakeConnection.success(Map.of("loanNumber", "L-1", "status", "Pending")));
        UpdateStatus message = new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING);

        GatewayResult result = shared.send(message);

        assertTrue(result.isSuccess());
        assertEquals("Pending", message.getStatus());
        verify(server, times(2)).Login("admin", "secret");
        verify(server, times(2)).send(any());
    }

    @Test
    void aQueuedChangeIsResentRatherThanMarkedRejected() throws IOException {
        signIn();
        when(server.send(any())).thenReturn(stale(), FakeConnection.success(Map.of()));

        SendResult result = shared.send(new PendingChange("customer.addEmail", "C-1", null, Map.of()));

        assertEquals(SendResult.Outcome.SUCCESS, result.getOutcome());
    }

    @Test
    void passesTheRejectionOnIfSigningInAgainIsRefused() throws IOException {
        signIn();
        when(server.Login("admin", "secret")).thenReturn(new Status(StatusMessage.SERVER_FAILURE.getCode(), "Locked", 1));
        when(server.send(any())).thenReturn(stale());

        GatewayResult result = shared.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING));

        assertEquals(GatewayResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("Invalid transaction number", result.getMessage());
        verify(server, times(1)).send(any());
    }

    @Test
    void passesTheRejectionOnIfTheServerCantBeReachedToSignInAgain() throws IOException {
        signIn();
        when(server.Login("admin", "secret")).thenThrow(new IOException("down"));
        when(server.send(any())).thenReturn(stale());

        GatewayResult result = shared.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING));

        assertEquals(GatewayResult.Outcome.REJECTED, result.getOutcome());
        verify(server, times(1)).send(any());
    }

    @Test
    void doesNotResendOtherRejections() throws IOException {
        signIn();
        when(server.send(any())).thenReturn(FakeConnection.rejected("cannot change from Active"),
                new MessageResponse(new Status(StatusMessage.SERVER_FAILURE.getCode(), null, 1), new HashMap<>()));

        assertEquals("cannot change from Active", shared.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING)).getMessage());
        assertEquals(GatewayResult.Outcome.REJECTED, shared.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING)).getOutcome());
        verify(server, times(1)).Login(any(), any());
    }

    @Test
    void doesNotSignInAgainAfterLoggingOut() throws IOException {
        signIn();
        shared.logout();
        when(server.send(any())).thenReturn(stale());

        assertEquals(GatewayResult.Outcome.REJECTED, shared.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING)).getOutcome());
        verify(server, times(1)).Login(any(), any());
    }

    @Test
    void doesNotKeepDetailsFromAFailedLogin() throws IOException {
        when(server.Login("admin", "wrong")).thenReturn(new Status(StatusMessage.SERVER_FAILURE.getCode(), "Bad login", 1));
        assertFalse(shared.login("admin", "wrong").isSuccess());
        when(server.send(any())).thenReturn(stale());

        assertEquals(GatewayResult.Outcome.REJECTED, shared.send(new UpdateStatus("L-1", UpdateStatus.STATUS_PENDING)).getOutcome());
        verify(server, times(1)).Login(any(), any());
    }

    // --- helpers ---

    private void signIn() throws IOException {
        when(server.Login("admin", "secret")).thenReturn(new Status(1));
        assertTrue(shared.login("admin", "secret").isSuccess());
    }

    // what the server says when another copy signed in as the same user used the next number
    private static MessageResponse stale() {
        return FakeConnection.rejected("Invalid transaction number");
    }

    private static PendingChange newCustomer(String firstName, String familyName, String dateOfBirth) {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("firstName", firstName);
        fields.put("familyName", familyName);
        fields.put("dateOfBirth", dateOfBirth);
        return new PendingChange("customer.create", null, "TMP-1", fields);
    }

    private static String[] customer(String number, String firstName, String familyName, String dateOfBirth) {
        return new String[] {number, firstName, familyName, dateOfBirth};
    }

    private void givenSearchPage(long page, long total, String[]... customers) {
        HashMap<String, String> values = new HashMap<>();
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(customers.length));
        for (int i = 1; i <= customers.length; i++) {
            values.put(i + ":number", customers[i - 1][0]);
            values.put(i + ":firstName", customers[i - 1][1]);
            values.put(i + ":familyName", customers[i - 1][2]);
            values.put(i + ":dateOfBirth", customers[i - 1][3]);
        }
        connection.whenRequested("customer.search", Map.of("page", Long.toString(page)),
                new MessageResponse(new Status(1), values));
    }
}
