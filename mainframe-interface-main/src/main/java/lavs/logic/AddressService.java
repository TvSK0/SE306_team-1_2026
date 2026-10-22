package lavs.logic;

import lavs.data.GatewayResult;
import lavs.data.MainframeGateway;
import lavs.model.Address;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.messages.customer.AddAddress;
import mainframe.client.messages.customer.DeleteAddress;
import mainframe.client.messages.customer.UpdateAddress;
import mainframe.client.messages.customerAddress.Retrieve;

import java.util.ArrayList;
import java.util.List;

// AI-assisted: US4, a customer's addresses: list, add, update and delete, using
// customerAddress.List/Retrieve and customer.AddAddress/UpdateAddress/DeleteAddress through
// the MainframeGateway. A mailing address is what makes a customer eligible for a loan.
//
// An address needs a type (and a description for "Other"), a first line, a city and a
// country. A New Zealand address also needs a 4-digit postcode.
//
// TEMPORARY: saves go straight to the mainframe until SyncService is composed in LavsApp.
public class AddressService {
    public static final String CUSTOMER_NUMBER = "customerNumber";
    static final long PAGE_SIZE = 50;

    private final MainframeGateway _gateway;

    public AddressService(MainframeGateway gateway) {
        _gateway = gateway;
    }

    // Checks the form without contacting the mainframe.
    public ValidationResult validate(AddressInput input) {
        ValidationResult result = new ValidationResult();
        ContactRules.checkType(result, input.getType(), input.getOtherType());
        result.addIfPresent(Validators.required(AddressInput.LINE1, input.getLine1()));
        result.addIfPresent(Validators.required(AddressInput.CITY, input.getCity()));
        result.addIfPresent(Validators.required(AddressInput.COUNTRY, input.getCountry()));
        if (isNewZealand(input.getCountry())) {
            result.addIfPresent(Validators.required(AddressInput.POST_CODE, input.getPostCode())
                    .or(() -> Validators.postcode(AddressInput.POST_CODE, input.getPostCode().trim())));
        }
        return result;
    }

    // Every address of the customer. The list message leaves out the street lines, suburb
    // and postcode, so each address is then read on its own; a customer has only a few.
    // Reads pages 1, 2, ... and also stops on an empty page, so a wrong total can't loop.
    public ServiceResult<List<Address>> listAddresses(String customerNumber) {
        ValidationResult validation = requireCustomer(customerNumber);
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        List<Address> addresses = new ArrayList<>();
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            mainframe.client.messages.customerAddress.List message =
                    new mainframe.client.messages.customerAddress.List(customerNumber, page, PAGE_SIZE);
            GatewayResult listed = _gateway.send(message);
            if (!listed.isSuccess()) {
                return ServiceResult.from(listed, null);
            }
            mainframe.client.messages.customerAddress.List.Item[] items = message.getItems();
            for (mainframe.client.messages.customerAddress.List.Item item : items) {
                Retrieve details = new Retrieve(item.getId(), customerNumber);
                GatewayResult read = _gateway.send(details);
                if (!read.isSuccess()) {
                    return ServiceResult.from(read, null);
                }
                addresses.add(toAddress(item.getId(), customerNumber, details.getType(), details.getOtherType(),
                        details.getLine1(), details.getLine2(), details.getSuburb(), details.getCity(),
                        details.getPostCode(), details.getCountry(), details.getIsPrimary(), details.getIsMailing()));
            }
            if (items.length == 0 || addresses.size() >= message.getTotal()) {
                return ServiceResult.from(listed, addresses);
            }
            page++;
        }
    }

    public ServiceResult<Address> addAddress(String customerNumber, AddressInput input) {
        ValidationResult validation = validate(input);
        validation.merge(requireCustomer(customerNumber));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        AddAddress message = new AddAddress(input.getCity().trim(), input.getCountry().trim(), customerNumber,
                input.isMailing(), input.isPrimary(), input.getLine1().trim(), ContactRules.trimmed(input.getLine2()),
                ContactRules.otherTypeToSend(input.getType(), input.getOtherType()),
                ContactRules.trimmed(input.getPostCode()), ContactRules.trimmed(input.getSuburb()), input.getType());
        GatewayResult result = _gateway.send(message);
        return ServiceResult.from(result, toAddress(message.getId(), customerNumber, message.getType(),
                message.getOtherType(), message.getLine1(), message.getLine2(), message.getSuburb(), message.getCity(),
                message.getPostCode(), message.getCountry(), message.getIsPrimary(), message.getIsMailing()));
    }

    public ServiceResult<Address> updateAddress(String customerNumber, long id, AddressInput input) {
        ValidationResult validation = validate(input);
        validation.merge(requireCustomer(customerNumber));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        UpdateAddress message = new UpdateAddress(input.getCity().trim(), input.getCountry().trim(), customerNumber,
                id, input.isMailing(), input.isPrimary(), input.getLine1().trim(),
                ContactRules.trimmed(input.getLine2()), ContactRules.otherTypeToSend(input.getType(), input.getOtherType()),
                ContactRules.trimmed(input.getPostCode()), ContactRules.trimmed(input.getSuburb()), input.getType());
        GatewayResult result = _gateway.send(message);
        return ServiceResult.from(result, toAddress(message.getId(), customerNumber, message.getType(),
                message.getOtherType(), message.getLine1(), message.getLine2(), message.getSuburb(), message.getCity(),
                message.getPostCode(), message.getCountry(), message.getIsPrimary(), message.getIsMailing()));
    }

    public ServiceResult<Void> deleteAddress(String customerNumber, long id) {
        ValidationResult validation = requireCustomer(customerNumber);
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        return ServiceResult.from(_gateway.send(new DeleteAddress(customerNumber, id)), null);
    }

    private static ValidationResult requireCustomer(String customerNumber) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        return validation;
    }

    private static boolean isNewZealand(String country) {
        return country != null && AddressInput.NEW_ZEALAND.equalsIgnoreCase(country.trim());
    }

    // Builds the model from what the mainframe sent back; the messages share no type, so each
    // passes its getters in here. Missing yes/no flags count as no.
    private static Address toAddress(Long id, String customerNumber, String type, String otherType, String line1,
                                     String line2, String suburb, String city, String postCode, String country,
                                     Boolean primary, Boolean mailing) {
        Address address = new Address(customerNumber, type, line1, line2, suburb, city, postCode, country,
                Boolean.TRUE.equals(primary), Boolean.TRUE.equals(mailing));
        address.setId(id);
        address.setOtherType(otherType);
        return address;
    }
}
