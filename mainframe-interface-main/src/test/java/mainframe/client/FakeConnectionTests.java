package mainframe.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FakeConnectionTests {
    @Test
    void repliesWithScriptedListResponse() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanRateType", "LoanStatus"));

        // Act
        var message = new mainframe.client.messages.constantCategory.List(1, 20);
        Status status = message.send(connection);

        // Assert
        var items = message.getItems();
        Assertions.assertAll(
                () -> Assertions.assertTrue(status.getWasSuccessful()),
                () -> Assertions.assertEquals(2, message.getTotal()),
                () -> Assertions.assertEquals("LoanRateType", items[0].getName()),
                () -> Assertions.assertEquals(1, items[0].getId()),
                () -> Assertions.assertEquals("LoanStatus", items[1].getName()),
                () -> Assertions.assertEquals(2, items[1].getId())
        );
    }

    @Test
    void choosesResponseByRequestValues() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantItem.list", Map.of("category", "7"), FakeConnection.list("Fixed", "Floating"));
        connection.whenRequested("constantItem.list", Map.of("category", "9"), FakeConnection.list("Weekly"));

        // Act
        var rateTypes = new mainframe.client.messages.constantItem.List(7, 1, 20);
        rateTypes.send(connection);
        var frequencies = new mainframe.client.messages.constantItem.List(9, 1, 20);
        frequencies.send(connection);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(2, rateTypes.getItems().length),
                () -> Assertions.assertEquals("Fixed", rateTypes.getItems()[0].getName()),
                () -> Assertions.assertEquals(1, rateTypes.getItems()[0].getOrder()),
                () -> Assertions.assertEquals(1, frequencies.getItems().length),
                () -> Assertions.assertEquals("Weekly", frequencies.getItems()[0].getName())
        );
    }

    @Test
    void recordsSentRequests() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list(3, 1));

        // Act
        new mainframe.client.messages.constantCategory.List(2, 20).send(connection);

        // Assert
        var sent = connection.getSentRequests();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, sent.size()),
                () -> Assertions.assertEquals("constantCategory.list", sent.get(0).getRequestType()),
                () -> Assertions.assertEquals("2", sent.get(0).getValue("page"))
        );
    }

    @Test
    void returnsRejection() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.rejected("Invalid page"));

        // Act
        Status status = new mainframe.client.messages.constantCategory.List(1, 20).send(connection);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(status.getWasSuccessful()),
                () -> Assertions.assertEquals(StatusMessage.SERVER_FAILURE.getCode(), status.getErrorCode()),
                () -> Assertions.assertEquals("Invalid page", status.getErrorMessage())
        );
    }

    @Test
    void returnsNetworkFailure() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.networkDown());

        // Act
        Status status = new mainframe.client.messages.constantCategory.List(1, 20).send(connection);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(status.getWasSuccessful()),
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), status.getErrorCode())
        );
    }

    @Test
    void failsLoudlyWhenNothingIsScripted() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act and Assert
        Assertions.assertThrows(
                IllegalStateException.class,
                () -> new mainframe.client.messages.constantCategory.List(1, 20).send(connection));
    }

    // AI-assisted: going offline partway through a test makes send() return a
    // network failure instead of the scripted response
    @Test
    void returnsNetworkFailureAfterGoingOfflineMidTest() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanRateType"));
        Status before = new mainframe.client.messages.constantCategory.List(1, 20).send(connection);

        // Act
        connection.setIsConnected(false);
        Status after = new mainframe.client.messages.constantCategory.List(1, 20).send(connection);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(before.getWasSuccessful()),
                () -> Assertions.assertFalse(after.getWasSuccessful()),
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), after.getErrorCode()),
                () -> Assertions.assertFalse(connection.isConnected())
        );
    }

    // AI-assisted
    @Test
    void resumesScriptedResponsesAfterReconnecting() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanRateType"));
        connection.setIsConnected(false);
        new mainframe.client.messages.constantCategory.List(1, 20).send(connection);

        // Act
        connection.setIsConnected(true);
        Status status = new mainframe.client.messages.constantCategory.List(1, 20).send(connection);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(status.getWasSuccessful()),
                () -> Assertions.assertTrue(connection.isConnected())
        );
    }

    // AI-assisted
    @Test
    void recordsRequestsSentWhileOffline() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        new mainframe.client.messages.constantCategory.List(1, 20).send(connection);

        // Assert
        var sent = connection.getSentRequests();
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, sent.size()),
                () -> Assertions.assertEquals("constantCategory.list", sent.get(0).getRequestType())
        );
    }

    // AI-assisted: a slow connection still answers, just later
    @Test
    void answersOnlyAfterTheDelay() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanRateType"));
        connection.setDelay(Duration.ofMillis(200));

        // Act
        long started = System.nanoTime();
        Status status = new mainframe.client.messages.constantCategory.List(1, 20).send(connection);
        Duration took = Duration.ofNanos(System.nanoTime() - started);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(status.getWasSuccessful()),
                () -> Assertions.assertTrue(took.compareTo(Duration.ofMillis(200)) >= 0, "took " + took)
        );
    }

    // AI-assisted: going offline takes as long as any other answer on a slow network
    @Test
    void waitsBeforeReportingANetworkFailureToo() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        connection.setDelay(Duration.ofMillis(200));

        // Act
        long started = System.nanoTime();
        Status status = new mainframe.client.messages.constantCategory.List(1, 20).send(connection);
        Duration took = Duration.ofNanos(System.nanoTime() - started);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), status.getErrorCode()),
                () -> Assertions.assertTrue(took.compareTo(Duration.ofMillis(200)) >= 0, "took " + took)
        );
    }

    // AI-assisted: e.g. the Cancel button stopping a slow search
    @Test
    void anInterruptedWaitGetsNoAnswerAndKeepsTheInterrupt() throws Exception {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("constantCategory.list", FakeConnection.list("LoanRateType"));
        connection.setDelay(Duration.ofSeconds(30));
        AtomicReference<Status> status = new AtomicReference<>();
        AtomicBoolean stillInterrupted = new AtomicBoolean();
        Thread caller = new Thread(() -> {
            try {
                status.set(new mainframe.client.messages.constantCategory.List(1, 20).send(connection));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            stillInterrupted.set(Thread.currentThread().isInterrupted());
        });

        // Act
        caller.start();
        while (connection.getSentRequests().isEmpty()) {
            Thread.onSpinWait();
        }
        caller.interrupt();
        caller.join(Duration.ofSeconds(5).toMillis());

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(caller.isAlive(), "send() should stop waiting once interrupted"),
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), status.get().getErrorCode()),
                () -> Assertions.assertTrue(stillInterrupted.get())
        );
    }

    // AI-assisted
    @Test
    void rejectsANegativeDelay() {
        FakeConnection connection = new FakeConnection();

        Assertions.assertThrows(IllegalArgumentException.class, () -> connection.setDelay(Duration.ofMillis(-1)));
    }
}
