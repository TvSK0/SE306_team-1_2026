package lavs.logic;

import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. Uses FakeConnection, which throws on any request a test has not
// scripted, so each test also proves no lookup is made that wasn't needed.
class LoanStatusServiceTests {
    private static final String LOAN_NUMBER = "01-0001-0000015-001";
    private static final String CUSTOMER_NUMBER = "CUST-42";

    private FakeConnection connection;
    private LoanStatusService service;

    @BeforeEach
    void setUp() {
        connection = new FakeConnection();
        service = new LoanStatusService(connection);
    }

    // --- submitted ---

    @Test
    void submitsANewLoanWhoseBorrowerHasAMailingAddressAPhoneAndAnEmployer() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 1);
        givenStatusChangeSucceeds();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
        assertEquals(LoanStatusService.PENDING, result.getLoanStatus());
        MessageRequest update = sent("loan.updateStatus").get(0);
        assertEquals(LOAN_NUMBER, update.getValue("loanNumber"));
        assertEquals(LoanStatusService.PENDING, update.getValue("status"));
        // a phone number is enough and so is an employer: no email or co-borrower lookup
        assertTrue(sent("customerEmail.list").isEmpty());
        assertTrue(sent("loanCoborrower.list").isEmpty());
    }

    @Test
    void checksTheLoansOwnBorrowerRatherThanTrustingTheCaller() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 1);
        givenStatusChangeSucceeds();

        service.submitForVerification(LOAN_NUMBER);

        assertEquals(LOAN_NUMBER, sent("loan.retrieve").get(0).getValue("number"));
        for (String lookup : List.of("customerAddress.list", "customerPhoneNumber.list", "customerEmployer.list")) {
            assertEquals(CUSTOMER_NUMBER, sent(lookup).get(0).getValue("number"), lookup);
        }
    }

    @Test
    void acceptsAnEmailWhenTheCustomerHasNoPhoneNumber() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 0);
        givenRows("customerEmail.list", 1);
        givenRows("customerEmployer.list", 1);
        givenStatusChangeSucceeds();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
    }

    @Test
    void findsAMailingAddressOnALaterPage() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddressPage(1, 3, "n", "n");
        givenAddressPage(2, 3, "y");
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 1);
        givenStatusChangeSucceeds();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
        assertEquals(2, sent("customerAddress.list").size());
    }

    @Test
    void submitsWithNoEmployerWhenAGuarantorIsOnTheLoan() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 0);
        givenCoborrowerTypes(LoanBorrowerService.COBORROWER, LoanBorrowerService.GUARANTOR);
        givenStatusChangeSucceeds();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertTrue(result.isSuccessful());
    }

    // --- blocked by a field error: the status is never changed ---

    @Test
    void blocksACustomerWhoseAddressesAreNotForMailing() throws IOException {
        givenEligibleExceptAddresses();
        givenAddresses("n", "n");

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result, LoanStatusService.MAILING_ADDRESS);
    }

    @Test
    void blocksACustomerWithNoAddressesAtAll() throws IOException {
        givenEligibleExceptAddresses();
        givenAddresses();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result, LoanStatusService.MAILING_ADDRESS);
    }

    @Test
    void stopsReadingAddressesOnAnEmptyPageEvenIfTheTotalSaysThereAreMore() throws IOException {
        givenEligibleExceptAddresses();
        givenAddressPage(1, 5, "n");
        givenAddressPage(2, 5);

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result, LoanStatusService.MAILING_ADDRESS);
        assertEquals(2, sent("customerAddress.list").size());
    }

    @Test
    void anEmailCountsAsContactDetailsEvenWhenTheAddressIsMissing() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses();
        givenRows("customerPhoneNumber.list", 0);
        givenRows("customerEmail.list", 1);
        givenRows("customerEmployer.list", 1);

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        // only the address is asked for, not a phone number or email as well
        assertBlockedBy(result, LoanStatusService.MAILING_ADDRESS);
    }

    @Test
    void blocksACustomerWithNoPhoneNumberOrEmail() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 0);
        givenRows("customerEmail.list", 0);
        givenRows("customerEmployer.list", 1);

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result, LoanStatusService.CONTACT_DETAILS);
    }

    @Test
    void blocksABorrowerWithNoEmployerAndOnlyACoborrower() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 0);
        givenCoborrowerTypes(LoanBorrowerService.COBORROWER);

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result, LoanStatusService.GUARANTOR);
    }

    @Test
    void blocksABorrowerWithNoEmployerAndNobodyElseOnTheLoan() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 0);
        givenCoborrowerTypes();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result, LoanStatusService.GUARANTOR);
    }

    @Test
    void reportsEveryProblemAtOnce() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses();
        givenRows("customerPhoneNumber.list", 0);
        givenRows("customerEmail.list", 0);
        givenRows("customerEmployer.list", 0);
        givenCoborrowerTypes();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result,
                LoanStatusService.MAILING_ADDRESS, LoanStatusService.CONTACT_DETAILS, LoanStatusService.GUARANTOR);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Pending", "Active", "Cancelled"})
    void onlySubmitsANewLoanAndChecksNothingElseOtherwise(String status) throws IOException {
        givenLoan(status);

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertBlockedBy(result, LoanStatusService.STATUS);
        assertTrue(result.getValidationErrors().getErrors().get(0).getMessage().endsWith(status));
        assertEquals(1, connection.getSentRequests().size());
    }

    @Test
    void rejectsAMissingLoanNumberWithoutContactingTheMainframe() throws IOException {
        LoanStatusResult result = service.submitForVerification(" ");

        assertBlockedBy(result, LoanStatusService.LOAN_NUMBER);
        assertTrue(connection.getSentRequests().isEmpty());
    }

    // --- a lookup or the change itself fails: the status is returned, nothing is changed ---

    @Test
    void reportsAnUnknownLoan() throws IOException {
        connection.whenRequested("loan.retrieve", FakeConnection.rejected("no such loan"));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertFalse(result.isSuccessful());
        assertEquals("no such loan", result.getMainframeStatus().getErrorMessage());
        assertEquals(1, connection.getSentRequests().size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"customerAddress.list", "customerPhoneNumber.list", "customerEmail.list",
            "customerEmployer.list", "loanCoborrower.list"})
    void reportsAFailedLookupWithoutChangingTheStatus(String failingLookup) throws IOException {
        // scripted first, so it wins over the empty replies below
        connection.whenRequested(failingLookup, FakeConnection.networkDown());
        givenLoan(LoanStatusService.NEW);
        givenAddresses();
        givenRows("customerPhoneNumber.list", 0);
        givenRows("customerEmail.list", 0);
        givenRows("customerEmployer.list", 0);
        givenCoborrowerTypes();

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertFalse(result.isSuccessful());
        assertTrue(result.getValidationErrors().isValid());
        assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), result.getMainframeStatus().getErrorCode());
        assertNull(result.getLoanStatus());
        assertTrue(sent("loan.updateStatus").isEmpty());
    }

    @Test
    void reportsTheMainframeRejectingTheChange() throws IOException {
        givenLoan(LoanStatusService.NEW);
        givenAddresses("y");
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 1);
        connection.whenRequested("loan.updateStatus",
                FakeConnection.rejected("Loan '" + LOAN_NUMBER + "' cannot change from New to Pending"));

        LoanStatusResult result = service.submitForVerification(LOAN_NUMBER);

        assertFalse(result.isSuccessful());
        assertFalse(result.getMainframeStatus().getWasSuccessful());
        assertNull(result.getLoanStatus());
    }

    // --- helpers ---

    private void assertBlockedBy(LoanStatusResult result, String... fieldNames) {
        assertFalse(result.isSuccessful());
        assertNull(result.getMainframeStatus());
        assertNull(result.getLoanStatus());
        assertEquals(List.of(fieldNames),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        assertTrue(sent("loan.updateStatus").isEmpty());
    }

    private List<MessageRequest> sent(String requestType) {
        return connection.getSentRequests().stream()
                .filter(request -> requestType.equals(request.getRequestType()))
                .toList();
    }

    // the starter loan.Retrieve needs the three amounts present, as the real server always sends them
    private void givenLoan(String status) {
        connection.whenRequested("loan.retrieve", FakeConnection.success(Map.of(
                "loanNumber", LOAN_NUMBER,
                "customerNumber", CUSTOMER_NUMBER,
                "status", status,
                "principal", "450000",
                "rateAmount", "6.29",
                "paymentAmount", "2786")));
    }

    // everything but the addresses passes, so only the address check can block
    private void givenEligibleExceptAddresses() {
        givenLoan(LoanStatusService.NEW);
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 1);
    }

    private void givenStatusChangeSucceeds() {
        connection.whenRequested("loan.updateStatus", FakeConnection.success(Map.of(
                "loanNumber", LOAN_NUMBER,
                "status", LoanStatusService.PENDING)));
    }

    // a single-page customerAddress.list reply; each isMailing value ("y" or "n") is one address
    private void givenAddresses(String... isMailing) {
        givenAddressPage(1, isMailing.length, isMailing);
    }

    private void givenAddressPage(long page, long total, String... isMailing) {
        Map<String, String> values = listReply(total, isMailing.length);
        for (int i = 1; i <= isMailing.length; i++) {
            values.put(i + ":isMailing", isMailing[i - 1]);
        }
        connection.whenRequested("customerAddress.list", Map.of("page", Long.toString(page)),
                FakeConnection.success(values));
    }

    // a list reply with the given number of rows; only the count matters to these checks
    private void givenRows(String requestType, int rows) {
        connection.whenRequested(requestType, FakeConnection.success(listReply(rows, rows)));
    }

    private void givenCoborrowerTypes(String... types) {
        Map<String, String> values = listReply(types.length, types.length);
        for (int i = 1; i <= types.length; i++) {
            values.put(i + ":type", types[i - 1]);
        }
        connection.whenRequested("loanCoborrower.list", FakeConnection.success(values));
    }

    private static Map<String, String> listReply(long total, int items) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(items));
        for (int i = 1; i <= items; i++) {
            values.put(i + ":id", Integer.toString(i));
        }
        return values;
    }
}
