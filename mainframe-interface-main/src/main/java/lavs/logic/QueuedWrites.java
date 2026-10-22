package lavs.logic;

import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

// AI-assisted: how a service saves through the upload queue (US11). A save is built as the
// usual message (e.g. customer.AddAddress), so its fields are exactly what that message sends.
// The message is "sent" to a connection that only records the request, and the request's type
// and fields are queued on SyncService; MainframeGateway later sends them as that same request.
// Nothing is copied by hand, so a queued save can't drift from the message it stands for.
final class QueuedWrites {
    // the error code the recording connection answers with: anything but 0 is not a success,
    // so the message keeps the values the MMM entered instead of reading a reply
    static final int NOT_SENT = -1;

    private QueuedWrites() {
    }

    // The request a save message would send: its type (e.g. "customer.addAddress") and fields.
    static final class Request {
        final String type;
        final Map<String, String> fields;

        Request(String type, Map<String, String> fields) {
            this.type = type;
            this.fields = fields;
        }
    }

    // Builds the request the message would send, without sending it. Only a message that
    // changes the mainframe can be queued; a read has to go to the mainframe for its answer.
    static Request capture(Message message) {
        RecordingConnection connection = new RecordingConnection();
        try {
            message.send(connection);
        } catch (IOException e) {
            // the recording connection never throws, so this would be a fault in the message
            throw new IllegalStateException("Could not build the request for " + message.getClass().getName(), e);
        }
        MessageRequest request = connection._request;
        if (request == null || !MessageRequest.UPDATE.equals(request.getMessageType())) {
            throw new IllegalArgumentException("Only a message that changes the mainframe can be queued: "
                    + message.getClass().getName());
        }
        Map<String, String> fields = new LinkedHashMap<>();
        for (String key : request.getValueKeys()) {
            fields.put(key, request.getValue(key));
        }
        return new Request(request.getRequestType(), fields);
    }

    // Queues the save and waits for its first upload attempt. Called on a BackgroundTasks
    // thread, never the sync thread, so waiting is safe; WAITING comes straight back when the
    // mainframe can't be reached. A save that couldn't even be kept on this laptop comes back
    // REJECTED with the reason, rather than as an exception.
    static SyncStep queue(SyncService sync, Message message, String customerNumber, String createsNumber) {
        Request request = capture(message);
        try {
            return sync.queue(request.type, customerNumber, createsNumber, request.fields).join();
        } catch (CompletionException e) {
            // SyncService fails the future with what went wrong, which join() wraps
            return new SyncStep(null, SyncStep.Outcome.REJECTED,
                    "Could not keep the change on this laptop: " + e.getCause().getMessage());
        }
    }

    // For the older result types that carry a mainframe Status (relationships, loans,
    // co-borrowers, loan status), the Status that stands for a queued save, so the screens that
    // word their messages from a Status keep working: sent reads as a success, rejected as the
    // mainframe refusing it, and still waiting as the mainframe not being reachable.
    static Status statusFor(SyncStep step) {
        switch (step.getOutcome()) {
            case SENT:
                return new Status(0);
            case REJECTED:
                return new Status(StatusMessage.SERVER_FAILURE.getCode(), step.getMessage(), 0);
            default:
                return new Status(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), step.getMessage(), 0);
        }
    }

    // true when the save is kept on this laptop to upload later: offline, or held behind a
    // possible-duplicate customer until the MMM decides
    static boolean isWaiting(SyncStep step) {
        return step.getOutcome() == SyncStep.Outcome.WAITING || step.getOutcome() == SyncStep.Outcome.PAUSED;
    }

    // what a result keeps of the saved record: all of it unless the save was rejected
    static <T> T keptValue(SyncStep step, T value) {
        return step.getOutcome() == SyncStep.Outcome.REJECTED ? null : value;
    }

    // A Connection that keeps the one request a message sends and answers "not sent".
    private static final class RecordingConnection implements Connection {
        private MessageRequest _request;

        @Override
        public MessageResponse send(MessageRequest request) {
            if (_request != null) {
                throw new IllegalStateException("A save message sends one request");
            }
            _request = request;
            return new MessageResponse(new Status(NOT_SENT, "Recorded for the upload queue, not sent", 0),
                    new HashMap<>());
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public Status Login(String username, String password) {
            throw new UnsupportedOperationException("A save message never logs in");
        }

        @Override
        public void Logout() {
        }

        @Override
        public void close() {
        }
    }
}
