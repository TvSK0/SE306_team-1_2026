// AI-assisted: searches for customers by name or number over the mainframe Connection.
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Customer;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.messages.customer.Search;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

// US2: finds existing customers so the MMM does not create a duplicate. Searching needs a
// connection, so a network failure is returned for the screen to report; nothing is queued.
// TEMPORARY: uses Connection directly until Scott's gateway lands; swap then.
public class CustomerSearchService {
    public static final String QUERY = "query";
    // the search screen shows ten rows at a time, in normal and select mode
    static final long PAGE_SIZE = 10;

    private final Connection _connection;

    public CustomerSearchService(Connection connection) {
        _connection = connection;
    }

    // the first page of matches
    public CustomerSearchResult search(String query) {
        return search(query, 1);
    }

    // Checks the query, then asks the mainframe (the query is trimmed before it is sent).
    // A blank query sends nothing.
    public CustomerSearchResult search(String query, long page) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(QUERY, query));
        if (!validation.isValid()) {
            return CustomerSearchResult.invalid(validation);
        }
        Search message = new Search(page, query.trim(), PAGE_SIZE);
        Status status;
        try {
            status = message.send(_connection);
        } catch (IOException ex) {
            return CustomerSearchResult.networkFailure(ex.getMessage());
        }
        if (status.getWasSuccessful()) {
            return CustomerSearchResult.success(toCustomers(message.getItems()), message.getPage(),
                    message.getTotal());
        }
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            return CustomerSearchResult.networkFailure(status.getErrorMessage());
        }
        return CustomerSearchResult.rejected(status.getErrorMessage());
    }

    private static List<Customer> toCustomers(Search.Item[] items) {
        List<Customer> customers = new ArrayList<>();
        for (Search.Item item : items) {
            Customer customer = new Customer();
            customer.setNumber(item.getNumber());
            customer.setTitle(item.getTitle());
            customer.setFirstName(item.getFirstName());
            customer.setFamilyName(item.getFamilyName());
            customer.setDateOfBirth(parseDate(item.getDateOfBirth()));
            customer.setStatus(item.getStatus());
            customers.add(customer);
        }
        return customers;
    }

    // the mainframe sends yyyy-MM-dd, or nothing when the date of birth is not known; a date
    // that cannot be read is treated as not known rather than failing the whole search
    private static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
