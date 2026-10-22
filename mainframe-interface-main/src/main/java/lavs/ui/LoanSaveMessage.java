package lavs.ui;

import lavs.logic.LoanSaveResult;
import lavs.logic.MainframeReply;

// The message shown at the top of the Loan Details screen after the MMM clicks Save.
// Kept free of JavaFX so it can be unit tested.
public final class LoanSaveMessage {
    private LoanSaveMessage() {}

    public static String forResult(LoanSaveResult result) {
        MainframeReply reply = result.getReply();
        if (result.isQueued()) return reply.getQueuedMessage();
        if (result.isSuccessful()) {
            return "Loan " + result.getLoan().getLoanNumber() + " saved. Status: " + result.getLoan().getStatus();
        }
        // no reply means the field checks stopped the save before the mainframe was contacted
        if (!reply.wasAnswered()) {
            return "Please fix the highlighted fields.";
        }
        if (reply.isNetworkFailure()) {
            return "Couldn't reach the mainframe. Check your connection and try again.";
        }
        return "The mainframe rejected the loan: " + reply.getErrorMessage();
    }
}
