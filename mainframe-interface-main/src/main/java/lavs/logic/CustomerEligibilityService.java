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

    public ServiceResult<Void> check(String customerNumber) {
        ValidationResult errors = new ValidationResult();
        errors.addIfPresent(Validators.required("customerNumber", customerNumber));
        if (!errors.isValid()) return ServiceResult.invalid(errors);

        boolean mailing = false;
        long seen = 0;
        for (long page = 1; !mailing; page++) {
            var addresses = new mainframe.client.messages.customerAddress.List(customerNumber, page, 50);
            GatewayResult result = gateway.send(addresses);
            if (!result.isSuccess()) return ServiceResult.from(result, null);
            for (var address : addresses.getItems()) {
                mailing |= "y".equalsIgnoreCase(address.getIsMailing());
            }
            seen += addresses.getItems().length;
            if (addresses.getItems().length == 0 || seen >= addresses.getTotal()) break;
        }
        var phones = new mainframe.client.messages.customerPhoneNumber.List(customerNumber, 1, 1);
        GatewayResult result = gateway.send(phones);
        if (!result.isSuccess()) return ServiceResult.from(result, null);
        boolean phone = phones.getItems().length > 0;
        boolean email = false;
        if (!phone) {
            var emails = new mainframe.client.messages.customerEmail.List(customerNumber, 1, 1);
            result = gateway.send(emails);
            if (!result.isSuccess()) return ServiceResult.from(result, null);
            email = emails.getItems().length > 0;
        }
        if (!EligibilityChecker.isEligible(mailing, phone, email)) {
            if (!mailing) errors.add(new FieldError("mailingAddress", "the customer needs a mailing address"));
            if (!phone && !email) errors.add(new FieldError("contactDetails", "the customer needs a phone number or email"));
            return ServiceResult.invalid(errors);
        }
        return ServiceResult.from(result, null);
    }
}
