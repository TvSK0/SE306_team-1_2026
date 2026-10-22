// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.model.Loan;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class LoanServiceTests {
    // AI-assisted: saves go through the upload queue (US11). Each service gets a real SyncService
    // over an in-memory queue that sends to the same FakeConnection, as the same requests.
    private final List<SyncService> _syncs = new ArrayList<>();
    private InMemoryChangeQueue _queue;

    private LoanService serviceOver(FakeConnection connection) {
        _queue = new InMemoryChangeQueue();
        SyncService sync = new SyncService(_queue, new MainframeGateway(connection),
                Executors.newSingleThreadScheduledExecutor());
        _syncs.add(sync);
        return new LoanService(connection, sync);
    }

    @AfterEach
    void closeSyncs() {
        _syncs.forEach(SyncService::close);
    }

    private static final String CUSTOMER_NUMBER = "C-10234";
    private static final String LOAN_NUMBER = "L-5521";

    private static final LoanDetailsInput VALID = new LoanDetailsInput(
            "450000.00", "Fixed", "6.29", "Yearly", "2786.00", "Monthly", "2026-11-01", "2027-11-01");

    // --- checkCanStartLoan ---

    @Test
    void customerWithAddressAndPhoneCanStartALoan() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(true, true, false);

        // Assert
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void customerWithAddressAndEmailCanStartALoan() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(true, false, true);

        // Assert
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void saysEachThingTheCustomerIsMissing() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(false, false, false);

        // Assert
        Assertions.assertEquals(List.of("mailingAddress", "contactDetails"), fieldNames(result));
    }

    @Test
    void saysOnlyTheMailingAddressIsMissing() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(false, false, true);

        // Assert
        Assertions.assertEquals(List.of("mailingAddress"), fieldNames(result));
    }

    @Test
    void saysOnlyTheContactDetailsAreMissing() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(true, false, false);

        // Assert
        Assertions.assertEquals(List.of("contactDetails"), fieldNames(result));
    }

    // --- validate ---

    @Test
    void acceptsValidDetails() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());

        // Act and Assert
        Assertions.assertTrue(service.validate(VALID).isValid());
    }

    @Test
    void acceptsAMissingRenewalDate() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(
                "450000", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "");

        // Act and Assert
        Assertions.assertTrue(service.validate(input).isValid());
    }

    @Test
    void reportsEveryEmptyRequiredField() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(null, null, null, null, null, null, null, null);

        // Act
        ValidationResult result = service.validate(input);

        // Assert
        Assertions.assertEquals(List.of(
                LoanDetailsInput.PRINCIPAL, LoanDetailsInput.RATE_TYPE,
                LoanDetailsInput.RATE_AMOUNT, LoanDetailsInput.RATE_FREQUENCY,
                LoanDetailsInput.PAYMENT_AMOUNT, LoanDetailsInput.PAYMENT_FREQUENCY,
                LoanDetailsInput.START_DATE), fieldNames(result));
    }

    @Test
    void reportsBadlyFormattedFields() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(
                "450,000", "Fixed", "six", "Yearly", "-5", "Monthly", "01/11/2026", "next year");

        // Act
        ValidationResult result = service.validate(input);

        // Assert
        Assertions.assertEquals(List.of(
                LoanDetailsInput.PRINCIPAL, LoanDetailsInput.RATE_AMOUNT,
                LoanDetailsInput.PAYMENT_AMOUNT, LoanDetailsInput.START_DATE,
                LoanDetailsInput.RENEWAL_DATE), fieldNames(result));
    }

    @Test
    void reportsAZeroPrincipal() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(
                "0", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "");

        // Act
        ValidationResult result = service.validate(input);

        // Assert
        Assertions.assertEquals(List.of(LoanDetailsInput.PRINCIPAL), fieldNames(result));
    }

    @Test
    void acceptsARateWithMoreThanTwoDecimalPlaces() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(
                "450000", "Fixed", "5.875", "Yearly", "2786", "Monthly", "2026-11-01", "");

        // Act and Assert
        Assertions.assertTrue(service.validate(input).isValid());
    }

    @Test
    void reportsARenewalDateBeforeTheStartDate() {
        // Arrange
        LoanService service = serviceOver(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(
                "450000", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "2026-10-31");

        // Act
        ValidationResult result = service.validate(input);

        // Assert
        Assertions.assertEquals(List.of(LoanDetailsInput.RENEWAL_DATE), fieldNames(result));
    }

    // --- createLoan ---

    @Test
    void createsTheLoanAndReturnsItWithItsNumberAndStatus() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.success(savedLoanReply(VALID)));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.createLoan(CUSTOMER_NUMBER, VALID);

        // Assert
        Loan loan = result.getLoan();
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(LOAN_NUMBER, loan.getLoanNumber()),
                () -> Assertions.assertEquals("New", loan.getStatus()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, loan.getCustomerNumber()),
                () -> Assertions.assertEquals(0, new BigDecimal("450000").compareTo(loan.getPrincipal())),
                () -> Assertions.assertEquals(0, new BigDecimal("6.29").compareTo(loan.getRateAmount())),
                () -> Assertions.assertEquals(LocalDate.of(2026, 11, 1), loan.getStartDate()),
                () -> Assertions.assertEquals(LocalDate.of(2027, 11, 1), loan.getRenewalDate()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, sent.getValue("customerNumber")),
                () -> Assertions.assertEquals("450000.0", sent.getValue("principal")),
                () -> Assertions.assertEquals("Fixed", sent.getValue("rateType")),
                () -> Assertions.assertEquals("2026-11-01", sent.getValue("startDate"))
        );
    }

    @Test
    void createsALoanWithoutARenewalDate() throws IOException {
        // Arrange
        LoanDetailsInput input = new LoanDetailsInput(
                "450000", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "");
        Map<String, String> reply = savedLoanReply(input);
        reply.remove("renewalDate");
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.success(reply));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.createLoan(CUSTOMER_NUMBER, input);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertNull(result.getLoan().getRenewalDate()),
                () -> Assertions.assertNull(connection.getSentRequests().get(0).getValue("renewalDate"))
        );
    }

    @Test
    void doesNotContactTheMainframeWhenTheDetailsAreInvalid() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        LoanService service = serviceOver(connection);
        LoanDetailsInput input = new LoanDetailsInput(
                "", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "");

        // Act
        LoanSaveResult result = service.createLoan(CUSTOMER_NUMBER, input);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(List.of(LoanDetailsInput.PRINCIPAL), fieldNames(result.getValidationErrors())),
                () -> Assertions.assertNull(result.getMainframeStatus()),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    void requiresACustomerNumber() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.createLoan(" ", VALID);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of("customerNumber"), fieldNames(result.getValidationErrors())),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    void reportsAMainframeRejection() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.rejected("Unknown rate type"));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.createLoan(CUSTOMER_NUMBER, VALID);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertNull(result.getLoan()),
                () -> Assertions.assertEquals(StatusMessage.SERVER_FAILURE.getCode(), result.getMainframeStatus().getErrorCode()),
                () -> Assertions.assertEquals("Unknown rate type", result.getMainframeStatus().getErrorMessage())
        );
    }

    @Test
    void reportsANetworkFailure() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.createLoan(CUSTOMER_NUMBER, VALID);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(
                        StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), result.getMainframeStatus().getErrorCode())
        );
    }

    // --- updateLoan ---

    @Test
    void updatesTheLoanByItsNumber() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.update", FakeConnection.success(savedLoanReply(VALID)));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.updateLoan(CUSTOMER_NUMBER, LOAN_NUMBER, VALID);

        // Assert
        MessageRequest sent = connection.getSentRequests().get(0);
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(LOAN_NUMBER, result.getLoan().getLoanNumber()),
                () -> Assertions.assertEquals(LOAN_NUMBER, sent.getValue("loanNumber")),
                () -> Assertions.assertEquals("2786.0", sent.getValue("paymentAmount"))
        );
    }

    @Test
    void updatesALoanWhoseRenewalDateWasCleared() throws IOException {
        // Arrange
        LoanDetailsInput input = new LoanDetailsInput(
                "450000", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", null);
        Map<String, String> reply = savedLoanReply(input);
        reply.put("renewalDate", "");
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.update", FakeConnection.success(reply));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.updateLoan(CUSTOMER_NUMBER, LOAN_NUMBER, input);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertNull(result.getLoan().getRenewalDate()),
                () -> Assertions.assertNull(connection.getSentRequests().get(0).getValue("renewalDate"))
        );
    }

    @Test
    void requiresALoanNumberToUpdate() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.updateLoan(CUSTOMER_NUMBER, null, VALID);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of("loanNumber"), fieldNames(result.getValidationErrors())),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    void reportsARejectedUpdate() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.update", FakeConnection.rejected("Loan is not New"));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.updateLoan(CUSTOMER_NUMBER, LOAN_NUMBER, VALID);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals("Loan is not New", result.getMainframeStatus().getErrorMessage())
        );
    }

    // --- saving through the upload queue (AI-assisted, US11) ---

    @Test
    void aLoanCreatedOfflineKeepsATemporaryNumberUntilItUploads() throws IOException {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.networkDown());

        LoanSaveResult result = serviceOver(connection).createLoan(CUSTOMER_NUMBER, VALID);

        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSaved()),
                () -> Assertions.assertTrue(result.isWaitingToUpload()),
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertTrue(TemporaryNumbers.isTemporary(result.getLoan().getLoanNumber())),
                () -> Assertions.assertEquals("New", result.getLoan().getStatus()),
                () -> Assertions.assertEquals(result.getLoan().getLoanNumber(), _queue.all().get(0).getCreatesNumber()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, _queue.all().get(0).getCustomerNumber())
        );
    }

    @Test
    void aLoanCreatedOnlineGetsTheNumberTheMainframeGaveIt() throws IOException {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.success(savedLoanReply(VALID)));

        LoanSaveResult result = serviceOver(connection).createLoan(CUSTOMER_NUMBER, VALID);

        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertFalse(result.isWaitingToUpload());
        Assertions.assertEquals(LOAN_NUMBER, result.getLoan().getLoanNumber());
        Assertions.assertTrue(_queue.all().isEmpty(), "an uploaded change leaves the queue");
    }

    @Test
    void anUpdateSavedThroughTheQueueLeavesTheStatusForTheScreenToKeep() throws IOException {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.update", FakeConnection.success(savedLoanReply(VALID)));

        LoanSaveResult result = serviceOver(connection).updateLoan(CUSTOMER_NUMBER, LOAN_NUMBER, VALID);

        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals(LOAN_NUMBER, result.getLoan().getLoanNumber());
        Assertions.assertNull(result.getLoan().getStatus());
    }

    @Test
    void aRefusedLoanIsNotSaved() throws IOException {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.rejected("Unknown rate type"));

        LoanSaveResult result = serviceOver(connection).createLoan(CUSTOMER_NUMBER, VALID);

        Assertions.assertFalse(result.isSaved());
        Assertions.assertFalse(result.isWaitingToUpload());
        Assertions.assertNull(result.getLoan());
    }

    // --- LoanDetailsInput.fromLoan ---

    @Test
    void fillsTheFormFromASavedLoan() {
        // Arrange
        Loan loan = new Loan(CUSTOMER_NUMBER, new BigDecimal("450000.00"), "Fixed", new BigDecimal("6.29"),
                "Yearly", new BigDecimal("2786.00"), "Monthly", LocalDate.of(2026, 11, 1), LocalDate.of(2027, 11, 1));

        // Act
        LoanDetailsInput input = LoanDetailsInput.fromLoan(loan);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals("450000.00", input.getPrincipal()),
                () -> Assertions.assertEquals("Fixed", input.getRateType()),
                () -> Assertions.assertEquals("6.29", input.getRateAmount()),
                () -> Assertions.assertEquals("Yearly", input.getRateFrequency()),
                () -> Assertions.assertEquals("2786.00", input.getPaymentAmount()),
                () -> Assertions.assertEquals("Monthly", input.getPaymentFrequency()),
                () -> Assertions.assertEquals("2026-11-01", input.getStartDate()),
                () -> Assertions.assertEquals("2027-11-01", input.getRenewalDate())
        );
    }

    @Test
    void leavesMissingLoanValuesBlank() {
        // Arrange
        Loan loan = new Loan();

        // Act
        LoanDetailsInput input = LoanDetailsInput.fromLoan(loan);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals("", input.getPrincipal()),
                () -> Assertions.assertNull(input.getRateType()),
                () -> Assertions.assertEquals("", input.getRateAmount()),
                () -> Assertions.assertNull(input.getRateFrequency()),
                () -> Assertions.assertEquals("", input.getPaymentAmount()),
                () -> Assertions.assertNull(input.getPaymentFrequency()),
                () -> Assertions.assertEquals("", input.getStartDate()),
                () -> Assertions.assertEquals("", input.getRenewalDate())
        );
    }

    // --- loadLoan ---

    @Test
    void loadsTheLoanByItsNumber() throws IOException {
        // Arrange
        Map<String, String> reply = savedLoanReply(VALID);
        reply.put("status", "Pending");
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", FakeConnection.success(reply));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.loadLoan(LOAN_NUMBER);

        // Assert
        Loan loan = result.getLoan();
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertEquals(LOAN_NUMBER, connection.getSentRequests().get(0).getValue("number")),
                () -> Assertions.assertEquals(LOAN_NUMBER, loan.getLoanNumber()),
                () -> Assertions.assertEquals(CUSTOMER_NUMBER, loan.getCustomerNumber()),
                () -> Assertions.assertEquals("Pending", loan.getStatus()),
                () -> Assertions.assertEquals(0, new BigDecimal("450000").compareTo(loan.getPrincipal())),
                () -> Assertions.assertEquals("Fixed", loan.getRateType()),
                () -> Assertions.assertEquals(0, new BigDecimal("6.29").compareTo(loan.getRateAmount())),
                () -> Assertions.assertEquals("Yearly", loan.getRateFrequency()),
                () -> Assertions.assertEquals(0, new BigDecimal("2786").compareTo(loan.getPaymentAmount())),
                () -> Assertions.assertEquals("Monthly", loan.getPaymentFrequency()),
                () -> Assertions.assertEquals(LocalDate.of(2026, 11, 1), loan.getStartDate()),
                () -> Assertions.assertEquals(LocalDate.of(2027, 11, 1), loan.getRenewalDate())
        );
    }

    @Test
    void loadsALoanWithoutARenewalDate() throws IOException {
        // Arrange
        Map<String, String> reply = savedLoanReply(VALID);
        reply.remove("renewalDate");
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", FakeConnection.success(reply));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.loadLoan(LOAN_NUMBER);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(result.isSuccessful()),
                () -> Assertions.assertNull(result.getLoan().getRenewalDate())
        );
    }

    @Test
    void loadingRequiresALoanNumber() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.loadLoan(" ");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals(List.of(LoanService.LOAN_NUMBER), fieldNames(result.getValidationErrors())),
                () -> Assertions.assertEquals(0, connection.getSentRequests().size())
        );
    }

    @Test
    void reportsALoanTheMainframeDoesNotHave() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", FakeConnection.rejected("Loan not found"));
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.loadLoan(LOAN_NUMBER);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertNull(result.getLoan()),
                () -> Assertions.assertEquals(StatusMessage.SERVER_FAILURE.getCode(), result.getMainframeStatus().getErrorCode()),
                () -> Assertions.assertEquals("Loan not found", result.getMainframeStatus().getErrorMessage())
        );
    }

    @Test
    void reportsANetworkFailureWhileLoading() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        LoanService service = serviceOver(connection);

        // Act
        LoanSaveResult result = service.loadLoan(LOAN_NUMBER);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertNull(result.getLoan()),
                () -> Assertions.assertEquals(
                        StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), result.getMainframeStatus().getErrorCode())
        );
    }

    private static Map<String, String> savedLoanReply(LoanDetailsInput input) {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN_NUMBER);
        reply.put("customerNumber", CUSTOMER_NUMBER);
        reply.put("status", "New");
        reply.put("principal", input.getPrincipal());
        reply.put("rateType", input.getRateType());
        reply.put("rateAmount", input.getRateAmount());
        reply.put("rateFrequency", input.getRateFrequency());
        reply.put("paymentAmount", input.getPaymentAmount());
        reply.put("paymentFrequency", input.getPaymentFrequency());
        reply.put("startDate", input.getStartDate());
        reply.put("renewalDate", input.getRenewalDate());
        return reply;
    }

    private static List<String> fieldNames(ValidationResult result) {
        List<String> names = new ArrayList<>();
        for (FieldError error : result.getErrors()) {
            names.add(error.getFieldName());
        }
        return names;
    }
}
