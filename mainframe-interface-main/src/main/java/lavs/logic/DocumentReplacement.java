package lavs.logic;

import lavs.data.ChangeQueue;
import lavs.data.SendResult;
import lavs.data.UploadGateway;
import lavs.model.PendingChange;
import java.util.HashMap;
import java.util.Map;

// AI-assisted: retain original metadata until the replacement is accepted.
final class DocumentReplacement {
    private final ChangeQueue queue;
    private final UploadGateway gateway;
    DocumentReplacement(ChangeQueue queue, UploadGateway gateway) { this.queue = queue; this.gateway = gateway; }
    SendResult send(PendingChange change) {
        String prefix = change.getMessageType().startsWith("customer.") ? "customer" : "loan";
        String ownerField = prefix.equals("customer") ? "customer" : "loanNumber";
        var fields = change.getFields();
        if (!"remove".equals(fields.get("stage"))) {
            Map<String, String> addedFields = new HashMap<>(fields);
            addedFields.remove("id");
            SendResult added = gateway.send(new PendingChange(prefix + ".addDocument", change.getCustomerNumber(), null, addedFields));
            if (added.getOutcome() != SendResult.Outcome.SUCCESS) return added;
            change.setField("stage", "remove");
            queue.update(change);
        }
        SendResult removed = gateway.send(new PendingChange(prefix + ".deleteDocument", change.getCustomerNumber(), null,
                Map.of(ownerField, fields.get(ownerField), "id", fields.get("id"))));
        if (removed.getOutcome() == SendResult.Outcome.REJECTED) {
            return SendResult.rejected("Replacement saved, but original metadata could not be removed: " + removed.getMessage());
        }
        return removed;
    }
}
