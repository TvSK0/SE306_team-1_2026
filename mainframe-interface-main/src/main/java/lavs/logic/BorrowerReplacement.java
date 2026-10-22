package lavs.logic;

import lavs.data.ChangeQueue;
import lavs.data.SendResult;
import lavs.data.UploadGateway;
import lavs.model.PendingChange;
import java.util.Map;

// AI-assisted: persist each completed stage so reconnecting does not repeat the removal.
final class BorrowerReplacement {
    private final ChangeQueue queue;
    private final UploadGateway gateway;
    BorrowerReplacement(ChangeQueue queue, UploadGateway gateway) {
        this.queue = queue;
        this.gateway = gateway;
    }

    SendResult send(PendingChange change) {
        var fields = change.getFields();
        String stage = fields.getOrDefault("stage", "remove");
        if ("remove".equals(stage)) {
            SendResult removed = gateway.send(new PendingChange("loan.deleteCoborrower", null, null,
                    Map.of("loanNumber", fields.get("loanNumber"), "id", fields.get("id"))));
            if (removed.getOutcome() != SendResult.Outcome.SUCCESS) return removed;
            change.setField("stage", "add");
            queue.update(change);
            stage = "add";
        }
        if ("add".equals(stage)) {
            SendResult added = add(fields, false);
            if (added.getOutcome() != SendResult.Outcome.REJECTED) return added;
            change.setField("stage", "restore");
            change.setField("reason", added.getMessage());
            queue.update(change);
        }
        SendResult restored = add(fields, true);
        if (restored.getOutcome() == SendResult.Outcome.NETWORK_FAILURE) return restored;
        String reason = fields.get("reason");
        return SendResult.rejected(reason + (restored.getOutcome() == SendResult.Outcome.SUCCESS
                ? ". The original borrower was restored."
                : ". The original borrower could not be restored: " + restored.getMessage()));
    }

    private SendResult add(Map<String, String> fields, boolean restore) {
        return gateway.send(new PendingChange("loan.addCoborrower", null, null, Map.of(
                "loanNumber", fields.get("loanNumber"),
                "coborrowerNumber", fields.get(restore ? "previousCustomer" : "coborrowerNumber"),
                "type", fields.get(restore ? "previousType" : "type"))));
    }
}
