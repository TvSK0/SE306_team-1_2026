package mainframe.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. Exercises the behaviour this card added to FakeConnection:
// going offline partway through a test makes send() return a rejection,
// instead of the scripted response, until the connection comes back.
class FakeConnectionTests {
    @Test
    void returnsTheScriptedResponseWhileConnected() {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("search", FakeConnection.list("Jane Doe"));

        MessageResponse response = connection.send(new MessageRequest(MessageRequest.QUERY, "search"));

        assertTrue(response.getStatus().getWasSuccessful());
        assertEquals("Jane Doe", response.getValue("1:name"));
    }

    @Test
    void goingOfflineMidTestMakesSendReturnARejection() {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("search", FakeConnection.list("Jane Doe"));
        MessageRequest request = new MessageRequest(MessageRequest.QUERY, "search");

        // works normally at first
        MessageResponse before = connection.send(request);
        assertTrue(before.getStatus().getWasSuccessful());

        // the connection drops partway through the test
        connection.setIsConnected(false);

        MessageResponse after = connection.send(request);

        assertFalse(after.getStatus().getWasSuccessful());
        assertEquals(after.getStatus().getErrorCode(), StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode());
        assertFalse(connection.isConnected());
    }

    @Test
    void reconnectingAfterwardsResumesTheScriptedResponse() {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("search", FakeConnection.list("Jane Doe"));
        MessageRequest request = new MessageRequest(MessageRequest.QUERY, "search");

        connection.setIsConnected(false);
        connection.send(request);

        connection.setIsConnected(true);
        MessageResponse afterReconnect = connection.send(request);

        assertTrue(afterReconnect.getStatus().getWasSuccessful());
        assertTrue(connection.isConnected());
    }

    @Test
    void requestsAttemptedWhileOfflineAreStillRecorded() {
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        MessageRequest request = new MessageRequest(MessageRequest.QUERY, "search");

        connection.send(request);

        assertEquals(1, connection.getSentRequests().size());
        assertSame(request, connection.getSentRequests().get(0));
    }

    @Test
    void throwsWhenNothingWasScriptedForTheRequest() {
        FakeConnection connection = new FakeConnection();

        assertThrows(IllegalStateException.class,
                () -> connection.send(new MessageRequest(MessageRequest.QUERY, "search")));
    }
}
