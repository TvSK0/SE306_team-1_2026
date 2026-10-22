package lavs.acceptance;

import lavs.data.MainframeGateway;
import lavs.logic.CustomerEligibilityService;
import lavs.logic.LoanDetailsInput;
import lavs.logic.LoanRepaymentsResult;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanSaveResult;
import lavs.logic.LoanService;
import lavs.logic.ServiceResult;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. US6 "Create a loan": one test per GIVEN/WHEN/THEN in the proposal, run against
// the real services with only the mainframe replaced by a FakeConnection.
@DisplayName("US6: Create a loan")
class US6AcceptanceTests {
    private static final String CUSTOMER = "C-10234";
    private static final String LOAN = "L-5521";
    private static final LoanDetailsInput VALID = new LoanDetailsInput(
            "450000.00", "Fixed", "6.29", "Yearly", "2786.00", "Monthly", "2026-11-01", "2027-11-01");

    private final FakeConnection mainframe = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(mainframe);

    @Test
    @DisplayName("GIVEN the customer has no mailing address, or has no phone number or email, "
            + "WHEN the MMM tries to start a loan, THEN the application stops them and says what is missing")
    void startingALoanSaysWhatTheCustomerIsMissing() {
        givenCustomer(false, false, false);

        // what the New loan button runs before it opens the loan form
        ServiceResult<Void> result = new CustomerEligibilityService(gateway).check(CUSTOMER);

        assertFalse(result.isSuccessful(), "the loan form must not open");
        assertEquals(List.of("the customer needs a mailing address", "the customer needs a phone number or email"),
                result.getValidationErrors().getErrors().stream().map(FieldError::getMessage).toList());
    }

    @Test
    @DisplayName("GIVEN the customer has a mailing address but no phone number or email, "
            + "WHEN the MMM tries to start a loan, THEN the application stops them and says what is missing")
    void startingALoanSaysWhenOnlyTheContactDetailsAreMissing() {
        givenCustomer(true, false, false);

        ServiceResult<Void> result = new CustomerEligibilityService(gateway).check(CUSTOMER);

        assertFalse(result.isSuccessful());
        assertEquals(List.of("the customer needs a phone number or email"),
                result.getValidationErrors().getErrors().stream().map(FieldError::getMessage).toList());
    }

    @Test
    @DisplayName("GIVEN a loan field is empty or invalid, WHEN the MMM saves, "
            + "THEN that field is highlighted and nothing else they entered is lost")
    void anInvalidFieldIsReportedAndTheRestIsKept() throws IOException {
        givenCustomer(true, true, false);
        LoanDetailsInput typed = new LoanDetailsInput(
                "lots", "Fixed", "6.29", "Yearly", "2786.00", "Monthly", "2026-11-01", "2027-11-01");

        LoanSaveResult result = new LoanService(gateway).createLoan(CUSTOMER, typed);

        assertFalse(result.isSuccessful());
        // only the bad field is named, so only it is highlighted
        assertEquals(List.of(LoanDetailsInput.PRINCIPAL),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        // nothing was sent, and every value the MMM typed is still there to show in the form
        assertTrue(mainframe.getSentRequests().stream().noneMatch(request -> "loan.create".equals(request.getRequestType())));
        assertAll(
                () -> assertEquals("lots", typed.getPrincipal()),
                () -> assertEquals("6.29", typed.getRateAmount()),
                () -> assertEquals("2786.00", typed.getPaymentAmount()),
                () -> assertEquals("2026-11-01", typed.getStartDate()),
                () -> assertEquals("2027-11-01", typed.getRenewalDate()));
    }

    @Test
    @DisplayName("GIVEN all the loan details are valid, WHEN the MMM saves, "
            + "THEN the loan is created with the status New and its summary and repayments are shown")
    void aValidLoanIsCreatedAsNewWithItsSummaryAndRepayments() throws IOException {
        givenCustomer(true, true, false);
        mainframe.whenRequested("loan.create", FakeConnection.success(savedLoan()));
        mainframe.whenRequested("loanSummary.retrieve", FakeConnection.success(summary()));
        mainframe.whenRequested("loanRepaymentSchedule.list", FakeConnection.success(schedule()));

        LoanSaveResult saved = new LoanService(gateway).createLoan(CUSTOMER, VALID);
        // once saved, the loan screen opens the Repayments tab, which loads these
        LoanRepaymentsResult repayments = new LoanRepaymentsService(gateway).load(saved.getLoan().getLoanNumber());

        assertTrue(saved.isSuccessful());
        assertEquals("New", saved.getLoan().getStatus());
        assertTrue(repayments.isSuccessful());
        assertEquals(2, repayments.getSummary().getNumberOfPayments());
        assertEquals(2, repayments.getRows().size());
    }

    // --- the mainframe's replies ---

    private void givenCustomer(boolean mailing, boolean phone, boolean email) {
        Map<String, String> addresses = list(1);
        addresses.put("1:isMailing", mailing ? "y" : "n");
        mainframe.whenRequested("customerAddress.list", FakeConnection.success(addresses));
        mainframe.whenRequested("customerPhoneNumber.list", FakeConnection.success(list(phone ? 1 : 0)));
        mainframe.whenRequested("customerEmail.list", FakeConnection.success(list(email ? 1 : 0)));
    }

    private static Map<String, String> savedLoan() {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN);
        reply.put("customerNumber", CUSTOMER);
        reply.put("status", "New");
        reply.put("principal", VALID.getPrincipal());
        reply.put("rateType", VALID.getRateType());
        reply.put("rateAmount", VALID.getRateAmount());
        reply.put("rateFrequency", VALID.getRateFrequency());
        reply.put("paymentAmount", VALID.getPaymentAmount());
        reply.put("paymentFrequency", VALID.getPaymentFrequency());
        reply.put("startDate", VALID.getStartDate());
        reply.put("renewalDate", VALID.getRenewalDate());
        return reply;
    }

    private static Map<String, String> summary() {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN);
        reply.put("totalAmountPaid", "5572.00");
        reply.put("totalInterestPaid", "72.00");
        reply.put("numberOfPayments", "2");
        reply.put("finalPaymentDate", "2026-12-01");
        reply.put("finalPaymentAmount", "2786.00");
        return reply;
    }

    private static Map<String, String> schedule() {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", LOAN);
        reply.put("total", "2");
        reply.put("items", "2");
        for (int i = 1; i <= 2; i++) {
            reply.put(i + ":paymentNumber", Integer.toString(i));
            reply.put(i + ":date", "2026-1" + i + "-01");
            reply.put(i + ":amountPaid", "2786.00");
            reply.put(i + ":principalAmount", "2750.00");
            reply.put(i + ":interestAmount", "36.00");
            reply.put(i + ":amountRemaining", i == 1 ? "447250.00" : "444500.00");
        }
        return reply;
    }

    private static Map<String, String> list(int rows) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Integer.toString(rows));
        values.put("items", Integer.toString(rows));
        for (int i = 1; i <= rows; i++) {
            values.put(i + ":id", Integer.toString(i));
        }
        return values;
    }
}
