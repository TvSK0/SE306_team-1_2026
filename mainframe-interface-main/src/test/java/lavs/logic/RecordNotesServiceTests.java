package lavs.logic;

import lavs.data.MainframeGateway;
import mainframe.client.FakeConnection;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// AI-assisted: both owner scopes exercise real protocol requests and failure handling.
class RecordNotesServiceTests {
    private final FakeConnection connection = new FakeConnection();
    private final RecordNotesService service = new RecordNotesService(new MainframeGateway(connection));
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void createsUpdatesAndDeletes(RecordOwner owner) {
        connection.whenRequested(owner.operation + ".addNote", FakeConnection.success(Map.of()));
        connection.whenRequested(owner.operation + ".updateNote", FakeConnection.success(Map.of()));
        connection.whenRequested(owner.operation + ".deleteNote", FakeConnection.success(Map.of()));
        Long id = null;
        assertTrue(service.save(owner, "C1", id, " General ", "Line one\nLine two").isSuccessful());
        id = 4L;
        assertTrue(service.save(owner, "C1", id, " General ", "Line one\nLine two").isSuccessful());
        assertTrue(service.delete(owner, "C1", 4).isSuccessful());
        var sent = connection.getSentRequests();
        assertEquals("C1", sent.get(0).getValue(owner.numberField));
        assertEquals("General", sent.get(0).getValue("type"));
        assertNull(sent.get(0).getValue("id"));
        assertEquals("4", sent.get(1).getValue("id"));
        assertEquals("4", sent.get(2).getValue("id"));
    }
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void invalidInputsNeverContactMainframe(RecordOwner owner) {
        assertFalse(service.save(owner, "", 0L, "", "").getValidationErrors().isValid());
        assertFalse(service.list(owner, " ").getValidationErrors().isValid());
        assertFalse(service.delete(owner, "C1", -1).getValidationErrors().isValid());
        assertTrue(connection.getSentRequests().isEmpty());
    }
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void readsEveryPageAndFullDetails(RecordOwner owner) {
        connection.whenRequested(owner.operation + "Note.list", Map.of("page", "1"),
                FakeConnection.success(Map.of("items", "1", "total", "2", "1:id", "4")));
        connection.whenRequested(owner.operation + "Note.list", Map.of("page", "2"),
                FakeConnection.success(Map.of("items", "1", "total", "2", "1:id", "5")));
        connection.whenRequested(owner.operation + "Note.retrieve", FakeConnection.success(Map.of("type", "General", "lines", "Line one\nLine two", "author", "Alex", "dateAdded", "2026-10-20")));
        var result = service.list(owner, "C1");
        assertTrue(result.isSuccessful());
        assertEquals(2, result.getValue().size());
        assertEquals("Line one\nLine two", result.getValue().get(0).lines());
        assertThrows(UnsupportedOperationException.class, () -> result.getValue().clear());
    }
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void emptyPageStopsEvenWithWrongTotal(RecordOwner owner) {
        connection.whenRequested(owner.operation + "Note.list", FakeConnection.success(Map.of("items", "0", "total", "100")));
        assertTrue(service.list(owner, "C1").getValue().isEmpty());
        assertEquals(1, connection.getSentRequests().size());
    }
    @ParameterizedTest @EnumSource(RecordOwner.class)
    void failedDetailDoesNotReturnPartialList(RecordOwner owner) {
        connection.whenRequested(owner.operation + "Note.list", FakeConnection.success(Map.of("items", "1", "total", "1", "1:id", "4")));
        connection.whenRequested(owner.operation + "Note.retrieve", FakeConnection.rejected("Denied"));
        var result = service.list(owner, "C1");
        assertEquals("Denied", result.getMessage());
        assertNull(result.getValue());
    }
    @Test void offlineListReportsNetworkFailure() {
        connection.setIsConnected(false);
        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, service.list(RecordOwner.CUSTOMER, "C1").getOutcome());
    }
    @Test void malformedListReportsUnreadableReply() {
        connection.whenRequested("customerNote.list", FakeConnection.success(Map.of("items", "bad")));
        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, service.list(RecordOwner.CUSTOMER, "C1").getOutcome());
    }
}
