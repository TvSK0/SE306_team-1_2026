package lavs.logic;

import lavs.data.*;
import lavs.model.PendingChange;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DocumentReplacementTests {
    @TempDir Path directory;
    private SqlitePendingChangeStore open() { return new SqlitePendingChangeStore(new LocalDatabase(directory.resolve("queue.db"))); }
    private void add(SqlitePendingChangeStore queue, RecordOwner owner) {
        queue.add(new PendingChange(owner.operation + ".replaceDocument", null, null, Map.of(
                owner.numberField, "1", "id", "5", "fileName", "new.pdf", "fileType", "pdf", "size", "42", "type", "General")));
    }
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void restartOnlyRetriesRemovalOnceReplacementWasAccepted(RecordOwner owner) {
        var queue = open(); add(queue, owner);
        var gateway = new FakeUploadGateway().answerSend(SendResult.success(), SendResult.networkFailure("offline"));
        assertEquals(SyncStep.Outcome.WAITING, new SyncProcessor(queue, gateway).processNext().orElseThrow().getOutcome());
        assertEquals("remove", open().all().get(0).getFields().get("stage"));
        assertEquals(SyncStep.Outcome.SENT, new SyncProcessor(open(), gateway).processNext().orElseThrow().getOutcome());
        assertEquals(java.util.List.of(owner.operation + ".addDocument", owner.operation + ".deleteDocument"), gateway.sent);
        assertNull(gateway.sentFields.get(0).get("id"));
        assertTrue(open().all().isEmpty());
    }
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void rejectedReplacementKeepsOriginal(RecordOwner owner) {
        var queue = open(); add(queue, owner);
        var gateway = new FakeUploadGateway().answerSend(SendResult.rejected("Bad file type"));
        assertEquals(SyncStep.Outcome.REJECTED, new SyncProcessor(queue, gateway).processNext().orElseThrow().getOutcome());
        assertTrue(gateway.sent.isEmpty());
    }
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void failedRemovalExplainsBothCopiesRemain(RecordOwner owner) {
        var queue = open(); add(queue, owner);
        var gateway = new FakeUploadGateway().answerSend(SendResult.success(), SendResult.rejected("Locked"));
        var result = new SyncProcessor(queue, gateway).processNext().orElseThrow();
        assertTrue(result.getMessage().contains("Replacement saved, but original metadata could not be removed"));
    }
}
