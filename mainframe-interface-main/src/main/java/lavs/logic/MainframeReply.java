package lavs.logic;

import mainframe.client.Status;
import mainframe.client.StatusMessage;

// AI-assisted: what a screen needs to know about the mainframe's answer to a request: was it
// kept to upload later, did the network fail, or what reason did the mainframe give. The ui
// layer reads this instead of mainframe.client.Status, so it never depends on the client.
public final class MainframeReply {
    private static final MainframeReply NO_REPLY = new MainframeReply(null);

    private final Status _status;

    private MainframeReply(Status status) {
        _status = status;
    }

    // status may be null: the field checks stopped the request before the mainframe was asked
    public static MainframeReply of(Status status) {
        return status == null ? NO_REPLY : new MainframeReply(status);
    }

    // false when nothing was sent, because the field checks stopped the request first
    public boolean wasAnswered() {
        return _status != null;
    }

    // saved on this laptop and waiting in the upload queue (or held for the MMM)
    public boolean isQueued() {
        return SaveStatus.isQueued(_status);
    }

    // e.g. "Saved locally. Waiting to upload."; only meaningful when isQueued()
    public String getQueuedMessage() {
        return SaveStatus.message(_status);
    }

    // the mainframe couldn't be reached
    public boolean isNetworkFailure() {
        return _status != null && StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(_status.getErrorCode());
    }

    // the reason the mainframe gave, e.g. "Unknown customer"; null when it wasn't asked
    public String getErrorMessage() {
        return _status == null ? null : _status.getErrorMessage();
    }
}
