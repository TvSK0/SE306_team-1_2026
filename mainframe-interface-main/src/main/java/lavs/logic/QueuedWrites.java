package lavs.logic;

import lavs.data.LocalSaveStatus;
import lavs.data.WriteDispatcher;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import java.util.HashMap;
import java.util.concurrent.CompletionException;

// AI-assisted: writes are durable before any upload. Called only from BackgroundTasks.
public final class QueuedWrites implements WriteDispatcher {
    private final SyncService sync;
    public QueuedWrites(SyncService sync) { this.sync = sync; }

    @Override public MessageResponse send(MessageRequest request) {
        if (!MessageRequest.UPDATE.equals(request.getMessageType())) {
            throw new IllegalArgumentException("Only writes belong in the upload queue");
        }
        HashMap<String, String> fields = new HashMap<>();
        for (String key : request.getValueKeys()) fields.put(key, request.getValue(key));
        String type = request.getRequestType();
        boolean customerCreate = "customer.create".equals(type);
        boolean loanCreate = "loan.create".equals(type);
        String temporary = customerCreate || loanCreate ? TemporaryNumbers.create() : null;
        String customer = customerCreate ? temporary : fields.get("customer");
        if (customer == null) customer = fields.get("customerNumber");
        if (customer == null && type.startsWith("customer.")) customer = fields.get("number");
        SyncStep step;
        try {
            // Do not use a timeout: a timed-out UI retry could enqueue the same write twice.
            step = sync.queue(type, customer, temporary, fields).join();
        } catch (CompletionException error) {
            throw new IllegalStateException("The change could not be saved locally", error.getCause());
        }
        if (step.getOutcome() == SyncStep.Outcome.SENT) {
            if (step.getResponse() == null) throw new IllegalStateException("The upload returned no response");
            return step.getResponse();
        }
        if (step.getOutcome() == SyncStep.Outcome.REJECTED) {
            return new MessageResponse(new Status(StatusMessage.SERVER_FAILURE.getCode(), step.getMessage(),
                    step.getChange().getId()), new HashMap<>());
        }
        HashMap<String, String> local = new HashMap<>(fields);
        if (customerCreate) { local.put("number", temporary); local.put("status", "Waiting to upload"); }
        if (loanCreate) { local.put("loanNumber", temporary); local.put("status", "Waiting to upload"); }
        int code = step.getOutcome() == SyncStep.Outcome.PAUSED ? LocalSaveStatus.PAUSED : LocalSaveStatus.WAITING;
        return new MessageResponse(new Status(code, step.getMessage(), step.getChange().getId()), local);
    }
}
