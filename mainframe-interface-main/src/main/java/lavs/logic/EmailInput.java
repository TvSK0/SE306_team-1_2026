package lavs.logic;

import lavs.model.Email;

// AI-assisted: US4, what the MMM typed into the email form, before it is checked. The field
// names are what EmailService uses in its FieldErrors.
public final class EmailInput {
    public static final String TYPE = ContactRules.TYPE;
    public static final String OTHER_TYPE = ContactRules.OTHER_TYPE;
    public static final String ADDRESS = "address";
    public static final String TYPE_OTHER = ContactRules.TYPE_OTHER;

    private final String _type;
    private final String _otherType;
    private final String _address;
    private final boolean _primary;

    public EmailInput(String type, String otherType, String address, boolean primary) {
        _type = type;
        _otherType = otherType;
        _address = address;
        _primary = primary;
    }

    // the form filled in from a saved email; a missing value becomes an empty field
    public static EmailInput fromEmail(Email email) {
        return new EmailInput(email.getType(), ContactRules.text(email.getOtherType()),
                ContactRules.text(email.getAddress()), email.isPrimary());
    }

    public String getType() {
        return _type;
    }

    public String getOtherType() {
        return _otherType;
    }

    public String getAddress() {
        return _address;
    }

    public boolean isPrimary() {
        return _primary;
    }
}
