package lavs.model;

// A customer's phone number. In the mainframe messages 'number' refers to the phone number itself so
// the owning customer is kept separately as customerNumber. Fields follow the mainframe customer.AddPhoneNumber,
// customerPhoneNumber.Retrieve messages.
public class PhoneNumber {

    private Long id;
    private String customerNumber;
    private String type;
    private String otherType;
    private String countryCode;
    private String number;
    private boolean primary;
    private boolean canTxt;

    public PhoneNumber() {
    }

    public PhoneNumber(String customerNumber, String type, String countryCode, String number,
                       boolean primary, boolean canTxt) {
        this.customerNumber = customerNumber;
        this.type = type;
        this.countryCode = countryCode;
        this.number = number;
        this.primary = primary;
        this.canTxt = canTxt;
    }

    /** True until the mainframe has assigned this phone number an id. */
    // Returns true until the mainframe has assigned this phone number an id.
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

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean canTxt() {
        return canTxt;
    }

    public void setCanTxt(boolean canTxt) {
        this.canTxt = canTxt;
    }
}
