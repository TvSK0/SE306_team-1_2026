// AI-assisted: the wording and small rules of the Loans section (Figma 03a, 08).
package lavs.ui;

import lavs.logic.LookupResult;
import lavs.model.Customer;

import java.util.Optional;

// Plain, so it is unit tested; CustomerLoansSection only copies these onto controls.
public final class LoansSectionText {
    static final String EMPTY = "No loans yet.";

    private LoansSectionText() {
    }

    // what to call the customer: their name, or their number if the name is not known yet
    public static String customerLabel(Customer customer, String customerNumber) {
        String name = customer == null ? "" : customer.getFullName();
        return UiText.hasText(name) ? name : customerNumber;
    }

    public static String subtitle(String customerLabel) {
        return "Loans where " + customerLabel + " is the main borrower, co-borrower or guarantor.";
    }

    // The loan screen's Back link shows the customer's name, so it is given the whole customer
    // when it has loaded and otherwise one that only has the number.
    public static Customer customerFor(Customer loaded, String customerNumber) {
        if (loaded != null) {
            return loaded;
        }
        Customer customer = new Customer();
        customer.setNumber(customerNumber);
        return customer;
    }

    // the banner for a failed read of the loans (the offline banner for a network failure)
    public static Optional<BannerContent> loadFailure(LookupResult<?> result) {
        return failure(result, "Couldn't load the loans", "Try again.");
    }

    // the banner for a loan that could not be opened
    public static Optional<BannerContent> openFailure(LookupResult<?> result) {
        return failure(result, "Couldn't open the loan", "Try again.");
    }

    private static Optional<BannerContent> failure(LookupResult<?> result, String title, String next) {
        return switch (result.getOutcome()) {
            case NETWORK_FAILURE -> Optional.of(BannerContent.offline());
            case REJECTED, INVALID -> Optional.of(new BannerContent(BannerVariant.ERROR, title,
                    reason(result.getMessage()) + " " + next));
            case SUCCESS -> Optional.empty();
        };
    }

    private static String reason(String mainframeMessage) {
        return UiText.hasText(mainframeMessage)
                ? "Reason given: " + UiText.endWithFullStop(mainframeMessage.trim())
                : "The mainframe gave no reason.";
    }

    // the background call threw something nobody planned for
    static BannerContent unexpected(String title, Throwable failure) {
        String detail = UiText.hasText(failure.getMessage()) ? ": " + failure.getMessage().trim() : "";
        return new BannerContent(BannerVariant.ERROR, title, UiText.endWithFullStop("Something went wrong" + detail));
    }
}
