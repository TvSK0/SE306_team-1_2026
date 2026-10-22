package lavs.ui;

import lavs.logic.LoanStatusResult;
import lavs.logic.LoanStatusService;
import lavs.logic.TemporaryNumbers;
import lavs.model.Loan;
import lavs.validation.FieldError;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.UncheckedIOException;
import java.util.stream.Collectors;

// AI-assisted: US9, what the "Submit for verification" button in the loan header does and
// says. Kept free of JavaFX so it can be unit tested; LoanScreenController copies it onto
// the screen.
public final class LoanSubmission {
    private LoanSubmission() {}

    // Shown for a loan that hasn't been saved yet (disabled, so the MMM knows it comes next)
    // and for a New loan; hidden once the loan has moved on, e.g. to Pending.
    public static boolean isShown(Loan loan) {
        return loan == null || isNew(loan);
    }

    // only a saved loan that is still New can be submitted. AI-assisted: and only once the
    // mainframe has it, since the checks read it there (US11)
    public static boolean isEnabled(Loan loan) {
        return loan != null && isNew(loan) && !TemporaryNumbers.isTemporary(loan.getLoanNumber());
    }

    public static String messageFor(LoanStatusResult result) {
        if (result.isSuccessful()) {
            return "Submitted for verification. Status: " + result.getLoanStatus();
        }
        // AI-assisted: a submission kept on the upload queue (US11)
        if (result.isWaitingToUpload()) {
            return "Submitted for verification. " + ResultText.WAITING_TO_UPLOAD;
        }
        Status status = result.getMainframeStatus();
        // no status means a check stopped it before the mainframe was asked to change anything
        if (status == null) {
            return "This loan can't be submitted yet:\n" + result.getValidationErrors().getErrors().stream()
                    .map(LoanSubmission::bullet)
                    .collect(Collectors.joining("\n"));
        }
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            return couldNotReachTheMainframe();
        }
        return "The mainframe didn't accept the submission: " + status.getErrorMessage();
    }

    // for something the service threw rather than returned
    public static String messageForError(Throwable failure) {
        if (failure instanceof UncheckedIOException) {
            return couldNotReachTheMainframe();
        }
        return "The loan could not be submitted: " + failure.getMessage();
    }

    private static boolean isNew(Loan loan) {
        return LoanStatusService.NEW.equals(loan.getStatus());
    }

    private static String couldNotReachTheMainframe() {
        return "Couldn't reach the mainframe. Check your connection and try again.";
    }

    // Validators-style messages read "the customer needs ..."; as a list item they start a sentence
    private static String bullet(FieldError error) {
        String message = error.getMessage();
        return "• " + message.substring(0, 1).toUpperCase() + message.substring(1);
    }
}
