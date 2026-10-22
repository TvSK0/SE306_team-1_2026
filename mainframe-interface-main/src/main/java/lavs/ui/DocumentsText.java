// AI-assisted: the wording and decisions of the Documents section (Figma 11).
package lavs.ui;

import lavs.logic.DocumentInput;
import lavs.logic.LookupResult;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;

import java.util.Optional;

// Plain, so every rule is unit tested; CustomerDocumentsSection only copies these onto controls.
public final class DocumentsText {
    static final String HEADING = "Documents";
    static final String SUBTITLE =
            "Document details only. The file itself stays with the customer and is not uploaded.";
    static final String ADD_BUTTON = "+ Add document";
    static final String EMPTY = "No documents yet.";
    static final String SAVE = "Save document";
    static final String SAVING = "Saving…";
    private static final String UNREACHABLE =
            "The mainframe could not be reached. Check your internet connection and try again.";

    // What the form shows after a save was tried. fieldErrors: INVALID (the checks that failed) or
    // the field a refusal seems to be about. validationBanner: show the "N fields need attention"
    // banner (INVALID only). banner: a refusal or a network failure. saved: the form can close.
    public record SaveDisplay(ValidationResult fieldErrors, boolean validationBanner,
                              Optional<BannerContent> banner, boolean saved) {
    }

    private DocumentsText() {
    }

    // What an editable dropdown holds: the text typed or picked in its box, or the selected value
    // if the box shows none (the two can differ until the box has been redrawn).
    public static String chosen(String boxText, String selectedValue) {
        if (UiText.hasText(boxText)) {
            return boxText;
        }
        return selectedValue == null ? "" : selectedValue;
    }

    public static SaveDisplay saveDisplay(LookupResult<?> result) {
        return switch (result.getOutcome()) {
            case SUCCESS -> new SaveDisplay(new ValidationResult(), false, Optional.empty(), true);
            case INVALID -> new SaveDisplay(result.getValidationErrors(), true, Optional.empty(), false);
            case NETWORK_FAILURE -> new SaveDisplay(new ValidationResult(), false,
                    Optional.of(new BannerContent(BannerVariant.ERROR, "Couldn't save the document", UNREACHABLE)),
                    false);
            case REJECTED -> refusal(result.getMessage());
        };
    }

    // Figma 11: the mainframe's reason, and the field it seems to refer to highlighted
    private static SaveDisplay refusal(String message) {
        Optional<String> field = fieldFor(message);
        ValidationResult errors = new ValidationResult();
        field.ifPresent(name -> errors.add(new FieldError(name, message.trim())));
        return new SaveDisplay(errors, false,
                Optional.of(BannerContent.rejection("document", message, field.isPresent())), false);
    }

    // The mainframe's reasons are free text, so the field is guessed from what the text names:
    // a file type, a file name, a size or a (document) type. Nothing named, nothing highlighted.
    static Optional<String> fieldFor(String message) {
        if (!UiText.hasText(message)) {
            return Optional.empty();
        }
        String text = message.toLowerCase().replace("_", " ");
        if (text.contains("file type") || text.contains("filetype")) {
            return Optional.of(DocumentInput.FILE_TYPE);
        }
        if (text.contains("file name") || text.contains("filename")) {
            return Optional.of(DocumentInput.FILE_NAME);
        }
        if (text.contains("size")) {
            return Optional.of(DocumentInput.SIZE);
        }
        return text.contains("type") ? Optional.of(DocumentInput.TYPE) : Optional.empty();
    }

    // the banner for a failed read of the documents (the offline banner for a network failure)
    public static Optional<BannerContent> loadFailure(LookupResult<?> result) {
        return switch (result.getOutcome()) {
            case NETWORK_FAILURE -> Optional.of(BannerContent.offline());
            case REJECTED, INVALID -> Optional.of(failure("Couldn't load the documents", result));
            case SUCCESS -> Optional.empty();
        };
    }

    // the banner for a document that could not be deleted
    public static Optional<BannerContent> deleteFailure(LookupResult<?> result) {
        return switch (result.getOutcome()) {
            case NETWORK_FAILURE -> Optional.of(
                    new BannerContent(BannerVariant.ERROR, "Couldn't delete the document", UNREACHABLE));
            case REJECTED, INVALID -> Optional.of(failure("Couldn't delete the document", result));
            case SUCCESS -> Optional.empty();
        };
    }

    private static BannerContent failure(String title, LookupResult<?> result) {
        String reason = UiText.hasText(result.getMessage())
                ? "Reason given: " + UiText.endWithFullStop(result.getMessage().trim())
                : "The mainframe gave no reason.";
        return new BannerContent(BannerVariant.ERROR, title, reason + " Try again.");
    }

    // the background call threw something nobody planned for
    static BannerContent unexpected(String title, Throwable failure) {
        String detail = UiText.hasText(failure.getMessage()) ? ": " + failure.getMessage().trim() : "";
        return new BannerContent(BannerVariant.ERROR, title, UiText.endWithFullStop("Something went wrong" + detail));
    }
}
