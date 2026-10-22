package lavs.logic;

import lavs.data.GatewayResult;
import lavs.data.MainframeGateway;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;

// AI-assisted: checks current mainframe records before a new loan is created.
public final class CustomerEligibilityService {
    private final MainframeGateway gateway;

    public CustomerEligibilityService(MainframeGateway gateway) {
        this.gateway = gateway;
    }

    // AI-assisted: split into hasMailingAddress, hasPhone, hasEmail and missingDetails to keep
    // each method's complexity under 10 (metrics baseline); the behaviour is unchanged.
    public ServiceResult<Void> check(String customerNumber) {
        ValidationResult errors = new ValidationResult();
        errors.addIfPresent(Validators.required("customerNumber", customerNumber));
        if (!errors.isValid()) return ServiceResult.invalid(errors);

        Lookup mailing = hasMailingAddress(customerNumber);
        if (mailing.failed()) return ServiceResult.from(mailing.result(), null);
        Lookup phone = hasPhone(customerNumber);
        if (phone.failed()) return ServiceResult.from(phone.result(), null);
        // an email is only looked up when there's no phone
        Lookup contact = phone.found() ? phone : hasEmail(customerNumber);
        if (contact.failed()) return ServiceResult.from(contact.result(), null);
        boolean email = !phone.found() && contact.found();
        if (EligibilityChecker.isEligible(mailing.found(), phone.found(), email)) {
            return ServiceResult.from(contact.result(), null);
        }
        return ServiceResult.invalid(missingDetails(errors, mailing.found(), contact.found()));
    }

    // one mainframe read: what it answered, and whether it found what was asked for
    private record Lookup(GatewayResult result, boolean found) {
        boolean failed() { return !result.isSuccess(); }
    }

    // reads the addresses a page at a time, stopping at the first mailing one
    private Lookup hasMailingAddress(String customerNumber) {
        long seen = 0;
        for (long page = 1; ; page++) {
            var addresses = new mainframe.client.messages.customerAddress.List(customerNumber, page, 50);
            GatewayResult result = gateway.send(addresses);
            if (!result.isSuccess()) return new Lookup(result, false);
            for (var address : addresses.getItems()) {
                if ("y".equalsIgnoreCase(address.getIsMailing())) return new Lookup(result, true);
            }
            seen += addresses.getItems().length;
            if (addresses.getItems().length == 0 || seen >= addresses.getTotal()) return new Lookup(result, false);
        }
    }

    private Lookup hasPhone(String customerNumber) {
        var phones = new mainframe.client.messages.customerPhoneNumber.List(customerNumber, 1, 1);
        GatewayResult result = gateway.send(phones);
        return new Lookup(result, result.isSuccess() && phones.getItems().length > 0);
    }

    private Lookup hasEmail(String customerNumber) {
        var emails = new mainframe.client.messages.customerEmail.List(customerNumber, 1, 1);
        GatewayResult result = gateway.send(emails);
        return new Lookup(result, result.isSuccess() && emails.getItems().length > 0);
    }

    private static ValidationResult missingDetails(ValidationResult errors, boolean mailing, boolean contact) {
        if (!mailing) errors.add(new FieldError("mailingAddress", "the customer needs a mailing address"));
        if (!contact) errors.add(new FieldError("contactDetails", "the customer needs a phone number or email"));
        return errors;
    }
}
