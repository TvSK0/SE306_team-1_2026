// AI-assisted: the outcome of reading something from the mainframe (success, network failure, rejected or invalid).
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.validation.ValidationResult;

// The same shape as AuthResult, CustomerSearchResult and CustomerRecordResult, for any value
// that is read or saved:
// INVALID when the input was blank and nothing was sent, or one of the three outcomes every
// service result has. It carries no UI text (the screen picks the copy).
// TEMPORARY: swap this for Scott's gateway result type once it exists.
public final class LookupResult<T> {
    // REJECTED is only for the mainframe's refusals; INVALID means nothing was sent
    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED, INVALID }

    private final Outcome _outcome;
    private final T _value;
    private final String _message;
    private final ValidationResult _validationErrors;

    private LookupResult(Outcome outcome, T value, String message, ValidationResult validationErrors) {
        _outcome = outcome;
        _value = value;
        _message = message;
        _validationErrors = validationErrors;
    }

    static <T> LookupResult<T> success(T value) {
        return new LookupResult<>(Outcome.SUCCESS, value, null, new ValidationResult());
    }

    static <T> LookupResult<T> networkFailure(String message) {
        return new LookupResult<>(Outcome.NETWORK_FAILURE, null, message, new ValidationResult());
    }

    static <T> LookupResult<T> rejected(String message) {
        return new LookupResult<>(Outcome.REJECTED, null, message, new ValidationResult());
    }

    static <T> LookupResult<T> invalid(ValidationResult validationErrors) {
        return new LookupResult<>(Outcome.INVALID, null, null, validationErrors);
    }

    public boolean isSuccessful() {
        return _outcome == Outcome.SUCCESS;
    }

    public Outcome getOutcome() {
        return _outcome;
    }

    // what was read; null unless SUCCESS
    public T getValue() {
        return _value;
    }

    // the mainframe's message for REJECTED, or the reason for NETWORK_FAILURE; null otherwise
    public String getMessage() {
        return _message;
    }

    // empty unless the outcome is INVALID
    public ValidationResult getValidationErrors() {
        return _validationErrors;
    }
}
