// AI-assisted: tests for CustomerContactService, one per US2 select-mode requirement.
package lavs.logic;

import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomerContactServiceTests {
    private static final String PHONES = "customerPhoneNumber.list";
    private static final String EMAILS = "customerEmail.list";

    // US2-S4
    @Test
    void aCustomerWithAPhoneIsCompleteAndTheEmailIsNotAsked() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(PHONES, FakeConnection.success(total(1)));
        CustomerContactService service = new CustomerContactService(connection);

        // Act
        CustomerContactService.Check check = service.check("C-1");

        // Assert
        Assertions.assertEquals(CustomerContactService.Check.COMPLETE, check);
        Assertions.assertEquals(1, connection.getSentRequests().size());
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertEquals("C-1", sent.getValue("number"));
        Assertions.assertEquals("1", sent.getValue("page"));
        Assertions.assertEquals("1", sent.getValue("size"));
    }

    // US2-S4
    @Test
    void aCustomerWithOnlyAnEmailIsComplete() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(PHONES, FakeConnection.success(total(0)));
        connection.whenRequested(EMAILS, FakeConnection.success(total(2)));
        CustomerContactService service = new CustomerContactService(connection);

        // Act and Assert
        Assertions.assertEquals(CustomerContactService.Check.COMPLETE, service.check("C-1"));
        Assertions.assertEquals(2, connection.getSentRequests().size());
    }

    // US2-S4
    @Test
    void aCustomerWithNeitherIsMissing() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(PHONES, FakeConnection.success(total(0)));
        connection.whenRequested(EMAILS, FakeConnection.success(total(0)));

        // Act and Assert
        Assertions.assertEquals(CustomerContactService.Check.MISSING,
                new CustomerContactService(connection).check("C-1"));
    }

    // US2-S4
    @Test
    void aRefusedPhoneListIsFailedNotAGuess() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(PHONES, FakeConnection.rejected("No such customer"));

        // Act and Assert
        Assertions.assertEquals(CustomerContactService.Check.FAILED,
                new CustomerContactService(connection).check("C-1"));
    }

    // US2-S4
    @Test
    void aRefusedEmailListIsFailed() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(PHONES, FakeConnection.success(total(0)));
        connection.whenRequested(EMAILS, FakeConnection.rejected("No such customer"));

        // Act and Assert
        Assertions.assertEquals(CustomerContactService.Check.FAILED,
                new CustomerContactService(connection).check("C-1"));
    }

    // US2-S4
    @Test
    void anUnreachableMainframeIsFailed() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act and Assert
        Assertions.assertEquals(CustomerContactService.Check.FAILED,
                new CustomerContactService(connection).check("C-1"));
    }

    // US2-S4 (the starter list message throws when an item has no id; that must not escape)
    @Test
    void aReplyTheStarterMessageCannotReadIsFailed() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(PHONES, FakeConnection.success(Map.of("total", "1", "items", "1")));

        // Act and Assert
        Assertions.assertEquals(CustomerContactService.Check.FAILED,
                new CustomerContactService(connection).check("C-1"));
    }

    // US2-S4
    @Test
    void checkAllReportsEachCustomerInOrder() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(PHONES, Map.of("number", "C-1"), FakeConnection.success(total(1)));
        connection.whenRequested(PHONES, Map.of("number", "C-2"), FakeConnection.success(total(0)));
        connection.whenRequested(EMAILS, Map.of("number", "C-2"), FakeConnection.success(total(0)));
        List<String> reported = new ArrayList<>();

        // Act
        new CustomerContactService(connection).checkAll(List.of("C-1", "C-2"),
                (number, check) -> reported.add(number + "=" + check));

        // Assert
        Assertions.assertEquals(List.of("C-1=COMPLETE", "C-2=MISSING"), reported);
    }

    // a list reply that says how many there are, with no items to read
    private static Map<String, String> total(long total) {
        return Map.of("total", Long.toString(total), "items", "0");
    }
}
