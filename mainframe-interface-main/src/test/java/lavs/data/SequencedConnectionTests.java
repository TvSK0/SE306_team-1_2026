package lavs.data;

import mainframe.client.Connection;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class SequencedConnectionTests {
    private static final MessageResponse STALE = FakeConnection.rejected("Invalid transaction number");
    private static final MessageResponse OK = FakeConnection.success(Map.of("name", "ok"));

    @Test
    void passesARequestThrough() {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(OK);
        SequencedConnection connection = new SequencedConnection(inner);

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(OK, response),
                () -> Assertions.assertEquals(1, inner._sends),
                () -> Assertions.assertEquals(0, inner._logins));
    }

    @Test
    void signsInAgainAndResendsWhenTheNumberIsStale() throws IOException {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(STALE, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(OK, response),
                () -> Assertions.assertEquals(2, inner._sends),
                () -> Assertions.assertEquals(List.of("admin:secret", "admin:secret"), inner._loginsAs));
    }

    @Test
    void resendsOnlyOnce() throws IOException {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(STALE, STALE, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(STALE, response),
                () -> Assertions.assertEquals(2, inner._sends));
    }

    @Test
    void leavesOtherRejectionsAlone() throws IOException {
        // Arrange
        MessageResponse rejected = FakeConnection.rejected("Invalid customer number");
        ScriptedConnection inner = new ScriptedConnection(rejected, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(rejected, response),
                () -> Assertions.assertEquals(1, inner._sends),
                () -> Assertions.assertEquals(1, inner._logins));
    }

    @Test
    void leavesARejectionWithNoMessageAlone() throws IOException {
        // Arrange
        MessageResponse rejected = FakeConnection.rejected(null);
        ScriptedConnection inner = new ScriptedConnection(rejected, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(rejected, response),
                () -> Assertions.assertEquals(1, inner._sends));
    }

    @Test
    void leavesOtherFailuresAlone() throws IOException {
        // Arrange
        MessageResponse down = FakeConnection.networkDown();
        ScriptedConnection inner = new ScriptedConnection(down, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(down, response),
                () -> Assertions.assertEquals(1, inner._sends));
    }

    @Test
    void cannotResendBeforeSigningIn() {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(STALE, OK);
        SequencedConnection connection = new SequencedConnection(inner);

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(STALE, response),
                () -> Assertions.assertEquals(1, inner._sends),
                () -> Assertions.assertEquals(0, inner._logins));
    }

    @Test
    void cannotResendAfterSigningOut() throws IOException {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(STALE, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");
        connection.Logout();

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(STALE, response),
                () -> Assertions.assertEquals(1, inner._sends),
                () -> Assertions.assertEquals(1, inner._logouts));
    }

    @Test
    void aFailedSignInIsNotRemembered() throws IOException {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(STALE, OK);
        inner._loginStatus = rejectedStatus("Invalid login");
        SequencedConnection connection = new SequencedConnection(inner);

        // Act
        Status login = connection.Login("admin", "wrong");
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(login.getWasSuccessful()),
                () -> Assertions.assertSame(STALE, response),
                () -> Assertions.assertEquals(1, inner._logins));
    }

    @Test
    void givesUpWhenSigningInAgainIsRefused() throws IOException {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(STALE, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");
        inner._loginStatus = rejectedStatus("Invalid login");

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(STALE, response),
                () -> Assertions.assertEquals(1, inner._sends));
    }

    @Test
    void givesUpWhenSigningInAgainCannotReachTheServer() throws IOException {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection(STALE, OK);
        SequencedConnection connection = new SequencedConnection(inner);
        connection.Login("admin", "secret");
        inner._loginThrows = true;

        // Act
        MessageResponse response = connection.send(request());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertSame(STALE, response),
                () -> Assertions.assertEquals(1, inner._sends));
    }

    @Test
    void sendsOneRequestAtATime() throws Exception {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection();
        inner._sendMillis = 20;
        SequencedConnection connection = new SequencedConnection(inner);
        ExecutorService threads = Executors.newFixedThreadPool(4);

        // Act
        List<Future<MessageResponse>> sends = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            sends.add(threads.submit(() -> connection.send(request())));
        }
        for (Future<MessageResponse> send : sends) {
            send.get(5, TimeUnit.SECONDS);
        }
        threads.shutdown();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(8, inner._sends),
                () -> Assertions.assertEquals(1, inner._mostAtOnce.get()));
    }

    @Test
    void passesTheRestThrough() throws IOException {
        // Arrange
        ScriptedConnection inner = new ScriptedConnection();
        SequencedConnection connection = new SequencedConnection(inner);

        // Act
        boolean connected = connection.isConnected();
        connection.close();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(connected),
                () -> Assertions.assertEquals(1, inner._closes));
    }

    private static MessageRequest request() {
        return new MessageRequest(MessageRequest.QUERY, "customer.retrieve");
    }

    private static Status rejectedStatus(String message) {
        return new Status(StatusMessage.SERVER_FAILURE.getCode(), message, 1);
    }

    // answers each send with the next scripted response (OK once they run out), counts
    // every call, and records the most sends it was ever asked to answer at once
    private static class ScriptedConnection implements Connection {
        private final Deque<MessageResponse> _responses = new ArrayDeque<>();
        private final List<String> _loginsAs = new ArrayList<>();
        private final AtomicInteger _inFlight = new AtomicInteger();
        private final AtomicInteger _mostAtOnce = new AtomicInteger();
        private volatile Status _loginStatus = new Status(1);
        private volatile boolean _loginThrows;
        private volatile long _sendMillis;
        private volatile int _sends;
        private int _logins;
        private int _logouts;
        private int _closes;

        ScriptedConnection(MessageResponse... responses) {
            _responses.addAll(List.of(responses));
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            _mostAtOnce.accumulateAndGet(_inFlight.incrementAndGet(), Math::max);
            try {
                Thread.sleep(_sendMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            _sends++;
            MessageResponse response = _responses.isEmpty() ? OK : _responses.poll();
            _inFlight.decrementAndGet();
            return response;
        }

        @Override
        public Status Login(String username, String password) throws IOException {
            _logins++;
            _loginsAs.add(username + ":" + password);
            if (_loginThrows) {
                throw new IOException("unreachable");
            }
            return _loginStatus;
        }

        @Override
        public void Logout() {
            _logouts++;
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void close() {
            _closes++;
        }
    }
}
