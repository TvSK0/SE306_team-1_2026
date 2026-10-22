package lavs.logic;

import lavs.model.PendingChange;

// Gives the outcome of one queued change:
// SENT = saved
// WAITING = waiting to upload
// REJECTED = shows the message against the change
// PAUSED = possible duplicate customer, MMM to resolve
public final class SyncStep {

    public enum Outcome { SENT, WAITING, REJECTED, PAUSED }

    private final PendingChange change;
    private final Outcome outcome;
    private final String message;
    private mainframe.client.MessageResponse response;

    public SyncStep(PendingChange change, Outcome outcome, String message) {
        this.change = change;
        this.outcome = outcome;
        this.message = message;
    }

    public PendingChange getChange() {
        return change;
    }

    // AI-assisted: preserve the actual upload reply for the service that queued this save.
    public SyncStep withResponse(mainframe.client.MessageResponse value) { response = value; return this; }
    public mainframe.client.MessageResponse getResponse() { return response; }

    public Outcome getOutcome() {
        return outcome;
    }

    // If SENT, returns number the mainframe assigned (if any) otherwise returns the reason. Can be null.
    public String getMessage() {
        return message;
    }
}
