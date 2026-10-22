package lavs.logic;

import lavs.data.*;
import lavs.model.PendingChange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class BorrowerReplacementTests {
    @TempDir Path directory;
    private SqlitePendingChangeStore open() {
        return new SqlitePendingChangeStore(new LocalDatabase(directory.resolve("queue.db")));
    }
    private PendingChange add(SqlitePendingChangeStore queue) {
        return queue.add(new PendingChange("loan.replaceCoborrower", null, null, Map.of(
                "loanNumber", "L1", "id", "5", "coborrowerNumber", "C2", "type", "Guarantor",
                "previousCustomer", "C1", "previousType", "Coborrower")));
    }
    @Test void resumesAdditionAfterRestartWithoutDeletingAgain() {
        var queue = open(); add(queue);
        var gateway = new FakeUploadGateway().answerSend(SendResult.success(), SendResult.networkFailure("offline"));
        assertEquals(SyncStep.Outcome.WAITING, new SyncProcessor(queue, gateway).processNext().orElseThrow().getOutcome());
        assertEquals("add", open().all().get(0).getFields().get("stage"));
        new SyncProcessor(open(), gateway).processNext();
        assertEquals(java.util.List.of("loan.deleteCoborrower", "loan.addCoborrower"), gateway.sent);
        assertTrue(open().all().isEmpty());
    }
    @Test void resumesRestorationAfterRestart() {
        var queue = open(); add(queue);
        var gateway = new FakeUploadGateway().answerSend(SendResult.success(), SendResult.rejected("Invalid type"),
                SendResult.networkFailure("offline"));
        new SyncProcessor(queue, gateway).processNext();
        assertEquals("restore", open().all().get(0).getFields().get("stage"));
        var result = new SyncProcessor(open(), gateway).processNext().orElseThrow();
        assertEquals(SyncStep.Outcome.REJECTED, result.getOutcome());
        assertTrue(result.getMessage().contains("original borrower was restored"));
        assertEquals("C1", gateway.sentFields.get(1).get("coborrowerNumber"));
    }
    @Test void failedRemovalDoesNotAdd() {
        var queue = open(); add(queue);
        var gateway = new FakeUploadGateway().answerSend(SendResult.rejected("Cannot remove"));
        assertEquals(SyncStep.Outcome.REJECTED, new SyncProcessor(queue, gateway).processNext().orElseThrow().getOutcome());
        assertTrue(gateway.sent.isEmpty());
    }
    @Test void failedRestorationExplainsWhatNeedsRepair() {
        var queue = open(); add(queue);
        var gateway = new FakeUploadGateway().answerSend(SendResult.success(), SendResult.rejected("Invalid"), SendResult.rejected("Locked"));
        var result = new SyncProcessor(queue, gateway).processNext().orElseThrow();
        assertTrue(result.getMessage().contains("could not be restored: Locked"));
    }
}
