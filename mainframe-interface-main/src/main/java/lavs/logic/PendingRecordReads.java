package lavs.logic;

import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import java.util.HashMap;
import java.util.function.Function;

// AI-assisted: reopen an unsent customer or loan from its durable queued fields after restart.
// Returning null leaves other reads to the mainframe; local data is never labelled uploaded.
public final class PendingRecordReads implements Function<MessageRequest, MessageResponse> {
    private final SyncService sync;
    public PendingRecordReads(SyncService sync) { this.sync = sync; }

    @Override public MessageResponse apply(MessageRequest request) {
        String number = request.getValue("number");
        String type = request.getRequestType();
        if (!TemporaryNumbers.isTemporary(number)
                || !("customer.retrieve".equals(type) || "loan.retrieve".equals(type))) return null;
        String prefix = type.substring(0, type.indexOf('.'));
        HashMap<String, String> fields = null;
        for (var change : sync.getChanges()) {
            if (number.equals(change.getCreatesNumber()) && (prefix + ".create").equals(change.getMessageType())) {
                fields = new HashMap<>(change.getFields());
            } else if (fields != null && (prefix + ".update").equals(change.getMessageType()) && change.references(number)) {
                fields.putAll(change.getFields());
            }
        }
        if (fields == null) return null;
        fields.put("customer".equals(prefix) ? "number" : "loanNumber", number);
        fields.put("status", "Waiting to upload");
        return new MessageResponse(new Status(0), fields);
    }
}
