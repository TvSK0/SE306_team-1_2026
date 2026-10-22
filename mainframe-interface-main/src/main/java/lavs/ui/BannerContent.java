// AI-assisted: the title and body of a banner, with the exact wording from the Figma frames.
package lavs.ui;

// Plain, so the wording is unit tested. Screens build one of these and give it to a Banner.
public record BannerContent(BannerVariant variant, String title, String body) {
    // Figma frame 09: shown on the customer search while offline
    public static BannerContent offline() {
        return new BannerContent(BannerVariant.WARNING, "You're offline",
                "Your changes are saved on this laptop and will upload automatically, in order, when the "
                        + "connection returns. Customer search is unavailable until then.");
    }

    // Figma frame 11: the mainframe refused a save. thing is what was being saved, e.g. "document".
    // fieldHighlighted is true when the screen could mark the field the reason refers to; when
    // it could not, the last sentence tells the user to check the details instead.
    public static BannerContent rejection(String thing, String message, boolean fieldHighlighted) {
        String reason = UiText.hasText(message)
                ? "Reason given: " + UiText.endWithFullStop(message.trim())
                : "The mainframe gave no reason.";
        String advice = fieldHighlighted
                ? "Fix the highlighted field and save again."
                : "Check the details and save again.";
        return new BannerContent(BannerVariant.ERROR, "The mainframe didn't accept this " + thing,
                reason + " Nothing has been lost. " + advice);
    }

    // Figma frame 07: "3 fields need attention"
    public static BannerContent validation(FieldErrorPresenter.Result result) {
        return new BannerContent(BannerVariant.ERROR, result.getBannerTitle(), result.getBannerBody());
    }
}
