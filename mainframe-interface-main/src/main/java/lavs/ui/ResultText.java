package lavs.ui;

import lavs.logic.ServiceResult;
import lavs.validation.FieldError;

import java.io.UncheckedIOException;

// AI-assisted: the messages a screen shows for a ServiceResult, so every screen built on
// the gateway words its three outcomes the same way. No JavaFX, so it is unit tested.
public final class ResultText {
    public static final String NO_CONNECTION = "Couldn't reach the mainframe. Check your connection and try again.";
    public static final String FIX_FIELDS = "Please fix the highlighted fields.";

    private ResultText() {}

    // after a save or a delete; doneText is what worked, e.g. "Customer created."
    public static String forChange(ServiceResult<?> result, String doneText) {
        if (result.isSuccessful()) {
            return doneText;
        }
        if (result.getOutcome() == null) {
            return FIX_FIELDS;
        }
        if (result.getOutcome() == ServiceResult.Outcome.NETWORK_FAILURE) {
            return NO_CONNECTION;
        }
        return "The mainframe rejected the change: " + result.getMessage();
    }

    // when something couldn't be read; what names it, e.g. "The customer"
    public static String forLoad(ServiceResult<?> result, String what) {
        if (result.getOutcome() == ServiceResult.Outcome.NETWORK_FAILURE) {
            return NO_CONNECTION;
        }
        if (result.getOutcome() == null) {
            FieldError error = result.getValidationErrors().getErrors().get(0);
            return what + " could not be loaded: " + error.getFieldName() + " " + error.getMessage();
        }
        return what + " could not be loaded: " + result.getMessage();
    }

    // for something a service threw rather than returned
    public static String forError(Throwable failure) {
        if (failure instanceof UncheckedIOException) {
            return NO_CONNECTION;
        }
        return "Something went wrong: " + failure.getMessage();
    }

    // Validators messages read "is required"; under a field they start a sentence
    public static String capitalise(String message) {
        return message.isEmpty() ? message : message.substring(0, 1).toUpperCase() + message.substring(1);
    }
}
