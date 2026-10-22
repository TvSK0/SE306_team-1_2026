// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.logic.LoanBorrowerListResult;
import lavs.logic.LoanBorrowerResult;
import lavs.logic.LoanBorrowerService;
import lavs.model.LoanCoborrower;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. The results come from the real LoanBorrowerService over a FakeConnection, so
// these also check that the tab reads them the way the service builds them.
class BorrowersTextTests {
    private static final String LOAN_NUMBER = "L-5521";

    private final FakeConnection connection = new FakeConnection();
    private final LoanBorrowerService service = new LoanBorrowerService(connection);

    // --- types ---

    @Test
    void namesEachTypeTheWayTheMmmReadsIt() {
        assertEquals("Co-borrower", BorrowersText.typeLabel(LoanBorrowerService.COBORROWER));
        assertEquals("Guarantor", BorrowersText.typeLabel(LoanBorrowerService.GUARANTOR));
        assertEquals("Cosigner", BorrowersText.typeLabel("Cosigner"));
        assertEquals("", BorrowersText.typeLabel(null));
    }

    @Test
    void switchesBetweenTheTwoTypes() {
        assertEquals(LoanBorrowerService.GUARANTOR, BorrowersText.otherType(LoanBorrowerService.COBORROWER));
        assertEquals(LoanBorrowerService.COBORROWER, BorrowersText.otherType(LoanBorrowerService.GUARANTOR));
    }

    @Test
    void theChangeButtonSaysWhatItWillDo() {
        assertEquals("Change type", BorrowersText.changeTypeButton(null));
        assertEquals("Make guarantor", BorrowersText.changeTypeButton(borrower("Mere Brown", LoanBorrowerService.COBORROWER)));
        assertEquals("Make co-borrower", BorrowersText.changeTypeButton(borrower("Mere Brown", LoanBorrowerService.GUARANTOR)));
    }

    // --- names ---

    @Test
    void usesTheNameAndFallsBackToTheCustomerNumber() {
        assertEquals("Mere Brown", BorrowersText.name(borrower("Mere Brown", LoanBorrowerService.COBORROWER)));
        assertEquals("C-2", BorrowersText.name(borrower(null, LoanBorrowerService.COBORROWER)));
        assertEquals("C-2", BorrowersText.name(borrower("  ", LoanBorrowerService.COBORROWER)));
    }

    @Test
    void confirmsARemovalByName() {
        assertEquals("Remove Mere Brown from this loan?",
                BorrowersText.confirmRemove(borrower("Mere Brown", LoanBorrowerService.GUARANTOR)));
    }

    // --- after a change ---

    @Test
    void saysWhatEachChangeDid() {
        LoanCoborrower mere = borrower("Mere Brown", LoanBorrowerService.COBORROWER);

        assertEquals("Added C-7 as a guarantor.", BorrowersText.added("C-7", LoanBorrowerService.GUARANTOR));
        assertEquals("Mere Brown is now a guarantor.", BorrowersText.changed(mere, LoanBorrowerService.GUARANTOR));
        assertEquals("Removed Mere Brown from the loan.", BorrowersText.removed(mere));
    }

    @Test
    void showsTheSuccessTextWhenTheChangeWorked() throws IOException {
        connection.whenRequested("loan.addCoborrower", FakeConnection.success(Map.of("id", "8")));

        LoanBorrowerResult result = service.addCoborrower(LOAN_NUMBER, "C-7", LoanBorrowerService.GUARANTOR);

        assertEquals("Added.", BorrowersText.forChange(result, "Added."));
    }

    @Test
    void asksToFixTheFieldsWhenTheChecksStoppedIt() throws IOException {
        LoanBorrowerResult result = service.addCoborrower(LOAN_NUMBER, "", null);

        assertEquals("Please fix the highlighted fields.", BorrowersText.forChange(result, "Added."));
    }

    @Test
    void passesOnWhyTheMainframeRefusedAChange() throws IOException {
        connection.whenRequested("loan.addCoborrower", FakeConnection.rejected("customer is already on this loan"));

        LoanBorrowerResult result = service.addCoborrower(LOAN_NUMBER, "C-7", LoanBorrowerService.GUARANTOR);

        assertEquals("The mainframe didn't accept the change: customer is already on this loan",
                BorrowersText.forChange(result, "Added."));
    }

    @Test
    void saysWhenTheMainframeCannotBeReachedForAChange() throws IOException {
        connection.whenRequested("loan.deleteCoborrower", FakeConnection.networkDown());

        LoanBorrowerResult result = service.removeCoborrower(7, LOAN_NUMBER);

        assertEquals("Couldn't reach the mainframe. Check your connection and try again.",
                BorrowersText.forChange(result, "Removed."));
    }

    // --- reading the list ---

    @Test
    void explainsAListTheMainframeRefused() throws IOException {
        connection.whenRequested("loanCoborrower.list", FakeConnection.rejected("no such loan"));

        LoanBorrowerListResult result = service.listCoborrowers(LOAN_NUMBER);

        assertEquals("The mainframe couldn't list the co-borrowers and guarantors: no such loan",
                BorrowersText.forListFailure(result.getReply()));
    }

    @Test
    void saysWhenTheMainframeCannotBeReachedForTheList() throws IOException {
        connection.whenRequested("loanCoborrower.list", FakeConnection.networkDown());

        LoanBorrowerListResult result = service.listCoborrowers(LOAN_NUMBER);

        assertEquals("Couldn't reach the mainframe. Check your connection and try again.",
                BorrowersText.forListFailure(result.getReply()));
    }

    // --- something thrown ---

    @Test
    void treatsAnUncheckedIOExceptionAsTheMainframeBeingUnreachable() {
        assertEquals("Couldn't reach the mainframe. Check your connection and try again.",
                BorrowersText.forError(new UncheckedIOException(new IOException("timed out"))));
    }

    @Test
    void reportsAnythingElseThatWentWrong() {
        assertEquals("Something went wrong: no response scripted",
                BorrowersText.forError(new IllegalStateException("no response scripted")));
    }

    private static LoanCoborrower borrower(String name, String type) {
        LoanCoborrower borrower = new LoanCoborrower(LOAN_NUMBER, "C-2", type);
        borrower.setId(7L);
        borrower.setCoborrowerName(name);
        return borrower;
    }
}
