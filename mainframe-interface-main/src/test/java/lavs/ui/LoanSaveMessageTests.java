// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.data.LocalDatabase;
import lavs.data.MainframeGateway;
import lavs.data.SqlitePendingChangeStore;
import lavs.logic.LoanDetailsInput;
import lavs.logic.LoanSaveResult;
import lavs.logic.LoanService;
import lavs.logic.SyncService;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class LoanSaveMessageTests {
    // AI-assisted: saves go through the upload queue (US11), kept in a SQLite file in a temp folder
    @TempDir
    Path folder;
    private SyncService _sync;

    private LoanService serviceOver(FakeConnection connection) {
        _sync = SyncService.create(new SqlitePendingChangeStore(new LocalDatabase(folder.resolve("lavs.db"))),
                new MainframeGateway(connection));
        return new LoanService(connection, _sync);
    }

    @AfterEach
    void closeSync() {
        if (_sync != null) {
            _sync.close();
        }
    }

    private static final LoanDetailsInput VALID = new LoanDetailsInput(
            "450000", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "2027-11-01");

    @Test
    void namesTheSavedLoanAndItsStatus() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.success(savedLoanReply()));

        // Act
        LoanSaveResult result = serviceOver(connection).createLoan("C-10234", VALID);

        // Assert
        Assertions.assertEquals("Loan L-5521 saved. Status: New", LoanSaveMessage.forResult(result));
    }

    @Test
    void asksToFixTheHighlightedFields() throws IOException {
        // Arrange
        LoanDetailsInput input = new LoanDetailsInput("", "Fixed", "6.29", "Yearly", "2786", "Monthly", "2026-11-01", "");

        // Act
        LoanSaveResult result = serviceOver(new FakeConnection()).createLoan("C-10234", input);

        // Assert
        Assertions.assertEquals("Please fix the highlighted fields.", LoanSaveMessage.forResult(result));
    }

    @Test
    // AI-assisted: since US11 a loan saved while the mainframe can't be reached is kept to upload later
    void saysALoanIsKeptOnThisLaptopWhenTheMainframeCannotBeReached() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        LoanSaveResult result = serviceOver(connection).createLoan("C-10234", VALID);

        // Assert
        Assertions.assertTrue(result.isSaved());
        Assertions.assertEquals("Loan saved. " + ResultText.WAITING_TO_UPLOAD, LoanSaveMessage.forResult(result));
    }

    @Test
    void passesOnTheMainframesReasonForARejection() throws IOException {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.create", FakeConnection.rejected("Unknown rate type"));

        // Act
        LoanSaveResult result = serviceOver(connection).createLoan("C-10234", VALID);

        // Assert
        Assertions.assertEquals(
                "The mainframe rejected the loan: Unknown rate type",
                LoanSaveMessage.forResult(result));
    }

    private static Map<String, String> savedLoanReply() {
        Map<String, String> reply = new HashMap<>();
        reply.put("loanNumber", "L-5521");
        reply.put("customerNumber", "C-10234");
        reply.put("status", "New");
        reply.put("principal", "450000");
        reply.put("rateType", "Fixed");
        reply.put("rateAmount", "6.29");
        reply.put("rateFrequency", "Yearly");
        reply.put("paymentAmount", "2786");
        reply.put("paymentFrequency", "Monthly");
        reply.put("startDate", "2026-11-01");
        reply.put("renewalDate", "2027-11-01");
        return reply;
    }
}
