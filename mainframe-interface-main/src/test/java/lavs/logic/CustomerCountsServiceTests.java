// AI-assisted: tests for CustomerCountsService, one per customer record requirement.
package lavs.logic;

import lavs.logic.CustomerCountsService.Kind;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;

public class CustomerCountsServiceTests {
    private static final Map<Kind, String> REQUESTS = Map.of(
            Kind.ADDRESSES, "customerAddress.list",
            Kind.PHONE_NUMBERS, "customerPhoneNumber.list",
            Kind.EMAILS, "customerEmail.list",
            Kind.EMPLOYERS, "customerEmployer.list",
            Kind.RELATIONSHIPS, "customerRelationship.list",
            Kind.NOTES, "customerNote.list",
            Kind.DOCUMENTS, "customerDocument.list",
            Kind.LOANS, "loan.list");

    // CR-R5
    @Test
    void reportsEverySectionsCountInOrderFromItsTotal() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        long total = 1;
        for (Kind kind : Kind.values()) {
            connection.whenRequested(REQUESTS.get(kind), FakeConnection.success(total(total++)));
        }
        Map<Kind, OptionalLong> counts = new LinkedHashMap<>();

        // Act
        new CustomerCountsService(connection).fetchCounts("C-1", counts::put);

        // Assert
        Assertions.assertEquals(List.of(Kind.values()), new ArrayList<>(counts.keySet()));
        Assertions.assertEquals(OptionalLong.of(1), counts.get(Kind.ADDRESSES));
        Assertions.assertEquals(OptionalLong.of(4), counts.get(Kind.EMPLOYERS));
        Assertions.assertEquals(OptionalLong.of(8), counts.get(Kind.LOANS));
    }

    // CR-R5
    @Test
    void asksForOneItemOfTheFirstPageOfEachList() {
        // Arrange
        FakeConnection connection = scripted(0);

        // Act
        new CustomerCountsService(connection).fetchCounts("C-1", (kind, count) -> { });

        // Assert
        Assertions.assertEquals(8, connection.getSentRequests().size());
        for (MessageRequest sent : connection.getSentRequests()) {
            Assertions.assertEquals("C-1", sent.getValue("number"));
            Assertions.assertEquals("1", sent.getValue("page"));
            Assertions.assertEquals("1", sent.getValue("size"));
        }
    }

    // CR-R5
    @Test
    void aCustomerWithNoneOfSomethingHasACountOfZero() {
        // Arrange
        Map<Kind, OptionalLong> counts = new LinkedHashMap<>();

        // Act
        new CustomerCountsService(scripted(0)).fetchCounts("C-1", counts::put);

        // Assert
        Assertions.assertEquals(OptionalLong.of(0), counts.get(Kind.NOTES));
    }

    // CR-R5 (a count that could not be read is empty, never a guess)
    @Test
    void aRefusedListHasNoCountButTheOthersStillDo() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        for (Kind kind : Kind.values()) {
            connection.whenRequested(REQUESTS.get(kind), kind == Kind.NOTES
                    ? FakeConnection.rejected("No such customer")
                    : FakeConnection.success(total(2)));
        }
        Map<Kind, OptionalLong> counts = new LinkedHashMap<>();

        // Act
        new CustomerCountsService(connection).fetchCounts("C-1", counts::put);

        // Assert
        Assertions.assertEquals(OptionalLong.empty(), counts.get(Kind.NOTES));
        Assertions.assertEquals(OptionalLong.of(2), counts.get(Kind.LOANS));
    }

    // CR-R5
    @Test
    void anUnreachableMainframeGivesNoCounts() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        Map<Kind, OptionalLong> counts = new LinkedHashMap<>();

        // Act
        new CustomerCountsService(connection).fetchCounts("C-1", counts::put);

        // Assert
        Assertions.assertEquals(8, counts.size());
        counts.values().forEach(count -> Assertions.assertTrue(count.isEmpty()));
    }

    // CR-R5 (the starter list message throws when an item has no id; that must not escape)
    @Test
    void aReplyTheStarterMessageCannotReadHasNoCount() {
        // Arrange
        // the first rule that matches wins, so the unreadable reply goes in before the others
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerAddress.list", FakeConnection.success(Map.of("total", "1", "items", "1")));
        for (Kind kind : Kind.values()) {
            connection.whenRequested(REQUESTS.get(kind), FakeConnection.success(total(0)));
        }
        Map<Kind, OptionalLong> counts = new LinkedHashMap<>();

        // Act
        new CustomerCountsService(connection).fetchCounts("C-1", counts::put);

        // Assert
        Assertions.assertEquals(OptionalLong.empty(), counts.get(Kind.ADDRESSES));
        Assertions.assertEquals(OptionalLong.of(0), counts.get(Kind.EMAILS));
    }

    private static FakeConnection scripted(long total) {
        FakeConnection connection = new FakeConnection();
        for (Kind kind : Kind.values()) {
            connection.whenRequested(REQUESTS.get(kind), FakeConnection.success(total(total)));
        }
        return connection;
    }

    // a list reply that says how many there are, with no items to read
    private static Map<String, String> total(long total) {
        return Map.of("total", Long.toString(total), "items", "0");
    }
}
