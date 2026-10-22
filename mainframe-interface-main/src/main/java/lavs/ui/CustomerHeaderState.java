// AI-assisted: what the customer record header shows, for a saved customer and a new one (Figma 03a).
package lavs.ui;

import lavs.logic.CustomerRecordResult;
import lavs.model.Customer;

import java.util.Optional;

// Plain (no JavaFX), so it is unit tested; CustomerRecordScreen copies it onto the controls.
// A null customer number is "+ Add New Customer": nothing is loaded and the header says the
// customer is not saved yet (US2-R3, CR-R4).
public final class CustomerHeaderState {
    private final String _number;
    private Customer _customer;
    private boolean _loadFailed;

    public CustomerHeaderState(String customerNumber) {
        _number = customerNumber;
    }

    // false for a new customer, which has no number until the mainframe assigns one
    public boolean isSaved() {
        return _number != null;
    }

    // present for a saved customer: the screen loads it once, and again on nothing else
    public Optional<String> numberToLoad() {
        return Optional.ofNullable(_number);
    }

    // present only once the customer has loaded: the sidebar counts need a customer that exists
    public Optional<String> numberToCount() {
        return Optional.ofNullable(_customer).map(customer -> _number);
    }

    // The result of loading the customer. On success the header fills in. A failure returns
    // the banner to show: the offline one for a network failure, the mainframe's reason
    // for a refusal.
    public Optional<BannerContent> applyLoad(CustomerRecordResult result) {
        _customer = result.getCustomer();
        _loadFailed = !result.isSuccessful();
        return switch (result.getOutcome()) {
            case NETWORK_FAILURE -> Optional.of(BannerContent.offline());
            case REJECTED -> Optional.of(rejected(result.getMessage()));
            case SUCCESS, INVALID -> Optional.empty();
        };
    }

    static BannerContent rejected(String mainframeMessage) {
        String reason = UiText.hasText(mainframeMessage)
                ? "Reason given: " + UiText.endWithFullStop(mainframeMessage.trim())
                : "The mainframe gave no reason.";
        return new BannerContent(BannerVariant.ERROR, "Couldn't load the customer",
                reason + " Go back and open the record again.");
    }

    // the loaded customer, or null until it has loaded
    public Customer customer() {
        return _customer;
    }

    // "Jane Smith"; "New customer"; "Loading…" until the customer arrives; "Customer C-10234"
    // if loading failed or the mainframe sent no name
    public String name() {
        if (!isSaved()) {
            return "New customer";
        }
        if (_customer == null) {
            return _loadFailed ? "Customer " + _number : "Loading…";
        }
        String fullName = _customer.getFullName();
        return UiText.hasText(fullName) ? fullName : "Customer " + _number;
    }

    // the tag beside the name: the customer's status, or "Not saved yet"; none while loading
    public Optional<CustomerSearchText.StatusTag> statusTag() {
        if (!isSaved()) {
            return Optional.of(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Not saved yet"));
        }
        return Optional.ofNullable(_customer).map(customer -> CustomerSearchText.statusTag(customer.getStatus()));
    }

    // "Customer no. C-10234 · DOB 14/03/1988" (no DOB part when it is not known)
    public String detailsLine() {
        if (!isSaved()) {
            return "Customer number is assigned by the mainframe";
        }
        String line = "Customer no. " + _number;
        String dateOfBirth = _customer == null ? "" : CustomerSearchText.dateOfBirth(_customer.getDateOfBirth());
        return dateOfBirth.isEmpty() ? line : line + " · DOB " + dateOfBirth;
    }

    // "Customer: Jane Smith (C-10234)" once loaded; "New customer"; otherwise the default title
    public String topBarTitle() {
        if (!isSaved()) {
            return "New customer";
        }
        return _customer == null ? "Customer" : "Customer: " + name() + " (" + _number + ")";
    }
}
