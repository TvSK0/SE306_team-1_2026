package lavs.model;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// One save that's waiting to be uploaded to the mainframe. Every write goes into the queue first
// and the sync thread will send it immediately once back online.

// A change that creates a customer or loan while offline carries the temporary number it created (createsNumber),
// where later changes will refer to that number until the mainframe assigns the real one (once back online).

public class PendingChange {

    // PENDING = waiting to be sent, PAUSED = waiting for the MMM, REJECTED = refused by the mainframe
    public enum Status { PENDING, PAUSED, REJECTED }

    private Long id;
    private Instant createdAt;
    private final String messageType;
    private String customerNumber;
    private final String createsNumber;
    private final Map<String, String> fields;
    private Status status;
    private String statusMessage;
    private boolean skipDuplicateCheck;
    // AI-assisted: for a new customer paused as a possible duplicate, the customers on the
    // mainframe it may be (US11); empty otherwise
    private List<String> duplicateMatches = List.of();

    public PendingChange(String messageType, String customerNumber, String createsNumber,
                         Map<String, String> fields) {
        this.messageType = Objects.requireNonNull(messageType, "messageType");
        this.customerNumber = customerNumber;
        this.createsNumber = createsNumber;
        this.fields = new LinkedHashMap<>(fields);
        this.createdAt = Instant.now();
        this.status = Status.PENDING;
    }

    // Returns true if the change mentions the number anywhere (its customer, what it creates, or a field)
    public boolean references(String number) {
        if (number == null) {
            return false;
        }
        return number.equals(customerNumber) || number.equals(createsNumber) || fields.containsValue(number);
    }

    //Swaps every use of oldNumber with the newNumber (for customer number and field values)
    public void replaceNumber(String oldNumber, String newNumber) {
        if (oldNumber.equals(customerNumber)) {
            customerNumber = newNumber;
        }
        fields.replaceAll((name, value) -> oldNumber.equals(value) ? newNumber : value);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // Mainframe message name eg "customer.addAddress"
    public String getMessageType() {
        return messageType;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    // Temporary number this change creates or null if nothing's created
    public String getCreatesNumber() {
        return createsNumber;
    }

    // Read-only message fields in the same order they were added.
    public Map<String, String> getFields() {
        return Collections.unmodifiableMap(fields);
    }

    /** Stores upload progress in the durable queue before the next request. */
    public void setField(String name, String value) { fields.put(name, value); }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    // Reason on why the change was paused or rejected, is null while change is pending
    public String getStatusMessage() {
        return statusMessage;
    }

    public void setStatusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    // Returns true if the MMM has confirmed a possible duplicate is really a new customer.
    public boolean isSkipDuplicateCheck() {
        return skipDuplicateCheck;
    }

    public void setSkipDuplicateCheck(boolean skipDuplicateCheck) {
        this.skipDuplicateCheck = skipDuplicateCheck;
    }

    // AI-assisted: the existing customers a paused new customer may be, e.g. ["C-10234"]; never null
    public List<String> getDuplicateMatches() {
        return duplicateMatches;
    }

    public void setDuplicateMatches(List<String> duplicateMatches) {
        this.duplicateMatches = duplicateMatches == null ? List.of() : List.copyOf(duplicateMatches);
    }
}
