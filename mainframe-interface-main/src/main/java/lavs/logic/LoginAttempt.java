// AI-assisted: what one press of Log in produced: the login result and whether the dropdown values are ready.
package lavs.logic;

// The result of LoginFlow.run. constantsAvailable and constantsMessage only mean something
// when the login itself worked; otherwise they are true and null.
public record LoginAttempt(AuthResult auth, boolean constantsAvailable, String constantsMessage) {
    static LoginAttempt loginOnly(AuthResult auth) {
        return new LoginAttempt(auth, true, null);
    }
}
