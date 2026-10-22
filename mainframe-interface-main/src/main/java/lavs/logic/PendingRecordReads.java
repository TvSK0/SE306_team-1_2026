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

    // AI-assisted: split into isUnsentRecordRead and queuedFields to keep each method's
    // complexity under 10 (metrics baseline); the behaviour is unchanged.
    @Override public MessageResponse apply(MessageRequest request) {
        String number = request.getValue("number");
        String type = request.getRequestType();
        if (!isUnsentRecordRead(number, type)) return null;
        String prefix = type.substring(0, type.indexOf('.'));
        HashMap<String, String> fields = queuedFields(prefix, number);
        if (fields == null) return null;
        fields.put("customer".equals(prefix) ? "number" : "loanNumber", number);
        fields.put("status", "Waiting to upload");
        return new MessageResponse(new Status(0), fields);
    }

    // reading a customer or loan that still has its temporary number
    private static boolean isUnsentRecordRead(String number, String type) {
        return TemporaryNumbers.isTemporary(number)
                && ("customer.retrieve".equals(type) || "loan.retrieve".equals(type));
    }

    // the record's queued create, with any queued updates on top; null when it isn't queued
    private HashMap<String, String> queuedFields(String prefix, String number) {
        HashMap<String, String> fields = null;
        for (var change : sync.getChanges()) {
            if (number.equals(change.getCreatesNumber()) && (prefix + ".create").equals(change.getMessageType())) {
                fields = new HashMap<>(change.getFields());
            } else if (fields != null && (prefix + ".update").equals(change.getMessageType()) && change.references(number)) {
                fields.putAll(change.getFields());
            }
        }
        return fields;
    }
}
