// AI-assisted: the Figma 11 wording and the save decisions of the Documents panel.
package lavs.ui;

import lavs.logic.ServiceResult;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;

import java.util.Optional;

// Plain, so every rule is unit tested; DocumentsPanel only copies these onto controls.
public final class DocumentsText {
    static final String HEADING = "Documents";
    static final String SUBTITLE =
            "Document details only. The file itself stays with the customer and is not uploaded.";
    static final String ADD_BUTTON = "+ Add document";
    static final String EMPTY = "No documents yet.";
    static final String UNREACHABLE =
            "The mainframe could not be reached. Check your internet connection and try again.";

    // What the form shows after a save was tried. fieldErrors: the checks that failed, or the field
    // a refusal seems to be about. validationBanner: show "N fields need attention" (field errors
    // caught before sending). banner: a refusal or a network failure. closeForm: it was saved or
    // queued for upload.
    public record SaveDisplay(ValidationResult fieldErrors, boolean validationBanner,
                              Optional<BannerContent> banner, boolean closeForm) {
    }

    private DocumentsText() {
    }

    public static SaveDisplay saveDisplay(ServiceResult<?> result) {
        if (result.isSuccessful() || result.isQueued()) {
            return new SaveDisplay(new ValidationResult(), false, Optional.empty(), true);
        }
        if (result.getOutcome() == null) {
            return new SaveDisplay(result.getValidationErrors(), true, Optional.empty(), false);
        }
        if (result.getOutcome() == ServiceResult.Outcome.NETWORK_FAILURE) {
            return new SaveDisplay(new ValidationResult(), false,
                    Optional.of(new BannerContent(BannerVariant.ERROR, "Couldn't save the document", UNREACHABLE)),
                    false);
        }
        return refusal(result.getMessage());
    }

    // Figma 11: the mainframe's reason, with the field it seems to refer to highlighted
    private static SaveDisplay refusal(String message) {
        Optional<String> field = fieldFor(message);
        ValidationResult errors = new ValidationResult();
        field.ifPresent(name -> errors.add(new FieldError(name, message.trim())));
        return new SaveDisplay(errors, false,
                Optional.of(BannerContent.rejection("document", message, field.isPresent())), false);
    }

    // The mainframe's reasons are free text, so the field is guessed from what the text names: a
    // file type, a file name, a size or a (document) type. Nothing named, nothing highlighted.
    static Optional<String> fieldFor(String message) {
        if (!UiText.hasText(message)) {
            return Optional.empty();
        }
        String text = message.toLowerCase().replace("_", " ");
        if (text.contains("file type") || text.contains("filetype")) {
            return Optional.of("fileType");
        }
        if (text.contains("file name") || text.contains("filename")) {
            return Optional.of("fileName");
        }
        if (text.contains("size")) {
            return Optional.of("size");
        }
        return text.contains("type") ? Optional.of("type") : Optional.empty();
    }
}
