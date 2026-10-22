package lavs.logic;

import lavs.data.GatewayResult;
import lavs.data.MainframeGateway;
import lavs.model.Email;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.messages.customer.AddEmail;
import mainframe.client.messages.customer.DeleteEmail;
import mainframe.client.messages.customer.UpdateEmail;
import mainframe.client.messages.customerEmail.Retrieve;

import java.util.ArrayList;
import java.util.List;

// AI-assisted: US4, a customer's email addresses: list, add, update and delete, using
// customerEmail.List/Retrieve and customer.AddEmail/UpdateEmail/DeleteEmail through the
// MainframeGateway.
//
// An email needs a type (and a description for "Other") and an address the shared email
// validator accepts.
//
// Saves go through the gateway, which hands every write to the upload queue (QueuedWrites)
// once LavsApp has set it up, so a save made offline waits on this laptop until it uploads.
public class EmailService {
    public static final String CUSTOMER_NUMBER = "customerNumber";
    static final long PAGE_SIZE = 50;

    private final MainframeGateway _gateway;

    public EmailService(MainframeGateway gateway) {
        _gateway = gateway;
    }

    // Checks the form without contacting the mainframe.
    public ValidationResult validate(EmailInput input) {
        ValidationResult result = new ValidationResult();
        ContactRules.checkType(result, input.getType(), input.getOtherType());
        result.addIfPresent(Validators.required(EmailInput.ADDRESS, input.getAddress())
                .or(() -> Validators.email(EmailInput.ADDRESS, input.getAddress().trim())));
        return result;
    }

    // Every email of the customer. The list message has everything except the description of
    // an "Other" type, so only those are read again on their own. Reads pages 1, 2, ... and
    // also stops on an empty page, so a wrong total can't loop.
    public ServiceResult<List<Email>> listEmails(String customerNumber) {
        ValidationResult validation = requireCustomer(customerNumber);
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        List<Email> emails = new ArrayList<>();
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            mainframe.client.messages.customerEmail.List message =
                    new mainframe.client.messages.customerEmail.List(customerNumber, page, PAGE_SIZE);
            GatewayResult listed = _gateway.send(message);
            if (!listed.isSuccess()) {
                return ServiceResult.from(listed, null);
            }
            mainframe.client.messages.customerEmail.List.Item[] items = message.getItems();
            for (mainframe.client.messages.customerEmail.List.Item item : items) {
                String otherType = null;
                if (ContactRules.TYPE_OTHER.equals(item.getType())) {
                    Retrieve details = new Retrieve(item.getId(), customerNumber);
                    GatewayResult read = _gateway.send(details);
                    if (!read.isSuccess()) {
                        return ServiceResult.from(read, null);
                    }
                    otherType = details.getOtherType();
                }
                emails.add(toEmail(item.getId(), customerNumber, item.getType(), otherType, item.getAddress(),
                        "y".equals(item.getIsPrimary())));
            }
            if (items.length == 0 || emails.size() >= message.getTotal()) {
                return ServiceResult.from(listed, emails);
            }
            page++;
        }
    }

    public ServiceResult<Email> addEmail(String customerNumber, EmailInput input) {
        ValidationResult validation = validate(input);
        validation.merge(requireCustomer(customerNumber));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        AddEmail message = new AddEmail(input.getAddress().trim(), customerNumber, input.isPrimary(),
                ContactRules.otherTypeToSend(input.getType(), input.getOtherType()), input.getType());
        GatewayResult result = _gateway.send(message);
        return ServiceResult.from(result, toEmail(message.getId(), customerNumber, message.getType(),
                message.getOtherType(), message.getAddress(), Boolean.TRUE.equals(message.getIsPrimary())));
    }

    public ServiceResult<Email> updateEmail(String customerNumber, long id, EmailInput input) {
        ValidationResult validation = validate(input);
        validation.merge(requireCustomer(customerNumber));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        UpdateEmail message = new UpdateEmail(input.getAddress().trim(), customerNumber, id, input.isPrimary(),
                ContactRules.otherTypeToSend(input.getType(), input.getOtherType()), input.getType());
        GatewayResult result = _gateway.send(message);
        return ServiceResult.from(result, toEmail(message.getId(), customerNumber, message.getType(),
                message.getOtherType(), message.getAddress(), Boolean.TRUE.equals(message.getIsPrimary())));
    }

    public ServiceResult<Void> deleteEmail(String customerNumber, long id) {
        ValidationResult validation = requireCustomer(customerNumber);
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        return ServiceResult.from(_gateway.send(new DeleteEmail(customerNumber, id)), null);
    }

    private static ValidationResult requireCustomer(String customerNumber) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        return validation;
    }

    private static Email toEmail(Long id, String customerNumber, String type, String otherType, String address,
                                 boolean primary) {
        Email email = new Email(customerNumber, type, address, primary);
        email.setId(id);
        email.setOtherType(otherType);
        return email;
    }
}
