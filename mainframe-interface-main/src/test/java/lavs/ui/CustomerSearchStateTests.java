// AI-assisted: tests for what the customer search screen shows, one per US2 requirement.
package lavs.ui;

import lavs.logic.CustomerSearchResult;
import lavs.logic.CustomerSearchService;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerSearchStateTests {
    // US2-R6 (nothing is shown until a search has worked)
    @Test
    void startsWithNoResultsShown() {
        // Act
        CustomerSearchState state = new CustomerSearchState();

        // Assert
        Assertions.assertFalse(state.hasSearched());
        Assertions.assertTrue(state.customers().isEmpty());
        Assertions.assertFalse(state.canShowMore());
    }

    // US2-R1
    @Test
    void aSearchShowsTheRowsAndTheTotal() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();

        // Act
        CustomerSearchState.Display display = state.applySearch(" smith ", found(" smith ", 1, 2, 1, 2));

        // Assert
        Assertions.assertTrue(display.fieldErrors().isValid());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertTrue(state.hasSearched());
        Assertions.assertEquals(List.of("C-1", "C-2"), numbers(state));
        Assertions.assertEquals("2 customers found", state.countLine());
        Assertions.assertEquals("smith", state.query());
        Assertions.assertFalse(state.canShowMore());
        Assertions.assertEquals(2, display.newRows().orElseThrow().size());
    }

    // US2-R6
    @Test
    void aSearchWithNoMatchesShowsZeroFoundAndTheEmptyTextWithTheTrimmedQuery() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();

        // Act
        state.applySearch("  zed ", found("  zed ", 1, 0));

        // Assert
        Assertions.assertTrue(state.hasSearched());
        Assertions.assertEquals("0 customers found", state.countLine());
        Assertions.assertEquals("No customers match \"zed\". Check the spelling, or add a new customer.",
                state.emptyText());
    }

    // US2-R8
    @Test
    void whenThereAreMoreMatchesThanRowsShowMoreOffersTheNextPage() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();

        // Act
        state.applySearch("smith", found("smith", 1, 25, 1, 10));

        // Assert
        Assertions.assertEquals("25 customers found", state.countLine());
        Assertions.assertTrue(state.canShowMore());
        Assertions.assertEquals("Show more (10 of 25 shown)", state.showMoreLabel());
        Assertions.assertEquals(2, state.nextPage());
    }

    // US2-R8
    @Test
    void showMoreAppendsTheNextPageUnderTheRowsAlreadyShown() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();
        state.applySearch("smith", found("smith", 1, 25, 1, 10));

        // Act
        CustomerSearchState.More more = state.applyMore(found("smith", 2, 25, 11, 10));

        // Assert
        Assertions.assertTrue(more.banner().isEmpty());
        Assertions.assertEquals(10, more.added().size());
        Assertions.assertEquals(20, state.customers().size());
        Assertions.assertEquals("C-1", state.customers().get(0).getNumber());
        Assertions.assertEquals("C-20", state.customers().get(19).getNumber());
        Assertions.assertEquals("Show more (20 of 25 shown)", state.showMoreLabel());
        Assertions.assertEquals(3, state.nextPage());
        Assertions.assertTrue(state.canShowMore());
    }

    // US2-R8
    @Test
    void showMoreDisappearsOnceEveryMatchIsShown() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();
        state.applySearch("smith", found("smith", 1, 12, 1, 10));

        // Act
        state.applyMore(found("smith", 2, 12, 11, 2));

        // Assert
        Assertions.assertEquals(12, state.customers().size());
        Assertions.assertFalse(state.canShowMore());
    }

    // US2-R8 (a wrong total must not leave a button that does nothing)
    @Test
    void anEmptyPageEndsTheListEvenIfTheTotalSaysThereAreMore() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();
        state.applySearch("smith", found("smith", 1, 25, 1, 10));

        // Act
        state.applyMore(found("smith", 2, 25, 11, 0));

        // Assert
        Assertions.assertEquals(10, state.customers().size());
        Assertions.assertFalse(state.canShowMore());
        Assertions.assertEquals("10 customers found", state.countLine());
    }

    // US2-R8
    @Test
    void aNewSearchReplacesTheRowsAndGoesBackToPageOne() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();
        state.applySearch("smith", found("smith", 1, 25, 1, 10));
        state.applyMore(found("smith", 2, 25, 11, 10));

        // Act
        state.applySearch("jones", found("jones", 1, 15, 101, 10));

        // Assert
        Assertions.assertEquals(10, state.customers().size());
        Assertions.assertEquals("C-101", state.customers().get(0).getNumber());
        Assertions.assertEquals("jones", state.query());
        Assertions.assertEquals(2, state.nextPage());
        Assertions.assertEquals("15 customers found", state.countLine());
    }

    // US2-R4
    @Test
    void aBlankQueryShowsAnInlineFieldErrorAndNoBanner() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();

        // Act
        CustomerSearchState.Display display = state.applySearch("  ", invalid());

        // Assert
        Assertions.assertEquals(List.of(CustomerSearchService.QUERY),
                display.fieldErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertFalse(state.hasSearched());
    }

    // US2-R5
    @Test
    void aNetworkFailureShowsTheOfflineBannerAndKeepsTheRows() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();
        state.applySearch("smith", found("smith", 1, 2, 1, 2));

        // Act
        CustomerSearchState.Display display = state.applySearch("jones", networkDown());

        // Assert
        Assertions.assertEquals(BannerContent.offline(), display.banner().orElseThrow());
        Assertions.assertTrue(display.fieldErrors().isValid());
        Assertions.assertTrue(display.newRows().isEmpty());
        Assertions.assertEquals(List.of("C-1", "C-2"), numbers(state));
        Assertions.assertEquals("smith", state.query());
    }

    // US2-R1
    @Test
    void aRefusedSearchShowsTheMainframesReasonAndKeepsTheRows() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();
        state.applySearch("smith", found("smith", 1, 2, 1, 2));

        // Act
        CustomerSearchState.Display display = state.applySearch("s", rejected("Search term too short"));

        // Assert
        Assertions.assertEquals(CustomerSearchText.rejected("Search term too short"), display.banner().orElseThrow());
        Assertions.assertEquals(2, state.customers().size());
    }

    // US2-R8
    @Test
    void aFailedShowMoreShowsABannerAndChangesNothing() {
        // Arrange
        CustomerSearchState state = new CustomerSearchState();
        state.applySearch("smith", found("smith", 1, 25, 1, 10));

        // Act
        CustomerSearchState.More more = state.applyMore(networkDown());

        // Assert
        Assertions.assertEquals(BannerContent.offline(), more.banner().orElseThrow());
        Assertions.assertTrue(more.added().isEmpty());
        Assertions.assertEquals(10, state.customers().size());
        Assertions.assertEquals(2, state.nextPage());
        Assertions.assertTrue(state.canShowMore());
    }

    private static List<String> numbers(CustomerSearchState state) {
        return state.customers().stream().map(customer -> customer.getNumber()).toList();
    }

    // the AuthResult-style factories are not public, so each result comes from the real service
    private static CustomerSearchResult found(String query, long page, long total, int firstNumber, int rows) {
        Map<String, String> values = new HashMap<>();
        values.put("page", Long.toString(page));
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(rows));
        for (int i = 0; i < rows; i++) {
            String prefix = (i + 1) + ":";
            values.put(prefix + "number", "C-" + (firstNumber + i));
            values.put(prefix + "firstName", "First" + (firstNumber + i));
            values.put(prefix + "familyName", "Family");
            values.put(prefix + "status", "Active");
        }
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.search", FakeConnection.success(values));
        return new CustomerSearchService(connection).search(query, page);
    }

    private static CustomerSearchResult found(String query, long page, long total) {
        return found(query, page, total, 1, 0);
    }

    private static CustomerSearchResult invalid() {
        return new CustomerSearchService(new FakeConnection()).search("  ");
    }

    private static CustomerSearchResult networkDown() {
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        return new CustomerSearchService(connection).search("x");
    }

    private static CustomerSearchResult rejected(String message) {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.search", FakeConnection.rejected(message));
        return new CustomerSearchService(connection).search("x");
    }
}
