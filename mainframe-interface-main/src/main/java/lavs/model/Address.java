package lavs.model;

// A customer's address. The id is null until the mainframe has stored the address.
public class Address {

    private Long id;
    private String customerNumber;
    private String type;
    private String otherType;
    private boolean primary;
    private boolean mailing;
    private String line1;
    private String line2;
    private String suburb;
    private String city;
    private String postCode;
    private String country;

    public Address() {
    }

    public Address(String customerNumber, String type, String line1, String line2, String suburb,
                   String city, String postCode, String country, boolean primary, boolean mailing) {
        this.customerNumber = customerNumber;
        this.type = type;
        this.line1 = line1;
        this.line2 = line2;
        this.suburb = suburb;
        this.city = city;
        this.postCode = postCode;
        this.country = country;
        this.primary = primary;
        this.mailing = mailing;
    }

    /** True until the mainframe has assigned this address an id. */
    // Returns True until the mainframe has assigned this address an id.
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

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public boolean isMailing() {
        return mailing;
    }

    public void setMailing(boolean mailing) {
        this.mailing = mailing;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getSuburb() {
        return suburb;
    }

    public void setSuburb(String suburb) {
        this.suburb = suburb;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostCode() {
        return postCode;
    }

    public void setPostCode(String postCode) {
        this.postCode = postCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
