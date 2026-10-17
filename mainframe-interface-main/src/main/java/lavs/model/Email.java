package lavs.model;

// A customer's email address. The id is null until the mainframe has stored it.
public class Email {

    private Long id;
    private String customerNumber;
    private String type;
    private String otherType;
    private String address;
    private boolean primary;

    public Email() {
    }

    public Email(String customerNumber, String type, String address, boolean primary) {
        this.customerNumber = customerNumber;
        this.type = type;
        this.address = address;
        this.primary = primary;
    }

    // Returns true until the mainframe has assigned this email an id.
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }
}
