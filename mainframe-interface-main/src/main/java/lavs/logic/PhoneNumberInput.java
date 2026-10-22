package lavs.logic;

import lavs.model.PhoneNumber;

// AI-assisted: US4, what the MMM typed into the phone number form, before it is checked.
// The field names are what PhoneNumberService uses in its FieldErrors.
public final class PhoneNumberInput {
    public static final String TYPE = ContactRules.TYPE;
    public static final String OTHER_TYPE = ContactRules.OTHER_TYPE;
    public static final String COUNTRY_CODE = "countryCode";
    public static final String NUMBER = "number";
    public static final String TYPE_OTHER = ContactRules.TYPE_OTHER;

    private final String _type;
    private final String _otherType;
    private final String _countryCode;
    private final String _number;
    private final boolean _primary;
    private final boolean _canTxt;

    public PhoneNumberInput(String type, String otherType, String countryCode, String number,
                            boolean primary, boolean canTxt) {
        _type = type;
        _otherType = otherType;
        _countryCode = countryCode;
        _number = number;
        _primary = primary;
        _canTxt = canTxt;
    }

    // the form filled in from a saved phone number; a missing value becomes an empty field
    public static PhoneNumberInput fromPhoneNumber(PhoneNumber phone) {
        return new PhoneNumberInput(phone.getType(), ContactRules.text(phone.getOtherType()),
                ContactRules.text(phone.getCountryCode()), ContactRules.text(phone.getNumber()),
                phone.isPrimary(), phone.canTxt());
    }

    public String getType() {
        return _type;
    }

    public String getOtherType() {
        return _otherType;
    }

    public String getCountryCode() {
        return _countryCode;
    }

    public String getNumber() {
        return _number;
    }

    public boolean isPrimary() {
        return _primary;
    }

    public boolean canTxt() {
        return _canTxt;
    }
}
