// AI-assisted: logs the MMM in and out over the mainframe Connection.
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.validation.ValidationResult;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;

// US1: logs in and out. The token stays inside the Connection; this class only remembers
// who is signed in.
// TEMPORARY: uses Connection directly until Scott's gateway lands; swap then.
public class AuthService {
    private final Connection _connection;
    // login runs on a background thread and the screens read this on the UI thread
    private volatile String _currentUser;

    public AuthService(Connection connection) {
        _connection = connection;
    }

    // Checks the form, then logs in. The username is trimmed before it is sent and the
    // password goes exactly as typed. A failed login leaves the current user unchanged.
    public AuthResult login(String username, String password) {
        ValidationResult validation = LoginForm.validate(username, password);
        if (!validation.isValid()) {
            return AuthResult.invalid(validation);
        }
        String trimmed = username.trim();
        Status status;
        try {
            status = _connection.Login(trimmed, password);
        } catch (IOException ex) {
            return AuthResult.networkFailure(ex.getMessage());
        }
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
        _connection.Logout();
        _currentUser = null;
    }

    // the signed-in user's name, or null before login and after logout
    public String currentUser() {
        return _currentUser;
    }
}
