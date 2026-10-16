// TEMPORARY: swap this package for Scott's data package once his skeleton is on main.
package lavs.data;

import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

// TEMPORARY: once the loader uses Scott's gateway, swap FakeConnection here for
// whatever fake the gateway takes (or keep FakeConnection under a real gateway).
public class CategoryConstantsLoaderTests {
    @Test
    void loadsEveryCategoryWithItsValues() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanRateType", "LoanStatus"));
        connection.whenRequested("constantItem.list", Map.of("category", "1"),
                FakeConnection.list("Fixed", "Floating", "InterestOnly"));
        connection.whenRequested("constantItem.list", Map.of("category", "2"),
                FakeConnection.list("New", "Pending"));

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection).load();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.SUCCESS, result.getOutcome()),
                () -> Assertions.assertEquals(Map.of(
                        "LoanRateType", List.of("Fixed", "Floating", "InterestOnly"),
                        "LoanStatus", List.of("New", "Pending")), result.getValues())
        );
    }

    @Test
    void sortsValuesByOrder() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanPaymentFrequency"));
        connection.whenRequested("constantItem.list", FakeConnection.success(Map.of(
                "total", "3",
                "items", "3",
                "1:name", "Monthly", "1:order", "3",
                "2:name", "Weekly", "2:order", "1",
                "3:name", "Fortnightly", "3:order", "2")));

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection).load();

        // Assert
        Assertions.assertEquals(
                List.of("Weekly", "Fortnightly", "Monthly"),
                result.getValues().get("LoanPaymentFrequency"));
    }

    @Test
    void pagesThroughCategories() {
        // Arrange: 3 categories, 2 per page
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", Map.of("page", "1"), FakeConnection.list(3, 1, "A", "B"));
        connection.whenRequested("constantCategory.list", Map.of("page", "2"), FakeConnection.list(3, 3, "C"));
        connection.whenRequested("constantItem.list", FakeConnection.list("Value"));

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection, 2).load();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.SUCCESS, result.getOutcome()),
                () -> Assertions.assertEquals(3, result.getValues().size()),
                () -> Assertions.assertTrue(result.getValues().keySet().containsAll(List.of("A", "B", "C"))),
                () -> Assertions.assertEquals(2, countRequests(connection, "constantCategory.list"))
        );
    }

    @Test
    void pagesThroughItems() {
        // Arrange: 3 values in one category, 2 per page
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanPaymentFrequency"));
        connection.whenRequested("constantItem.list", Map.of("page", "1"),
                FakeConnection.list(3, 1, "Weekly", "Fortnightly"));
        connection.whenRequested("constantItem.list", Map.of("page", "2"),
                FakeConnection.list(3, 3, "Monthly"));

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection, 2).load();

        // Assert
        Assertions.assertEquals(
                List.of("Weekly", "Fortnightly", "Monthly"),
                result.getValues().get("LoanPaymentFrequency"));
    }

    @Test
    void mapsEmptyCategoryToEmptyList() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("CustomerStatus"));
        connection.whenRequested("constantItem.list", FakeConnection.list());

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection).load();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.SUCCESS, result.getOutcome()),
                () -> Assertions.assertEquals(List.of(), result.getValues().get("CustomerStatus"))
        );
    }

    @Test
    void reportsRejectionWithServerMessage() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.rejected("Invalid page size"));

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection).load();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.REJECTED, result.getOutcome()),
                () -> Assertions.assertEquals("Invalid page size", result.getMessage()),
                () -> Assertions.assertTrue(result.getValues().isEmpty())
        );
    }

    @Test
    void reportsNetworkFailure() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.networkDown());

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection).load();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.NETWORK_FAILURE, result.getOutcome()),
                () -> Assertions.assertTrue(result.getValues().isEmpty())
        );
    }

    @Test
    void returnsNothingWhenOneCategoryFails() {
        // Arrange: the first category loads, the second loses the connection
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanRateType", "LoanStatus"));
        connection.whenRequested("constantItem.list", Map.of("category", "1"), FakeConnection.list("Fixed"));
        connection.whenRequested("constantItem.list", Map.of("category", "2"), FakeConnection.networkDown());

        // Act
        ConstantsLoadResult result = new CategoryConstantsLoader(connection).load();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.NETWORK_FAILURE, result.getOutcome()),
                () -> Assertions.assertTrue(result.getValues().isEmpty())
        );
    }

    private static long countRequests(FakeConnection connection, String requestType) {
        return connection.getSentRequests().stream()
                .map(MessageRequest::getRequestType)
                .filter(requestType::equals)
                .count();
    }
}
