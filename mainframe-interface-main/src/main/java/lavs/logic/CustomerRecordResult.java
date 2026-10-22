// AI-assisted: the outcome of loading one customer (success, network failure, rejected or invalid).
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Customer;
import lavs.validation.ValidationResult;

// The outcome of CustomerRecordService.load: INVALID when no customer number was given and
// nothing was sent, or one of the three outcomes every service result has. It carries no UI
// text (the customer record screen picks the copy).
// TEMPORARY: swap this for Scott's gateway result type once it exists.
public final class CustomerRecordResult {
    // REJECTED is only for the mainframe's refusals; INVALID means nothing was sent
    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED, INVALID }

    private final Outcome _outcome;
    private final Customer _customer;
    private final String _message;
    private final ValidationResult _validationErrors;

    private CustomerRecordResult(Outcome outcome, Customer customer, String message,
                                 ValidationResult validationErrors) {
        _outcome = outcome;
        _customer = customer;
        _message = message;
        _validationErrors = validationErrors;
    }

    static CustomerRecordResult success(Customer customer) {
        return new CustomerRecordResult(Outcome.SUCCESS, customer, null, new ValidationResult());
    }

    static CustomerRecordResult networkFailure(String message) {
        return new CustomerRecordResult(Outcome.NETWORK_FAILURE, null, message, new ValidationResult());
    }

    static CustomerRecordResult rejected(String message) {
        return new CustomerRecordResult(Outcome.REJECTED, null, message, new ValidationResult());
    }

    static CustomerRecordResult invalid(ValidationResult validationErrors) {
        return new CustomerRecordResult(Outcome.INVALID, null, null, validationErrors);
    }

    public boolean isSuccessful() {
        return _outcome == Outcome.SUCCESS;
    }

    public Outcome getOutcome() {
        return _outcome;
    }

    // the customer as the mainframe holds it; null unless SUCCESS
    public Customer getCustomer() {
        return _customer;
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
