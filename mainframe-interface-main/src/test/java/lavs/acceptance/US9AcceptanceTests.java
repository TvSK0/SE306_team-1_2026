package lavs.acceptance;

import lavs.data.MainframeGateway;
import lavs.logic.LoanBorrowerService;
import lavs.logic.LoanStatusResult;
import lavs.logic.LoanStatusService;
import lavs.ui.LoanSubmission;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. US9 "Change a loan's status": one test per GIVEN/WHEN/THEN in the proposal, run
// against the real service with only the mainframe replaced by a FakeConnection. The loan has
// a main borrower (C-1) with an employer, and a co-borrower (C-2, Mere Brown).
@DisplayName("US9: Change a loan's status")
class US9AcceptanceTests {
    private static final String LOAN = "L-5521";

    private final FakeConnection mainframe = new FakeConnection();
    private final LoanStatusService service = new LoanStatusService(new MainframeGateway(mainframe));

    @Test
    @DisplayName("GIVEN every borrower on the loan has a mailing address and a phone number or email, "
            + "WHEN the MMM confirms the change, THEN the loan's status becomes Pending")
    void aLoanWhoseBorrowersCanAllBeReachedBecomesPending() throws IOException {
        givenCoborrowerDetails(true, false, true);
        givenMainBorrowerIsComplete();
        mainframe.whenRequested("loan.updateStatus", FakeConnection.success(Map.of("loanNumber", LOAN, "status", "Pending")));

        LoanStatusResult result = service.submitForVerification(LOAN);

        assertTrue(result.isSuccessful());
        assertEquals("Pending", result.getLoanStatus());
        assertEquals("Submitted for verification. Status: Pending", LoanSubmission.messageFor(result));
    }

    @Test
    @DisplayName("GIVEN a co-borrower is missing one of these, WHEN the MMM tries to change the status, "
            + "THEN the application stops them and says who needs updating")
    void aCoborrowerMissingDetailsStopsTheChangeAndIsNamed() throws IOException {
        givenCoborrowerDetails(false, true, false);
        givenMainBorrowerIsComplete();

        LoanStatusResult result = service.submitForVerification(LOAN);

        assertFalse(result.isSuccessful());
        assertEquals(List.of("co-borrower Mere Brown (C-2) needs a mailing address"),
                result.getValidationErrors().getErrors().stream().map(FieldError::getMessage).toList());
        assertTrue(mainframe.getSentRequests().stream().noneMatch(request -> "loan.updateStatus".equals(request.getRequestType())),
                "the status must not change");
        assertEquals("This loan can't be submitted yet:\n• Co-borrower Mere Brown (C-2) needs a mailing address",
                LoanSubmission.messageFor(result));
    }

    // --- the mainframe's replies ---

    // the co-borrower's replies are scripted by customer number, so call this first
    private void givenCoborrowerDetails(boolean mailing, boolean phone, boolean email) {
        Map<String, String> addresses = list(1);
        addresses.put("1:isMailing", mailing ? "y" : "n");
        mainframe.whenRequested("customerAddress.list", Map.of("number", "C-2"), FakeConnection.success(addresses));
        mainframe.whenRequested("customerPhoneNumber.list", Map.of("number", "C-2"), FakeConnection.success(list(phone ? 1 : 0)));
        mainframe.whenRequested("customerEmail.list", Map.of("number", "C-2"), FakeConnection.success(list(email ? 1 : 0)));
    }

    private void givenMainBorrowerIsComplete() {
        mainframe.whenRequested("loan.retrieve", FakeConnection.success(Map.of("loanNumber", LOAN, "customerNumber", "C-1",
                "status", "New", "principal", "450000", "rateAmount", "6.29", "paymentAmount", "2786")));
        Map<String, String> addresses = list(1);
        addresses.put("1:isMailing", "y");
        mainframe.whenRequested("customerAddress.list", FakeConnection.success(addresses));
        mainframe.whenRequested("customerPhoneNumber.list", FakeConnection.success(list(1)));
        mainframe.whenRequested("customerEmployer.list", FakeConnection.success(list(1)));
        Map<String, String> others = list(1);
        others.put("1:coborrowerNumber", "C-2");
        others.put("1:coborrowerName", "Mere Brown");
        others.put("1:type", LoanBorrowerService.COBORROWER);
        mainframe.whenRequested("loanCoborrower.list", FakeConnection.success(others));
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
