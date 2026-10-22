// AI-assisted: the outcome of a login attempt (success, network failure, rejected or invalid).
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.validation.ValidationResult;

// The outcome of AuthService.login: INVALID when the form was incomplete and nothing was sent,
// or one of the three outcomes every service result has. It carries no UI text (the login
// screen picks the copy) and no token (that stays inside Connection).
// TEMPORARY: swap this for Scott's gateway result type once it exists.
public final class AuthResult {
    // REJECTED is only for the mainframe's refusals; INVALID means nothing was sent
    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED, INVALID }

    private final Outcome _outcome;
    private final String _userName;
    private final String _message;
    private final ValidationResult _validationErrors;

    private AuthResult(Outcome outcome, String userName, String message, ValidationResult validationErrors) {
        _outcome = outcome;
        _userName = userName;
        _message = message;
        _validationErrors = validationErrors;
    }

    static AuthResult success(String userName) {
        return new AuthResult(Outcome.SUCCESS, userName, null, new ValidationResult());
    }

    static AuthResult networkFailure(String message) {
        return new AuthResult(Outcome.NETWORK_FAILURE, null, message, new ValidationResult());
    }

    static AuthResult rejected(String message) {
        return new AuthResult(Outcome.REJECTED, null, message, new ValidationResult());
    }

    // nothing was sent: the outcome is INVALID with no message, and the errors say which field
    static AuthResult invalid(ValidationResult validationErrors) {
        return new AuthResult(Outcome.INVALID, null, null, validationErrors);
    }

    // True only when the mainframe accepted the login (an INVALID form never reaches it).
    public boolean isSuccessful() {
        return _outcome == Outcome.SUCCESS;
    }

    public Outcome getOutcome() {
        return _outcome;
    }

    // the signed-in user's name; null unless the login worked
    public String getUserName() {
        return _userName;
    }

    // the mainframe's message for REJECTED, or the reason for NETWORK_FAILURE; null otherwise (including INVALID)
    public String getMessage() {
        return _message;
    }

    // empty unless the outcome is INVALID
    public ValidationResult getValidationErrors() {
        return _validationErrors;
    }
}
