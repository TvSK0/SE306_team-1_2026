package lavs.logic;

import lavs.data.LocalSaveStatus;
import mainframe.client.Status;

// AI-assisted: UI-facing interpretation of local saves, separate from uploaded success.
public final class SaveStatus {
    private SaveStatus() {}
    public static boolean isQueued(Status status) {
        return status != null && (status.getErrorCode() == LocalSaveStatus.WAITING
                || status.getErrorCode() == LocalSaveStatus.PAUSED);
    }
    public static String message(Status status) {
        return status.getErrorCode() == LocalSaveStatus.PAUSED
                ? "Saved locally. Resolve the possible duplicate in Pending uploads."
                : "Saved locally. Waiting to upload.";
    }
}
