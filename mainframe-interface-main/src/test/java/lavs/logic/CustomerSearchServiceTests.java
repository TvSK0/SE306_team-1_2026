// AI-assisted: tests for CustomerSearchService, one per US2 requirement.
package lavs.logic;

import lavs.model.Customer;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerSearchServiceTests {
    private static final String SEARCH = "customer.search";

    // US2-R1
    @Test
    void returnsTheMatchingCustomersWithTheirDetails() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(SEARCH, FakeConnection.success(results(2, row("C-1", "Ms", "Jane", "Smith",
                "1990-04-12", "Active"), row("C-2", "Mr", "John", "Smythe", "1985-11-03", "Inactive"))));
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("smi");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(CustomerSearchResult.Outcome.SUCCESS, result.getOutcome());
        Assertions.assertEquals(2, result.getTotal());
        Assertions.assertEquals(1, result.getPage());
        Assertions.assertNull(result.getMessage());
        Assertions.assertTrue(result.getValidationErrors().isValid());
        Customer first = result.getCustomers().get(0);
        Assertions.assertEquals("C-1", first.getNumber());
        Assertions.assertEquals("Ms", first.getTitle());
        Assertions.assertEquals("Jane Smith", first.getFullName());
        Assertions.assertEquals(LocalDate.of(1990, 4, 12), first.getDateOfBirth());
        Assertions.assertEquals("Active", first.getStatus());
        Assertions.assertEquals("Inactive", result.getCustomers().get(1).getStatus());
    }

    // US2-R1
    @Test
    void sendsTheTrimmedQueryWithThePageAndTheTenRowPageSize() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(SEARCH, FakeConnection.success(results(25)));
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("  smith  ", 1);

        // Assert
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertEquals("smith", sent.getValue("search"));
        Assertions.assertEquals("1", sent.getValue("page"));
        Assertions.assertEquals("10", sent.getValue("size"));
        Assertions.assertEquals(25, result.getTotal());
    }

    // US2-R1
    @Test
    void asksForTheRequestedPage() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        Map<String, String> values = results(25);
        values.put("page", "2");
        connection.whenRequested(SEARCH, FakeConnection.success(values));
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("smith", 2);

        // Assert
        Assertions.assertEquals("2", connection.getSentRequests().get(0).getValue("page"));
        Assertions.assertEquals(2, result.getPage());
    }

    // US2-R1 (a search that finds nobody is a success with no rows; the screen shows the empty state, US2-R6)
    @Test
    void noMatchesIsASuccessWithNoCustomers() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(SEARCH, FakeConnection.success(results(0)));
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("nobody");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertTrue(result.getCustomers().isEmpty());
        Assertions.assertEquals(0, result.getTotal());
    }

    // US2-R1
    @Test
    void aMissingDateOfBirthIsLeftOutRatherThanFailingTheSearch() {
        // Arrange
        Map<String, String> noDate = row("C-1", "Ms", "Jane", "Smith", null, "Active");
        Map<String, String> blankDate = row("C-2", "Ms", "Ann", "Lee", "  ", "Active");
        Map<String, String> badDate = row("C-3", "Mr", "Bob", "Ray", "03/02/2001", "Active");
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(SEARCH, FakeConnection.success(results(3, noDate, blankDate, badDate)));
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("a");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(3, result.getCustomers().size());
        result.getCustomers().forEach(customer -> Assertions.assertNull(customer.getDateOfBirth()));
    }

    // US2-R4
    @Test
    void aBlankQuerySendsNothingAndFlagsTheField() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("   ");

        // Assert
        Assertions.assertEquals(CustomerSearchResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertFalse(result.isSuccessful());
        Assertions.assertNull(result.getMessage());
        Assertions.assertTrue(result.getCustomers().isEmpty());
        Assertions.assertEquals(List.of(CustomerSearchService.QUERY),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // US2-R4
    @Test
    void aMissingQuerySendsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act
        CustomerSearchResult result = new CustomerSearchService(connection).search(null);

        // Assert
        Assertions.assertEquals(CustomerSearchResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // US2-R5 (searching needs a connection; the screen disables Search and shows the offline banner)
    @Test
    void anUnreachableMainframeIsANetworkFailure() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("smith");

        // Assert
        Assertions.assertEquals(CustomerSearchResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        Assertions.assertEquals(FakeConnection.networkDown().getStatus().getErrorMessage(), result.getMessage());
        Assertions.assertTrue(result.getCustomers().isEmpty());
    }

    // US2-R1
    @Test
    void reportsTheMainframesReasonWhenTheSearchIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(SEARCH, FakeConnection.rejected("Search term too short"));
        CustomerSearchService service = new CustomerSearchService(connection);

        // Act
        CustomerSearchResult result = service.search("s");

        // Assert
        Assertions.assertEquals(CustomerSearchResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("Search term too short", result.getMessage());
        Assertions.assertTrue(result.getCustomers().isEmpty());
    }

    @SafeVarargs
    private static Map<String, String> results(long total, Map<String, String>... rows) {
        Map<String, String> values = new HashMap<>();
        values.put("page", "1");
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(rows.length));
        for (int i = 0; i < rows.length; i++) {
            String prefix = (i + 1) + ":";
            rows[i].forEach((key, value) -> {
                if (value != null) {
                    values.put(prefix + key, value);
                }
            });
        }
        return values;
    }

    private static Map<String, String> row(String number, String title, String firstName, String familyName,
                                           String dateOfBirth, String status) {
        Map<String, String> row = new HashMap<>();
        row.put("number", number);
        row.put("title", title);
        row.put("firstName", firstName);
        row.put("familyName", familyName);
        row.put("dateOfBirth", dateOfBirth);
        row.put("status", status);
        return row;
    }
}
