// AI-assisted: the Figma 02 wording and formatting rules of the customer search screen.
package lavs.ui;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

// Plain, so every rule is unit tested; CustomerSearchScreen only copies these onto controls.
public final class CustomerSearchText {
    static final String TITLE = "Find a customer";
    static final String SUBTITLE = "Search before adding a new customer to avoid creating a duplicate.";
    static final String SELECT_TITLE = "Select a customer";
    static final String FIELD_LABEL = "Search by name or customer number";
    static final String ADD_NEW = "+ Add New Customer";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // the tag shown in a table cell
    public record StatusTag(TagVariant variant, String text) {
    }

    private CustomerSearchText() {
    }

    // always the total across every page: "1 customer found", "25 customers found", plus the
    // page when there is more than one, e.g. "60 customers found · Page 2 of 3"
    public static String countLine(long total, long page, int pageSize) {
        String count = total == 1 ? "1 customer found" : total + " customers found";
        long pages = (total + pageSize - 1) / pageSize;
        return pages > 1 ? count + " · Page " + page + " of " + pages : count;
    }

    // US2-R6; query is the trimmed query
    public static String emptyText(String query) {
        return "No customers match \"" + query + "\". Check the spelling, or add a new customer.";
    }

    // "Active" and "Inactive" (any case) are the mainframe's two statuses; anything else is
    // shown exactly as the mainframe sent it, in the neutral colour
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

    // Search cannot be pressed while offline (searching needs a connection) or while a search runs
    public static boolean searchDisabled(boolean online, boolean searching) {
        return !online || searching;
    }

    // Figma frame 09, shown for as long as the app is offline
    public static Optional<BannerContent> offlineBanner(boolean online) {
        return online ? Optional.empty() : Optional.of(BannerContent.offline());
    }
}
