package lavs.logic;

import lavs.data.GatewayResult;
import lavs.data.MainframeGateway;
import lavs.model.PhoneNumber;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.messages.customer.AddPhoneNumber;
import mainframe.client.messages.customer.DeletePhoneNumber;
import mainframe.client.messages.customer.UpdatePhoneNumber;
import mainframe.client.messages.customerPhoneNumber.Retrieve;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// AI-assisted: US4, a customer's phone numbers: list, add, update and delete, using
// customerPhoneNumber.List/Retrieve and customer.AddPhoneNumber/UpdatePhoneNumber/
// DeletePhoneNumber through the MainframeGateway.
//
// A phone number needs a type (and a description for "Other") and a number the shared
// phone validator accepts. The country code is optional, e.g. "+64".
//
// TEMPORARY: saves go straight to the mainframe until SyncService is composed in LavsApp.
public class PhoneNumberService {
    public static final String CUSTOMER_NUMBER = "customerNumber";
    static final long PAGE_SIZE = 50;
    // "+64" or "64": one to three digits
    private static final Pattern COUNTRY_CODE = Pattern.compile("^\\+?\\d{1,3}$");

    private final MainframeGateway _gateway;

    public PhoneNumberService(MainframeGateway gateway) {
        _gateway = gateway;
    }

    // Checks the form without contacting the mainframe.
    public ValidationResult validate(PhoneNumberInput input) {
        ValidationResult result = new ValidationResult();
        ContactRules.checkType(result, input.getType(), input.getOtherType());
        String countryCode = ContactRules.trimmed(input.getCountryCode());
        if (countryCode != null && !COUNTRY_CODE.matcher(countryCode).matches()) {
            result.add(new FieldError(PhoneNumberInput.COUNTRY_CODE, "must be like +64"));
        }
        result.addIfPresent(Validators.required(PhoneNumberInput.NUMBER, input.getNumber())
                .or(() -> Validators.phone(PhoneNumberInput.NUMBER, input.getNumber().trim())));
        return result;
    }

    // Every phone number of the customer. The list message has everything except the
    // description of an "Other" type, so only those are read again on their own. Reads pages
    // 1, 2, ... and also stops on an empty page, so a wrong total can't loop.
    public ServiceResult<List<PhoneNumber>> listPhoneNumbers(String customerNumber) {
        ValidationResult validation = requireCustomer(customerNumber);
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        List<PhoneNumber> phones = new ArrayList<>();
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            mainframe.client.messages.customerPhoneNumber.List message =
                    new mainframe.client.messages.customerPhoneNumber.List(customerNumber, page, PAGE_SIZE);
            GatewayResult listed = _gateway.send(message);
            if (!listed.isSuccess()) {
                return ServiceResult.from(listed, null);
            }
            mainframe.client.messages.customerPhoneNumber.List.Item[] items = message.getItems();
            for (mainframe.client.messages.customerPhoneNumber.List.Item item : items) {
                String otherType = null;
                if (ContactRules.TYPE_OTHER.equals(item.getType())) {
                    Retrieve details = new Retrieve(item.getId(), customerNumber);
                    GatewayResult read = _gateway.send(details);
                    if (!read.isSuccess()) {
                        return ServiceResult.from(read, null);
                    }
                    otherType = details.getOtherType();
                }
                phones.add(toPhoneNumber(item.getId(), customerNumber, item.getType(), otherType,
                        item.getCountryCode(), item.getNumber(), "y".equals(item.getIsPrimary()),
                        "y".equals(item.getCanTxt())));
            }
            if (items.length == 0 || phones.size() >= message.getTotal()) {
                return ServiceResult.from(listed, phones);
            }
            page++;
        }
    }

    public ServiceResult<PhoneNumber> addPhoneNumber(String customerNumber, PhoneNumberInput input) {
        ValidationResult validation = validate(input);
        validation.merge(requireCustomer(customerNumber));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        AddPhoneNumber message = new AddPhoneNumber(input.canTxt(), ContactRules.trimmed(input.getCountryCode()),
                customerNumber, input.isPrimary(), input.getNumber().trim(),
                ContactRules.otherTypeToSend(input.getType(), input.getOtherType()), input.getType());
        GatewayResult result = _gateway.send(message);
        return ServiceResult.from(result, toPhoneNumber(message.getId(), customerNumber, message.getType(),
                message.getOtherType(), message.getCountryCode(), message.getNumber(),
                Boolean.TRUE.equals(message.getIsPrimary()), Boolean.TRUE.equals(message.getCanTxt())));
    }

    public ServiceResult<PhoneNumber> updatePhoneNumber(String customerNumber, long id, PhoneNumberInput input) {
        ValidationResult validation = validate(input);
        validation.merge(requireCustomer(customerNumber));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        UpdatePhoneNumber message = new UpdatePhoneNumber(input.canTxt(), ContactRules.trimmed(input.getCountryCode()),
                customerNumber, id, input.isPrimary(), input.getNumber().trim(),
                ContactRules.otherTypeToSend(input.getType(), input.getOtherType()), input.getType());
        GatewayResult result = _gateway.send(message);
        return ServiceResult.from(result, toPhoneNumber(message.getId(), customerNumber, message.getType(),
                message.getOtherType(), message.getCountryCode(), message.getNumber(),
                Boolean.TRUE.equals(message.getIsPrimary()), Boolean.TRUE.equals(message.getCanTxt())));
    }

    public ServiceResult<Void> deletePhoneNumber(String customerNumber, long id) {
        ValidationResult validation = requireCustomer(customerNumber);
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        return ServiceResult.from(_gateway.send(new DeletePhoneNumber(customerNumber, id)), null);
    }

    private static ValidationResult requireCustomer(String customerNumber) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        return validation;
    }

    private static PhoneNumber toPhoneNumber(Long id, String customerNumber, String type, String otherType,
                                             String countryCode, String number, boolean primary, boolean canTxt) {
        PhoneNumber phone = new PhoneNumber(customerNumber, type, countryCode, number, primary, canTxt);
        phone.setId(id);
        phone.setOtherType(otherType);
        return phone;
    }
}
