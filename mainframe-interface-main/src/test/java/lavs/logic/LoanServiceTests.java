// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Loan;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LoanServiceTests {
    private static final String CUSTOMER_NUMBER = "C-10234";
    private static final String LOAN_NUMBER = "L-5521";

    private static final LoanDetailsInput VALID = new LoanDetailsInput(
            "450000.00", "Fixed", "6.29", "Yearly", "2786.00", "Monthly", "2026-11-01", "2027-11-01");

    // --- checkCanStartLoan ---

    @Test
    void customerWithAddressAndPhoneCanStartALoan() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(true, true, false);

        // Assert
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void customerWithAddressAndEmailCanStartALoan() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(true, false, true);

        // Assert
        Assertions.assertTrue(result.isValid());
    }

    @Test
    void saysEachThingTheCustomerIsMissing() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(false, false, false);

        // Assert
        Assertions.assertEquals(List.of("mailingAddress", "contactDetails"), fieldNames(result));
    }

    @Test
    void saysOnlyTheMailingAddressIsMissing() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(false, false, true);

        // Assert
        Assertions.assertEquals(List.of("mailingAddress"), fieldNames(result));
    }

    @Test
    void saysOnlyTheContactDetailsAreMissing() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());

        // Act
        ValidationResult result = service.checkCanStartLoan(true, false, false);

        // Assert
        Assertions.assertEquals(List.of("contactDetails"), fieldNames(result));
    }

    // --- validate ---

    @Test
    void acceptsValidDetails() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());

        // Act and Assert
        Assertions.assertTrue(service.validate(VALID).isValid());
    }

    @Test
    void acceptsAMissingRenewalDate() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(
                "450000", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "");

        // Act and Assert
        Assertions.assertTrue(service.validate(input).isValid());
    }

    @Test
    void reportsEveryEmptyRequiredField() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());
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
        LoanService service = new LoanService(new FakeConnection());
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
        LoanService service = new LoanService(new FakeConnection());
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
        LoanService service = new LoanService(new FakeConnection());
        LoanDetailsInput input = new LoanDetailsInput(
                "450000", "Fixed", "5.875", "Yearly", "2786", "Monthly", "2026-11-01", "");

        // Act and Assert
        Assertions.assertTrue(service.validate(input).isValid());
    }

    @Test
    void reportsARenewalDateBeforeTheStartDate() {
        // Arrange
        LoanService service = new LoanService(new FakeConnection());
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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);
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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);

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
        LoanService service = new LoanService(connection);

        // Act
        LoanSaveResult result = service.updateLoan(CUSTOMER_NUMBER, LOAN_NUMBER, VALID);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(result.isSuccessful()),
                () -> Assertions.assertEquals("Loan is not New", result.getMainframeStatus().getErrorMessage())
        );
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
        Assertions.assertEquals(VALID, input);
    }

    @Test
    void leavesMissingLoanValuesBlank() {
        // Arrange
        Loan loan = new Loan();

        // Act
        LoanDetailsInput input = LoanDetailsInput.fromLoan(loan);

        // Assert
        Assertions.assertEquals(new LoanDetailsInput("", null, "", null, "", null, "", ""), input);
    }

    private static Map<String, String> savedLoanReply(LoanDetailsInput input) {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN_NUMBER);
        reply.put("customerNumber", CUSTOMER_NUMBER);
        reply.put("status", "New");
        reply.put("principal", input.principal());
        reply.put("rateType", input.rateType());
        reply.put("rateAmount", input.rateAmount());
        reply.put("rateFrequency", input.rateFrequency());
        reply.put("paymentAmount", input.paymentAmount());
        reply.put("paymentFrequency", input.paymentFrequency());
        reply.put("startDate", input.startDate());
        reply.put("renewalDate", input.renewalDate());
        return reply;
    }

    private static List<String> fieldNames(ValidationResult result) {
        return result.getErrors().stream().map(FieldError::getFieldName).collect(Collectors.toList());
    }
}
