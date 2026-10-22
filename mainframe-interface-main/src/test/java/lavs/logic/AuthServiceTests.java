// AI-assisted: tests for AuthService, one per US1 requirement.
package lavs.logic;

import lavs.validation.FieldError;
import mainframe.client.Connection;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AuthServiceTests {
    // US1-R1
    @Test
    void logsInAndRemembersWhoIsSignedIn() {
        // Arrange
        AuthService service = new AuthService(new FakeConnection());

        // Act
        AuthResult result = service.login("admin", "secret");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(AuthResult.Outcome.SUCCESS, result.getOutcome());
        Assertions.assertEquals("admin", result.getUserName());
        Assertions.assertNull(result.getMessage());
        Assertions.assertTrue(result.getValidationErrors().isValid());
        Assertions.assertEquals("admin", service.currentUser());
    }

    // US1-R3
    @Test
    void trimsTheUsernameButSendsThePasswordAsTyped() {
        // Arrange
        RecordingConnection connection = new RecordingConnection(new Status(1));
        AuthService service = new AuthService(connection);

        // Act
        AuthResult result = service.login("  admin  ", " secret ");

        // Assert
        Assertions.assertEquals(List.of("admin|" + " secret "), connection.logins);
        Assertions.assertEquals("admin", result.getUserName());
        Assertions.assertEquals("admin", service.currentUser());
    }

    // US1-R2
    @Test
    void reportsTheMainframesReasonWhenTheLoginIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setLoginStatus(FakeConnection.rejected("Invalid username or password").getStatus());
        AuthService service = new AuthService(connection);

        // Act
        AuthResult result = service.login("admin", "wrong");

        // Assert
        Assertions.assertFalse(result.isSuccessful());
        Assertions.assertEquals(AuthResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("Invalid username or password", result.getMessage());
        Assertions.assertNull(result.getUserName());
        Assertions.assertNull(service.currentUser());
    }

    // US1-R2
    @Test
    void aFailedLoginLeavesTheCurrentUserUnchanged() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        AuthService service = new AuthService(connection);
        service.login("admin", "secret");
        connection.setLoginStatus(FakeConnection.rejected("Invalid username or password").getStatus());

        // Act
        service.login("other", "wrong");

        // Assert
        Assertions.assertEquals("admin", service.currentUser());
    }

    // US1-R3
    @Test
    void sendsNothingWhenAFieldIsBlank() {
        // Arrange
        RecordingConnection connection = new RecordingConnection(new Status(1));
        AuthService service = new AuthService(connection);

        // Act
        AuthResult result = service.login("   ", "");

        // Assert
        Assertions.assertTrue(connection.logins.isEmpty());
        Assertions.assertFalse(result.isSuccessful());
        Assertions.assertEquals(AuthResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertNull(result.getMessage());
        Assertions.assertEquals(
                List.of(LoginForm.USERNAME, LoginForm.PASSWORD),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        Assertions.assertNull(service.currentUser());
    }

    // US1-R4
    @Test
    void anUnavailableMainframeIsANetworkFailure() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        MessageResponse down = FakeConnection.networkDown();
        connection.setLoginStatus(down.getStatus());
        AuthService service = new AuthService(connection);

        // Act
        AuthResult result = service.login("admin", "secret");

        // Assert
        Assertions.assertEquals(AuthResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        Assertions.assertEquals(down.getStatus().getErrorMessage(), result.getMessage());
        Assertions.assertNull(service.currentUser());
    }

    // US1-R4
    @Test
    void anIoExceptionIsANetworkFailure() {
        // Arrange
        AuthService service = new AuthService(new RecordingConnection(new IOException("no route to host")));

        // Act
        AuthResult result = service.login("admin", "secret");

        // Assert
        Assertions.assertEquals(AuthResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        Assertions.assertEquals("no route to host", result.getMessage());
        Assertions.assertNull(service.currentUser());
    }

    // US1-R5
    @Test
    void logoutClearsTheCurrentUserAndLogsOutOfTheConnection() {
        // Arrange
        RecordingConnection connection = new RecordingConnection(new Status(1));
        AuthService service = new AuthService(connection);
        service.login("admin", "secret");

        // Act
        service.logout();

        // Assert
        Assertions.assertNull(service.currentUser());
        Assertions.assertEquals(1, connection.logouts);
    }

    // US1-R5
    @Test
    void nobodyIsSignedInBeforeLogin() {
        // Act and assert
        Assertions.assertNull(new AuthService(new FakeConnection()).currentUser());
    }

    // A Connection that records Login and Logout calls, or throws, which FakeConnection cannot do.
    private static final class RecordingConnection implements Connection {
        private final Status _status;
        private final IOException _failure;
        private final List<String> logins = new ArrayList<>();
        private int logouts;

        RecordingConnection(Status status) {
            _status = status;
            _failure = null;
        }

        RecordingConnection(IOException failure) {
            _status = null;
            _failure = failure;
        }

        @Override
        public Status Login(String username, String password) throws IOException {
            logins.add(username + "|" + password);
            if (_failure != null) {
                throw _failure;
            }
            return _status;
        }

        @Override
        public void Logout() {
            logouts++;
        }

        @Override
        public MessageResponse send(MessageRequest request) {
            throw new UnsupportedOperationException("login does not send messages");
        }

        @Override
        public boolean isConnected() {
            return true;
        }

        @Override
        public void close() {
        }
    }
}
