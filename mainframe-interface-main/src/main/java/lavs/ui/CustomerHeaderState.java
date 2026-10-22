// AI-assisted: what the customer record header and top bar show, for a saved customer and a new one (Figma 03a).
package lavs.ui;

import lavs.logic.ServiceResult;
import lavs.model.Customer;

import java.util.Optional;

// Plain (no JavaFX), so it is unit tested; CustomerRecordController copies it onto the header.
// A null customer number is "+ Add New Customer": nothing is loaded and the header says the
// customer is not saved yet (CR-R4) until the Details tab saves them.
public final class CustomerHeaderState {
    private String _number;
    private Customer _customer;
    private boolean _loadFailed;

    public CustomerHeaderState(String customerNumber) {
        _number = customerNumber;
    }

    // false for a new customer, which has no number until it is saved
    public boolean isSaved() {
        return _number != null;
    }

    // The customer loaded, or just saved from the Details tab (which is how a new one gets a number).
    public void setCustomer(Customer customer) {
        _customer = customer;
        _number = customer.getNumber();
        _loadFailed = false;
    }

    // The result of loading the customer. On success the header fills in; a failure returns the
    // banner to show: the offline one for a network failure, otherwise the reason.
    public Optional<BannerContent> applyLoad(ServiceResult<Customer> result) {
        if (result.isSuccessful()) {
            setCustomer(result.getValue());
            return Optional.empty();
        }
        _loadFailed = true;
        if (result.getOutcome() == ServiceResult.Outcome.NETWORK_FAILURE) {
            return Optional.of(BannerContent.offline());
        }
        return Optional.of(couldntLoad(result.getMessage()));
    }

    static BannerContent couldntLoad(String message) {
        String reason = UiText.hasText(message)
                ? "Reason given: " + UiText.endWithFullStop(message.trim())
                : "The mainframe gave no reason.";
        return new BannerContent(BannerVariant.ERROR, "Couldn't load the customer",
                reason + " Go back and open the record again.");
    }

    // "Jane Smith"; "New customer"; "Loading…" until the customer arrives; "Customer C-10234" if
    // loading failed or the mainframe sent no name
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
