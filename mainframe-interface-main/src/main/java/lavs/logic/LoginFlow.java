// AI-assisted: logs in, then refreshes the dropdown values, as the login screen needs both.
package lavs.logic;

import lavs.data.ConstantsLoadResult;

// US1: what the Log in button does. Runs in the background, so it never touches JavaFX.
// Yu's rule for the constants: values saved from last run are good enough to carry on with,
// so a failed refresh only blocks the login when nothing is saved either.
public class LoginFlow {
    private final Authentication _auth;
    private final CategoryValues _constants;
    private java.util.function.Consumer<String> onReady = user -> {};

    public void setOnReady(java.util.function.Consumer<String> callback) { onReady = callback; }

    public LoginFlow(Authentication auth, CategoryValues constants) {
        _auth = auth;
        _constants = constants;
    }

    public LoginAttempt run(String username, String password) {
        AuthResult auth = _auth.login(username, password);
        if (!auth.isSuccessful()) {
            return LoginAttempt.loginOnly(auth);
        }
        _constants.loadSaved();
        ConstantsLoadResult constants = _constants.refresh();
        boolean available = constants.getOutcome() == ConstantsLoadResult.Outcome.SUCCESS || _constants.isLoaded();
        if (!available) {
            // the screen stays on Login, so nobody should be left signed in behind it
            _auth.logout();
        }
        if (available) {
            try { onReady.accept(_auth.currentUser()); }
            catch (RuntimeException error) { _auth.logout(); throw error; }
        }
        return new LoginAttempt(auth, available, constants.getMessage());
    }
}
