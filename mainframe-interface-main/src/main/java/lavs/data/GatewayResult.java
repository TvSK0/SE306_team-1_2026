package lavs.data;

import mainframe.client.Status;
import mainframe.client.StatusMessage;

// AI-assisted: what happened when the gateway sent a message, as one of the three results
// every service has to handle: SUCCESS, NETWORK_FAILURE (try again later; a queued change
// stays queued) or REJECTED (the mainframe refused it; show its message, never retry).
public final class GatewayResult {

    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED, WAITING, PAUSED }

    private final Outcome outcome;
    private final Status status;
    private mainframe.client.MessageResponse response;

    private GatewayResult(Outcome outcome, Status status) {
        this.outcome = outcome;
        this.status = status;
    }

    // Sorts a Status into the three outcomes. A reply that couldn't be read counts as a
    // network failure, not a rejection: it usually means something between the laptop and
    // the mainframe answered instead (e.g. a proxy error page), so trying again is right.
    public static GatewayResult from(Status status) {
        if (status.getWasSuccessful()) {
            return new GatewayResult(Outcome.SUCCESS, status);
        }
        Integer code = status.getErrorCode();
        if (code == LocalSaveStatus.WAITING) return new GatewayResult(Outcome.WAITING, status);
        if (code == LocalSaveStatus.PAUSED) return new GatewayResult(Outcome.PAUSED, status);
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(code)
                || StatusMessage.UNABLE_TO_PARSE.getCode().equals(code)) {
            return new GatewayResult(Outcome.NETWORK_FAILURE, status);
        }
        return new GatewayResult(Outcome.REJECTED, status);
    }

    // a failure that never produced a Status, e.g. the message threw an IOException
    static GatewayResult networkFailure(String message) {
        return new GatewayResult(Outcome.NETWORK_FAILURE,
                new Status(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), message, 0));
    }

    GatewayResult withResponse(mainframe.client.MessageResponse response) {
        this.response = response;
        return this;
    }

    public String getValue(String key) { return response == null ? null : response.getValue(key); }

    public boolean isQueued() { return outcome == Outcome.WAITING || outcome == Outcome.PAUSED; }

    public Outcome getOutcome() {
        return outcome;
    }

    public boolean isSuccess() {
        return outcome == Outcome.SUCCESS;
    }

    // why it failed, as the mainframe or the network put it; null on success
    public String getMessage() {
        return status.getErrorMessage();
    }

    // the Status underneath, for screens that already word their messages from one
    public Status getStatus() {
        return status;
    }
}
