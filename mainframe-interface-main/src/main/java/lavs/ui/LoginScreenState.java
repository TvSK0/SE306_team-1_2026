// AI-assisted: decides what the login screen shows for each outcome, with the wording from Figma 01.
package lavs.ui;

import lavs.logic.LoginAttempt;
import lavs.validation.ValidationResult;

import java.util.Optional;

// Plain, so every decision here is unit tested; LoginScreen only copies the Display onto the
// controls. The screen never clears the fields after a failure, so what was typed is kept
// (US1-R2).
public final class LoginScreenState {
    public static final String CONNECTION_NOTE =
            "An internet connection is needed to log in. Once signed in, you can keep working offline.";

    private LoginScreenState() {
    }

    // fieldErrors: INVALID only, shown inline. banner: REJECTED, NETWORK_FAILURE or an
    // unusable login. signedInUser: present only when the MMM may move on to Customer Search.
    public record Display(ValidationResult fieldErrors, Optional<BannerContent> banner, Optional<String> signedInUser) {
    }

    public static Display show(LoginAttempt attempt) {
        var auth = attempt.auth();
        return switch (auth.getOutcome()) {
            case INVALID -> new Display(auth.getValidationErrors(), Optional.empty(), Optional.empty());
            case REJECTED -> withBanner(rejected(auth.getMessage()));
            case NETWORK_FAILURE -> withBanner(networkFailure());
            case SUCCESS -> attempt.constantsAvailable()
                    ? new Display(new ValidationResult(), Optional.empty(), Optional.of(auth.getUserName()))
                    : withBanner(constantsUnavailable(attempt.constantsMessage()));
        };
    }

    // the background call threw something nobody planned for
    public static Display showUnexpected(Throwable failure) {
        String detail = UiText.hasText(failure.getMessage()) ? ": " + failure.getMessage().trim() : "";
        return withBanner(new BannerContent(BannerVariant.ERROR, "Couldn't log in",
                UiText.endWithFullStop("Something went wrong" + detail)));
    }

    static BannerContent rejected(String mainframeMessage) {
        String reason = UiText.hasText(mainframeMessage)
                ? "Reason given: " + UiText.endWithFullStop(mainframeMessage.trim())
                : "The mainframe gave no reason.";
        return new BannerContent(BannerVariant.ERROR, "Couldn't log in",
                reason + " Check your username and password and try again.");
    }

    static BannerContent networkFailure() {
        return new BannerContent(BannerVariant.ERROR, "Can't reach the mainframe",
                "Check your internet connection and try again.");
    }

    // logged in, but there are no dropdown values to work with, saved or fresh
    static BannerContent constantsUnavailable(String message) {
        String reason = UiText.hasText(message)
                ? "Reason given: " + UiText.endWithFullStop(message.trim())
                : "The mainframe gave no reason.";
        return new BannerContent(BannerVariant.ERROR, "Couldn't load the dropdown values",
                reason + " Try logging in again.");
    }

    private static Display withBanner(BannerContent content) {
        return new Display(new ValidationResult(), Optional.of(content), Optional.empty());
    }
}
