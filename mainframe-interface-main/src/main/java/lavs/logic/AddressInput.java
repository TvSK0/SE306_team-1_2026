package lavs.logic;

import lavs.model.Address;

// AI-assisted: US4, what the MMM typed into the address form, before it is checked. The
// field names are what AddressService uses in its FieldErrors.
public final class AddressInput {
    public static final String TYPE = ContactRules.TYPE;
    public static final String OTHER_TYPE = ContactRules.OTHER_TYPE;
    public static final String LINE1 = "line1";
    public static final String LINE2 = "line2";
    public static final String SUBURB = "suburb";
    public static final String CITY = "city";
    public static final String POST_CODE = "postCode";
    public static final String COUNTRY = "country";
    public static final String TYPE_OTHER = ContactRules.TYPE_OTHER;
    public static final String NEW_ZEALAND = "New Zealand";

    private final String _type;
    private final String _otherType;
    private final String _line1;
    private final String _line2;
    private final String _suburb;
    private final String _city;
    private final String _postCode;
    private final String _country;
    private final boolean _primary;
    private final boolean _mailing;

    public AddressInput(String type, String otherType, String line1, String line2, String suburb, String city,
                        String postCode, String country, boolean primary, boolean mailing) {
        _type = type;
        _otherType = otherType;
        _line1 = line1;
        _line2 = line2;
        _suburb = suburb;
        _city = city;
        _postCode = postCode;
        _country = country;
        _primary = primary;
        _mailing = mailing;
    }

    // the form filled in from a saved address; a missing value becomes an empty field
    public static AddressInput fromAddress(Address address) {
        return new AddressInput(address.getType(), ContactRules.text(address.getOtherType()),
                ContactRules.text(address.getLine1()), ContactRules.text(address.getLine2()),
                ContactRules.text(address.getSuburb()), ContactRules.text(address.getCity()),
                ContactRules.text(address.getPostCode()), ContactRules.text(address.getCountry()),
                address.isPrimary(), address.isMailing());
    }

    public String getType() {
        return _type;
    }

    public String getOtherType() {
        return _otherType;
    }

    public String getLine1() {
        return _line1;
    }

    public String getLine2() {
        return _line2;
    }

    public String getSuburb() {
        return _suburb;
    }

    public String getCity() {
        return _city;
    }

    public String getPostCode() {
        return _postCode;
    }

    public String getCountry() {
        return _country;
    }

    public boolean isPrimary() {
        return _primary;
    }

    public boolean isMailing() {
        return _mailing;
    }
}
