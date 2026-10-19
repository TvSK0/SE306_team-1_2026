// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
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
            return "Loan " + result.getLoan().getLoanNumber() + " saved. Status: " + result.getLoan().getStatus();
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
