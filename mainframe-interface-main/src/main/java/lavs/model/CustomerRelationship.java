package lavs.model;

// How a customer is related to another customer, e.g. "Spouse" or "Parent".
// Fields follow the mainframe customer.AddRelationship and customerRelationship.Retrieve
// messages. otherType describes the relationship when the type is "Other".
// The id is null until the mainframe has stored the relationship.
public class CustomerRelationship {

    private Long id;
    private String customerNumber;
    private String otherCustomerNumber;
    // only filled in when read back from the mainframe, so the screen can show it
    private String otherCustomerName;
    private String type;
    private String otherType;

    public CustomerRelationship() {
    }

    public CustomerRelationship(String customerNumber, String otherCustomerNumber, String type, String otherType) {
        this.customerNumber = customerNumber;
        this.otherCustomerNumber = otherCustomerNumber;
        this.type = type;
        this.otherType = otherType;
    }

    // Returns true until the mainframe has assigned this relationship an id.
    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public void setCustomerNumber(String customerNumber) {
        this.customerNumber = customerNumber;
    }

    public String getOtherCustomerNumber() {
        return otherCustomerNumber;
    }

    public void setOtherCustomerNumber(String otherCustomerNumber) {
        this.otherCustomerNumber = otherCustomerNumber;
    }

    public String getOtherCustomerName() {
        return otherCustomerName;
    }

    public void setOtherCustomerName(String otherCustomerName) {
        this.otherCustomerName = otherCustomerName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getOtherType() {
        return otherType;
    }

    public void setOtherType(String otherType) {
        this.otherType = otherType;
    }
}
