// AI-assisted: logs the MMM in and out over the mainframe Connection.
package lavs.logic;

import lavs.validation.ValidationResult;
import mainframe.client.Connection;
import lavs.data.MainframeGateway;
import mainframe.client.Status;
import mainframe.client.StatusMessage;


// US1: logs in and out. The token stays inside the Connection; this class only remembers
// who is signed in.
public class AuthService implements Authentication {
    private final MainframeGateway _gateway;
    // login runs on a background thread and the screens read this on the UI thread
    private volatile String _currentUser;

    public AuthService(MainframeGateway gateway) {
        _gateway = gateway;
    }

    public AuthService(Connection connection) {
        _gateway = new MainframeGateway(connection);
    }

    // Checks the form, then logs in. The username is trimmed before it is sent and the
    // password goes exactly as typed. A failed login leaves the current user unchanged.
    public AuthResult login(String username, String password) {
        ValidationResult validation = LoginForm.validate(username, password);
        if (!validation.isValid()) {
            return AuthResult.invalid(validation);
        }
        String trimmed = username.trim();
        Status status = _gateway.login(trimmed, password).getStatus();
        if (status.getWasSuccessful()) {
            _currentUser = trimmed;
            return AuthResult.success(trimmed);
        }
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            return AuthResult.networkFailure(status.getErrorMessage());
        }
        return AuthResult.rejected(status.getErrorMessage());
    }

    public void logout() {
        _gateway.logout();
        _currentUser = null;
    }

    // the signed-in user's name, or null before login and after logout
    public String currentUser() {
        return _currentUser;
    }
}
