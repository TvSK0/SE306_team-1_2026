// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoanRepaymentsServiceTests {
    private static final String LOAN = "01-0001-0005347-001";

    @Test
    void readsTheSummaryTotals() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", Map.of("number", LOAN), summaryReply());
        connection.whenRequested("loanRepaymentSchedule.list", scheduleReply(0, 1));

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection).load(LOAN);

        // Assert
        LoanSummary summary = result.getSummary();
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(new BigDecimal("508234.25"), summary.getTotalAmountPaid()),
                () -> Assertions.assertEquals(new BigDecimal("8234.25"), summary.getTotalInterestPaid()),
                () -> Assertions.assertEquals(26, summary.getNumberOfPayments()),
                () -> Assertions.assertEquals(LocalDate.of(2027, 10, 19), summary.getFinalPaymentDate()),
                () -> Assertions.assertEquals(new BigDecimal("12032.25"), summary.getFinalPaymentAmount()));
    }

    @Test
    void readsEveryPaymentOnAPage() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", summaryReply());
        connection.whenRequested("loanRepaymentSchedule.list", scheduleReply(1, 1));

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection).load(LOAN);

        // Assert
        RepaymentRow row = result.getRows().get(0);
        Assertions.assertAll(
                () -> Assertions.assertEquals(1, result.getRows().size()),
                () -> Assertions.assertEquals(1, row.getPaymentNumber()),
                () -> Assertions.assertEquals(LocalDate.of(2026, 11, 3), row.getDate()),
                () -> Assertions.assertEquals(new BigDecimal("19848.08"), row.getAmountPaid()),
                () -> Assertions.assertEquals(new BigDecimal("19232.43"), row.getPrincipalAmount()),
                () -> Assertions.assertEquals(new BigDecimal("615.65"), row.getInterestAmount()),
                () -> Assertions.assertEquals(new BigDecimal("480767.57"), row.getAmountRemaining()));
    }

    @Test
    void readsPageAfterPageUntilEveryPaymentIsLoaded() throws IOException {
        // Arrange: 5 payments, 2 per page
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", summaryReply());
        connection.whenRequested("loanRepaymentSchedule.list", Map.of("page", "1"), schedulePage(5, 1, 2));
        connection.whenRequested("loanRepaymentSchedule.list", Map.of("page", "2"), schedulePage(5, 3, 2));
        connection.whenRequested("loanRepaymentSchedule.list", Map.of("page", "3"), schedulePage(5, 5, 1));

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection, 2).load(LOAN);

        // Assert
        List<RepaymentRow> rows = result.getRows();
        Assertions.assertAll(
                () -> Assertions.assertEquals(5, rows.size()),
                () -> Assertions.assertEquals(1, rows.get(0).getPaymentNumber()),
                () -> Assertions.assertEquals(5, rows.get(4).getPaymentNumber()),
                () -> Assertions.assertEquals(4, connection.getSentRequests().size()));
    }

    @Test
    void asksForTheLoanAndPageSizeOnEachPage() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", summaryReply());
        connection.whenRequested("loanRepaymentSchedule.list", scheduleReply(1, 1));

        // Act
        new LoanRepaymentsService(connection, 50).load(LOAN);

        // Assert
        MessageRequest request = connection.getSentRequests().get(1);
        Assertions.assertAll(
                () -> Assertions.assertEquals(LOAN, request.getValue("number")),
                () -> Assertions.assertEquals("1", request.getValue("page")),
                () -> Assertions.assertEquals("50", request.getValue("size")));
    }

    @Test
    void stopsIfAPageComesBackEmpty() throws IOException {
        // Arrange: the server says 5 but only ever sends 2
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", summaryReply());
        connection.whenRequested("loanRepaymentSchedule.list", Map.of("page", "1"), schedulePage(5, 1, 2));
        connection.whenRequested("loanRepaymentSchedule.list", Map.of("page", "2"), schedulePage(5, 3, 0));

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection, 2).load(LOAN);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(2, result.getRows().size()));
    }

    @Test
    void loadsALoanWithNoPayments() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", summaryReply());
        connection.whenRequested("loanRepaymentSchedule.list", scheduleReply(0, 1));

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection).load(LOAN);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertTrue(result.getRows().isEmpty()));
    }

    @Test
    void passesOnARejectedSummaryWithoutAskingForTheSchedule() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", FakeConnection.rejected("Unable to run query: query error"));

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection).load(LOAN);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(StatusMessage.SERVER_FAILURE.getCode(), result.getMainframeStatus().getErrorCode()),
                () -> Assertions.assertEquals("Unable to run query: query error", result.getMainframeStatus().getErrorMessage()),
                () -> Assertions.assertNull(result.getSummary()),
                () -> Assertions.assertEquals(1, connection.getSentRequests().size()));
    }

    @Test
    void returnsNothingWhenALaterPageFails() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loanSummary.retrieve", summaryReply());
        connection.whenRequested("loanRepaymentSchedule.list", Map.of("page", "1"), schedulePage(5, 1, 2));
        connection.whenRequested("loanRepaymentSchedule.list", Map.of("page", "2"), FakeConnection.networkDown());

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection, 2).load(LOAN);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), result.getMainframeStatus().getErrorCode()),
                () -> Assertions.assertTrue(result.getRows().isEmpty()));
    }

    @Test
    void reportsANetworkFailure() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection).load(LOAN);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), result.getMainframeStatus().getErrorCode()));
    }

    @Test
    void treatsAReplyWithMissingValuesAsUnreadable() throws IOException {
        // Arrange: the starter message throws when a number is missing from the reply
        FakeConnection connection = new FakeConnection();
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN);
        connection.whenRequested("loanSummary.retrieve", FakeConnection.success(reply));

        // Act
        LoanRepaymentsResult result = new LoanRepaymentsService(connection).load(LOAN);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(StatusMessage.UNABLE_TO_PARSE.getCode(), result.getMainframeStatus().getErrorCode()));
    }

    @Test
    void needsALoanNumber() {
        // Arrange
        LoanRepaymentsService service = new LoanRepaymentsService(new FakeConnection());

        // Act / Assert
        Assertions.assertAll(
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> service.load(null)),
                () -> Assertions.assertThrows(IllegalArgumentException.class, () -> service.load(" ")));
    }

    // values copied from a real loanSummary.retrieve reply
    private static MessageResponse summaryReply() {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN);
        reply.put("customerName", "alu a");
        reply.put("totalAmountPaid", "508234.25");
        reply.put("totalInterestPaid", "8234.25");
        reply.put("numberOfPayments", "26");
        reply.put("finalPaymentDate", "2027-10-19");
        reply.put("finalPaymentAmount", "12032.25");
        return FakeConnection.success(reply);
    }

    private static MessageResponse scheduleReply(long total, long firstPayment) {
        return schedulePage(total, firstPayment, (int) total);
    }

    // a page of `count` payments numbered from firstPayment; the first row's values come
    // from a real loanRepaymentSchedule.list reply
    private static MessageResponse schedulePage(long total, long firstPayment, int count) {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN);
        reply.put("customerName", "alu a");
        reply.put("total", Long.toString(total));
        reply.put("items", Integer.toString(count));
        for (int i = 1; i <= count; i++) {
            reply.put(i + ":paymentNumber", Long.toString(firstPayment + i - 1));
            reply.put(i + ":date", "2026-11-03");
            reply.put(i + ":amountPaid", "19848.08");
            reply.put(i + ":principalAmount", "19232.43");
            reply.put(i + ":interestAmount", "615.65");
            reply.put(i + ":amountRemaining", "480767.57");
        }
        return FakeConnection.success(reply);
    }
}
