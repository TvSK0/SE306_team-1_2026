// AI-assisted: the wording and small formatting rules of the customer search screen (Figma 02, 09, 12).
package lavs.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

// Plain, so every rule is unit tested; CustomerSearchScreen only copies these onto controls.
public final class CustomerSearchText {
    static final String LOADING_MORE = "Loading…";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // the tag shown in the Status column
    public record StatusTag(TagVariant variant, String text) {
    }

    private CustomerSearchText() {
    }

    // always the total across every page, never just the rows on screen
    public static String countLine(long total) {
        return total == 1 ? "1 customer found" : total + " customers found";
    }

    // query is the trimmed query
    public static String emptyText(String query) {
        return "No customers match \"" + query + "\". Check the spelling, or add a new customer.";
    }

    // "Active" and "Inactive" (any case) are the mainframe's two statuses (UpdateStatus); anything
    // else is shown exactly as the mainframe sent it, in the neutral colour
    public static StatusTag statusTag(String status) {
        if ("Active".equalsIgnoreCase(status)) {
            return new StatusTag(TagVariant.SUCCESS, "Active");
        }
        if ("Inactive".equalsIgnoreCase(status)) {
            return new StatusTag(TagVariant.NEUTRAL, "Inactive");
        }
        return new StatusTag(TagVariant.NEUTRAL, status == null ? "" : status);
    }

    // dd/MM/yyyy, or blank when the date of birth is not known
    public static String dateOfBirth(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    // the button under the table when there are more matches than rows
    public static String showMoreLabel(int shown, long total) {
        return "Show more (" + shown + " of " + total + " shown)";
    }

    // Search cannot be pressed while offline (searching needs a connection) or while a search runs
    public static boolean searchDisabled(boolean online, boolean searching) {
        return !online || searching;
    }

    // Figma frame 09, shown for as long as the app is offline
    public static Optional<BannerContent> offlineBanner(boolean online) {
        return online ? Optional.empty() : Optional.of(BannerContent.offline());
    }

    static BannerContent rejected(String mainframeMessage) {
        String reason = UiText.hasText(mainframeMessage)
                ? "Reason given: " + UiText.endWithFullStop(mainframeMessage.trim())
                : "The mainframe gave no reason.";
        return new BannerContent(BannerVariant.ERROR, "Couldn't search", reason + " Try again.");
    }

    // the background call threw something nobody planned for
    static BannerContent unexpected(Throwable failure) {
        String detail = UiText.hasText(failure.getMessage()) ? ": " + failure.getMessage().trim() : "";
        return new BannerContent(BannerVariant.ERROR, "Couldn't search",
                UiText.endWithFullStop("Something went wrong" + detail));
    }
}
