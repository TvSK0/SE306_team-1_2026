// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.logic.LoanStatusResult;
import lavs.logic.LoanStatusService;
import lavs.model.Loan;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. The results come from the real LoanStatusService over a FakeConnection, so
// these also check that the screen reads them the way the service builds them.
class LoanSubmissionTests {
    private static final String LOAN_NUMBER = "L-5521";

    private final FakeConnection connection = new FakeConnection();

    // --- the button ---

    @Test
    void showsADisabledButtonForALoanThatIsNotSavedYet() {
        assertTrue(LoanSubmission.isShown(null));
        assertFalse(LoanSubmission.isEnabled(null));
    }

    @Test
    void enablesTheButtonForASavedNewLoan() {
        Loan loan = loanWithStatus(LoanStatusService.NEW);

        assertTrue(LoanSubmission.isShown(loan));
        assertTrue(LoanSubmission.isEnabled(loan));
    }

    @Test
    void hidesTheButtonOnceTheLoanHasMovedOn() {
        Loan loan = loanWithStatus(LoanStatusService.PENDING);

        assertFalse(LoanSubmission.isShown(loan));
        assertFalse(LoanSubmission.isEnabled(loan));
    }

    // --- what it reports ---

    @Test
    void confirmsTheNewStatus() throws IOException {
        givenLoanThatPassesEveryCheck();
        connection.whenRequested("loan.updateStatus",
                FakeConnection.success(Map.of("loanNumber", LOAN_NUMBER, "status", "Pending")));

        String message = LoanSubmission.messageFor(submit());

        assertEquals("Submitted for verification. Status: Pending", message);
    }

    @Test
    void listsEverythingThatStopsTheSubmission() throws IOException {
        givenLoan();
        givenRows("customerAddress.list", 0);
        givenRows("customerPhoneNumber.list", 0);
        givenRows("customerEmail.list", 0);
        givenRows("customerEmployer.list", 0);
        givenRows("loanCoborrower.list", 0);

        String message = LoanSubmission.messageFor(submit());

        assertEquals("""
                This loan can't be submitted yet:
                • The customer needs a mailing address
                • The customer needs a phone number or email
                • The customer has no employer, so the loan needs a guarantor""", message);
    }

    @Test
    void saysWhenTheMainframeCannotBeReached() throws IOException {
        connection.whenRequested("loan.retrieve", FakeConnection.networkDown());

        String message = LoanSubmission.messageFor(submit());

        assertEquals("Couldn't reach the mainframe. Check your connection and try again.", message);
    }

    @Test
    void passesOnWhyTheMainframeRefused() throws IOException {
        givenLoanThatPassesEveryCheck();
        connection.whenRequested("loan.updateStatus", FakeConnection.rejected("loan is locked"));

        String message = LoanSubmission.messageFor(submit());

        assertEquals("The mainframe didn't accept the submission: loan is locked", message);
    }

    @Test
    void treatsAnUncheckedIOExceptionAsTheMainframeBeingUnreachable() {
        String message = LoanSubmission.messageForError(new UncheckedIOException(new IOException("timed out")));

        assertEquals("Couldn't reach the mainframe. Check your connection and try again.", message);
    }

    @Test
    void reportsAnythingElseThatWentWrong() {
        String message = LoanSubmission.messageForError(new IllegalStateException("no response scripted"));

        assertEquals("The loan could not be submitted: no response scripted", message);
    }

    // --- helpers ---

    private LoanStatusResult submit() throws IOException {
        return new LoanStatusService(connection).submitForVerification(LOAN_NUMBER);
    }

    private static Loan loanWithStatus(String status) {
        Loan loan = new Loan();
        loan.setLoanNumber(LOAN_NUMBER);
        loan.setStatus(status);
        return loan;
    }

    // the starter loan.Retrieve needs the three amounts present, as the real server always sends them
    private void givenLoan() {
        connection.whenRequested("loan.retrieve", FakeConnection.success(Map.of(
                "loanNumber", LOAN_NUMBER, "customerNumber", "C-10234", "status", "New",
                "principal", "450000", "rateAmount", "6.29", "paymentAmount", "2786")));
    }

    private void givenLoanThatPassesEveryCheck() {
        givenLoan();
        Map<String, String> mailingAddress = listReply(1);
        mailingAddress.put("1:isMailing", "y");
        connection.whenRequested("customerAddress.list", FakeConnection.success(mailingAddress));
        givenRows("customerPhoneNumber.list", 1);
        givenRows("customerEmployer.list", 1);
        givenRows("loanCoborrower.list", 0);
    }

    private void givenRows(String requestType, int rows) {
        connection.whenRequested(requestType, FakeConnection.success(listReply(rows)));
    }

    private static Map<String, String> listReply(int rows) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Integer.toString(rows));
        values.put("items", Integer.toString(rows));
        for (int i = 1; i <= rows; i++) {
            values.put(i + ":id", Integer.toString(i));
        }
        return values;
    }
}
