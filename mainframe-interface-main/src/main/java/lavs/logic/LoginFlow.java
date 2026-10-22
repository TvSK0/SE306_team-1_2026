// AI-assisted: logs in, then refreshes the dropdown values, as the login screen needs both.
package lavs.logic;

import lavs.data.ConstantsLoadResult;

// US1: what the Log in button does. Runs in the background, so it never touches JavaFX.
// Yu's rule for the constants: values saved from last run are good enough to carry on with,
// so a failed refresh only blocks the login when nothing is saved either.
public class LoginFlow {
    private final AuthService _auth;
    private final ConstantsService _constants;

    public LoginFlow(AuthService auth, ConstantsService constants) {
        _auth = auth;
        _constants = constants;
    }

    public LoginAttempt run(String username, String password) {
        AuthResult auth = _auth.login(username, password);
        if (!auth.isSuccessful()) {
            return LoginAttempt.loginOnly(auth);
        }
        ConstantsLoadResult constants = _constants.refresh();
        boolean available = constants.getOutcome() == ConstantsLoadResult.Outcome.SUCCESS || _constants.isLoaded();
        if (!available) {
            // the screen stays on Login, so nobody should be left signed in behind it
            _auth.logout();
        }
        return new LoginAttempt(auth, available, constants.getMessage());
    }
}
