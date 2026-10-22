package lavs.ui;

import lavs.logic.LoanSaveResult;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

// The message shown at the top of the Loan Details screen after the MMM clicks Save.
// Kept free of JavaFX so it can be unit tested.
public final class LoanSaveMessage {
    private LoanSaveMessage() {}

    public static String forResult(LoanSaveResult result) {
        if (result.isSuccessful()) {
            // AI-assisted: an update saved through the upload queue has no status to show (US11)
            String status = result.getLoan().getStatus();
            return "Loan " + result.getLoan().getLoanNumber() + " saved." + (status == null ? "" : " Status: " + status);
        }
        // AI-assisted: kept on the upload queue; a new loan's temporary number isn't shown (US11)
        if (result.isWaitingToUpload()) {
            return "Loan saved. " + ResultText.WAITING_TO_UPLOAD;
        }
        Status status = result.getMainframeStatus();
        // no status means the field checks stopped the save before the mainframe was contacted
        if (status == null) {
            return "Please fix the highlighted fields.";
        }
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            return "Couldn't reach the mainframe. Check your connection and try again.";
        }
        return "The mainframe rejected the loan: " + status.getErrorMessage();
    }
}
