package lavs.data;

// AI-assisted.
// Kept next to the gateway's GatewayResult because it also carries the number the mainframe
// gave a new record; MainframeGateway.send(PendingChange) returns one.

// What had happened when one queued change was sent (the three outcomes SUCCESS, NETWORK_FAILURE, REJECTED)
public final class SendResult {

    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED }

    private final Outcome outcome;
    private final String assignedNumber;
    private final String message;
    private mainframe.client.MessageResponse response;

    private SendResult(Outcome outcome, String assignedNumber, String message) {
        this.outcome = outcome;
        this.assignedNumber = assignedNumber;
        this.message = message;
    }

    public static SendResult success() {
        return new SendResult(Outcome.SUCCESS, null, null);
    }

    // Accepted and the mainframe assigned this number (eg new customer number or loan number)
    public static SendResult success(String assignedNumber) {
        return new SendResult(Outcome.SUCCESS, assignedNumber, null);
    }

    public static SendResult success(String number, mainframe.client.MessageResponse response) {
        SendResult result = success(number);
        result.response = response;
        return result;
    }

    public mainframe.client.MessageResponse getResponse() { return response; }

    public static SendResult networkFailure(String message) {
        return new SendResult(Outcome.NETWORK_FAILURE, null, message);
    }

    public static SendResult rejected(String message) {
        return new SendResult(Outcome.REJECTED, null, message);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    // The number the mainframe gave a new record, null if none
    public String getAssignedNumber() {
        return assignedNumber;
    }

    public String getMessage() {
        return message;
    }
}
