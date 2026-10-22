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

    // AI-assisted: split into fieldsOf, customerFor, queue, responseFor and localResponse to keep
    // each method's complexity under 10 (metrics baseline); the behaviour is unchanged.
    @Override public MessageResponse send(MessageRequest request) {
        if (!MessageRequest.UPDATE.equals(request.getMessageType())) {
            throw new IllegalArgumentException("Only writes belong in the upload queue");
        }
        HashMap<String, String> fields = fieldsOf(request);
        String type = request.getRequestType();
        String temporary = createdNumberField(type) != null ? TemporaryNumbers.create() : null;
        SyncStep step = queue(type, customerFor(type, temporary, fields), temporary, fields);
        return responseFor(step, type, temporary, fields);
    }

    private static HashMap<String, String> fieldsOf(MessageRequest request) {
        HashMap<String, String> fields = new HashMap<>();
        for (String key : request.getValueKeys()) fields.put(key, request.getValue(key));
        return fields;
    }

    // the field a new customer's or loan's number goes in; null for a change that creates nothing
    private static String createdNumberField(String type) {
        if ("customer.create".equals(type)) return "number";
        if ("loan.create".equals(type)) return "loanNumber";
        return null;
    }

    // the customer the change belongs to: a new customer is its own temporary number
    private static String customerFor(String type, String temporary, HashMap<String, String> fields) {
        if ("customer.create".equals(type)) return temporary;
        String customer = fields.get("customer");
        if (customer == null) customer = fields.get("customerNumber");
        if (customer == null && type.startsWith("customer.")) customer = fields.get("number");
        return customer;
    }

    private SyncStep queue(String type, String customer, String temporary, HashMap<String, String> fields) {
        try {
            // Do not use a timeout: a timed-out UI retry could enqueue the same write twice.
            return sync.queue(type, customer, temporary, fields).join();
        } catch (CompletionException error) {
            throw new IllegalStateException("The change could not be saved locally", error.getCause());
        }
    }

    private static MessageResponse responseFor(SyncStep step, String type, String temporary, HashMap<String, String> fields) {
        if (step.getOutcome() == SyncStep.Outcome.SENT) {
            if (step.getResponse() == null) throw new IllegalStateException("The upload returned no response");
            return step.getResponse();
        }
        if (step.getOutcome() == SyncStep.Outcome.REJECTED) {
            return new MessageResponse(new Status(StatusMessage.SERVER_FAILURE.getCode(), step.getMessage(),
                    step.getChange().getId()), new HashMap<>());
        }
        return localResponse(step, type, temporary, fields);
    }

    // kept on the laptop: answer with what was saved, a new record under its temporary number
    private static MessageResponse localResponse(SyncStep step, String type, String temporary, HashMap<String, String> fields) {
        HashMap<String, String> local = new HashMap<>(fields);
        String numberField = createdNumberField(type);
        if (numberField != null) { local.put(numberField, temporary); local.put("status", "Waiting to upload"); }
        int code = step.getOutcome() == SyncStep.Outcome.PAUSED ? LocalSaveStatus.PAUSED : LocalSaveStatus.WAITING;
        return new MessageResponse(new Status(code, step.getMessage(), step.getChange().getId()), local);
    }
}
