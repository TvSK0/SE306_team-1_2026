// AI-assisted: the outcome of a customer search (success, network failure, rejected or invalid).
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Customer;
import lavs.validation.ValidationResult;

import java.util.List;

// The outcome of CustomerSearchService.search: INVALID when the query was blank and nothing
// was sent, or one of the three outcomes every service result has. It carries no UI text
// (the search screen picks the copy).
// TEMPORARY: swap this for Scott's gateway result type once it exists.
public final class CustomerSearchResult {
    // REJECTED is only for the mainframe's refusals; INVALID means nothing was sent
    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED, INVALID }

    private final Outcome _outcome;
    private final List<Customer> _customers;
    private final long _page;
    private final long _total;
    private final String _message;
    private final ValidationResult _validationErrors;

    private CustomerSearchResult(Outcome outcome, List<Customer> customers, long page, long total, String message,
                                 ValidationResult validationErrors) {
        _outcome = outcome;
        _customers = List.copyOf(customers);
        _page = page;
        _total = total;
        _message = message;
        _validationErrors = validationErrors;
    }

    static CustomerSearchResult success(List<Customer> customers, long page, long total) {
        return new CustomerSearchResult(Outcome.SUCCESS, customers, page, total, null, new ValidationResult());
    }

    static CustomerSearchResult networkFailure(String message) {
        return new CustomerSearchResult(Outcome.NETWORK_FAILURE, List.of(), 0, 0, message, new ValidationResult());
    }

    static CustomerSearchResult rejected(String message) {
        return new CustomerSearchResult(Outcome.REJECTED, List.of(), 0, 0, message, new ValidationResult());
    }

    // nothing was sent: the outcome is INVALID with no message, and the errors say which field
    static CustomerSearchResult invalid(ValidationResult validationErrors) {
        return new CustomerSearchResult(Outcome.INVALID, List.of(), 0, 0, null, validationErrors);
    }

    public boolean isSuccessful() {
        return _outcome == Outcome.SUCCESS;
    }

    public Outcome getOutcome() {
        return _outcome;
    }

    // this page of matches in the order the mainframe returned them; empty unless SUCCESS.
    // Each Customer has number, title, names, date of birth and status (nothing else is known).
    public List<Customer> getCustomers() {
        return _customers;
    }

    public long getPage() {
        return _page;
    }

    // how many customers match in all, across every page ("N customers found")
    public long getTotal() {
        return _total;
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
