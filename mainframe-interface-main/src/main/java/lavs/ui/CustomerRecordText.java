// AI-assisted: wording for the customer record frame that is not part of the header or sidebar.
package lavs.ui;

// Plain, so it is unit tested.
public final class CustomerRecordText {
    private CustomerRecordText() {
    }

    // the background call threw something nobody planned for
    static BannerContent unexpected(Throwable failure) {
        String detail = UiText.hasText(failure.getMessage()) ? ": " + failure.getMessage().trim() : "";
        return new BannerContent(BannerVariant.ERROR, "Couldn't load the customer",
                UiText.endWithFullStop("Something went wrong" + detail));
    }
}
