package lavs.logic;

import lavs.model.CustomerRelationship;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Optional component: a customer's relationships to other customers (spouse, parent,
// legal authority or other). Lists, adds, updates and deletes them.
// AI-assisted: reads go to the mainframe over the Connection; saves go through the upload queue
// (SyncService), so adding, updating and deleting work offline (US11).
// TEMPORARY: the reads still use the Connection directly rather than the MainframeGateway.
public class CustomerRelationshipService {
    public static final String CUSTOMER_NUMBER = "customerNumber";
    static final long PAGE_SIZE = 50;

    private final Connection _connection;
    private final SyncService _sync;

    public CustomerRelationshipService(Connection connection, SyncService sync) {
        _connection = connection;
        _sync = sync;
    }

    // Checks the form without contacting the mainframe: the other customer and the type
    // are required, an "Other" relationship needs a description, and a customer cannot
    // be related to themselves.
    public ValidationResult validate(String customerNumber, RelationshipInput input) {
        ValidationResult result = new ValidationResult();
        result.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        result.addIfPresent(Validators.required(RelationshipInput.OTHER_CUSTOMER_NUMBER, input.getOtherCustomerNumber()));
        result.addIfPresent(Validators.required(RelationshipInput.TYPE, input.getType()));
        if (isOther(input)) {
            result.addIfPresent(Validators.required(RelationshipInput.OTHER_TYPE, input.getOtherType()));
        }
        if (result.isValid() && customerNumber.trim().equalsIgnoreCase(input.getOtherCustomerNumber().trim())) {
            result.add(new FieldError(RelationshipInput.OTHER_CUSTOMER_NUMBER, "must be a different customer"));
        }
        return result;
    }

    // Every relationship of the customer, with the other customer's name and the
    // description of an "Other" relationship filled in. The list message has neither, so
    // each relationship is then retrieved on its own; a customer has only a few.
    // Requests pages 1, 2, ... and also stops on an empty page, so a wrong total from the
    // server cannot cause an endless loop (same approach as CategoryConstantsLoader).
    public RelationshipListResult listRelationships(String customerNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        if (!validation.isValid()) {
            return RelationshipListResult.invalid(validation);
        }

        List<CustomerRelationship> relationships = new ArrayList<>();
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            mainframe.client.messages.customerRelationship.List message =
                    new mainframe.client.messages.customerRelationship.List(customerNumber, page, PAGE_SIZE);
            Status status = message.send(_connection);
            if (!status.getWasSuccessful()) {
                return RelationshipListResult.fromStatus(status, List.of());
            }
            mainframe.client.messages.customerRelationship.List.Item[] items = message.getItems();
            for (mainframe.client.messages.customerRelationship.List.Item item : items) {
                mainframe.client.messages.customerRelationship.Retrieve details =
                        new mainframe.client.messages.customerRelationship.Retrieve(item.getId(), customerNumber);
                Status detailsStatus = details.send(_connection);
                if (!detailsStatus.getWasSuccessful()) {
                    return RelationshipListResult.fromStatus(detailsStatus, List.of());
                }
                CustomerRelationship relationship = new CustomerRelationship(customerNumber,
                        item.getSecondaryCustomerNumber(), item.getType(), details.getOtherType());
                relationship.setId(item.getId());
                relationship.setOtherCustomerName(details.getSecondaryCustomerName());
                relationships.add(relationship);
            }
            if (items.length == 0 || relationships.size() >= message.getTotal()) {
                return RelationshipListResult.fromStatus(status, relationships);
            }
            page++;
        }
    }

    // Adds the relationship with customer.AddRelationship. The mainframe gives it an id.
    public RelationshipResult addRelationship(String customerNumber, RelationshipInput input) throws IOException {
        ValidationResult validation = validate(customerNumber, input);
        if (!validation.isValid()) {
            return RelationshipResult.invalid(validation);
        }

        mainframe.client.messages.customer.AddRelationship message = new mainframe.client.messages.customer.AddRelationship(
                customerNumber, otherTypeToSend(input), input.getOtherCustomerNumber().trim(), input.getType());
        // AI-assisted: queued; the new relationship has no id until it is uploaded, so the tab lists again
        SyncStep step = QueuedWrites.queue(_sync, message, customerNumber, null);
        return RelationshipResult.queued(step, toRelationship(null, customerNumber,
                message.getSecondaryCustomerNumber(), message.getType(), message.getOtherType()));
    }

    // Saves changes to an existing relationship with customer.UpdateRelationship.
    public RelationshipResult updateRelationship(String customerNumber, long id, RelationshipInput input)
            throws IOException {
        ValidationResult validation = validate(customerNumber, input);
        if (!validation.isValid()) {
            return RelationshipResult.invalid(validation);
        }

        mainframe.client.messages.customer.UpdateRelationship message =
                new mainframe.client.messages.customer.UpdateRelationship(customerNumber, id,
                        otherTypeToSend(input), input.getOtherCustomerNumber().trim(), input.getType());
        // AI-assisted: queued, so the update works offline
        SyncStep step = QueuedWrites.queue(_sync, message, customerNumber, null);
        return RelationshipResult.queued(step, toRelationship(id, customerNumber,
                message.getSecondaryCustomerNumber(), message.getType(), message.getOtherType()));
    }

    // Removes the relationship with customer.DeleteRelationship.
    public RelationshipResult deleteRelationship(String customerNumber, long id) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        if (!validation.isValid()) {
            return RelationshipResult.invalid(validation);
        }

        mainframe.client.messages.customer.DeleteRelationship message =
                new mainframe.client.messages.customer.DeleteRelationship(customerNumber, id);
        // AI-assisted: queued, so the delete works offline
        return RelationshipResult.queued(QueuedWrites.queue(_sync, message, customerNumber, null), null);
    }

    private static boolean isOther(RelationshipInput input) {
        return RelationshipInput.TYPE_OTHER.equals(input.getType());
    }

    // the description only means something for an "Other" relationship, so it is not sent otherwise
    private static String otherTypeToSend(RelationshipInput input) {
        return isOther(input) ? input.getOtherType().trim() : null;
    }

    // Builds the model from what was queued. customer.AddRelationship and
    // customer.UpdateRelationship share no type, so both pass their getters in here.
    private static CustomerRelationship toRelationship(Long id, String customerNumber, String otherCustomerNumber,
                                                       String type, String otherType) {
        CustomerRelationship relationship = new CustomerRelationship(customerNumber, otherCustomerNumber, type, otherType);
        relationship.setId(id);
        return relationship;
    }
}
