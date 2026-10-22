package lavs.logic;

import lavs.validation.ValidationResult;
import lavs.validation.Validators;

// AI-assisted: US4, the checks addresses, phone numbers and emails share. Each has a type
// from the mainframe's list, and an "Other" type needs a description of what it is.
final class ContactRules {
    // every contact type list has "Other", spelled the same
    static final String TYPE_OTHER = mainframe.client.messages.customer.AddAddress.TYPE_OTHER;
    static final String TYPE = "type";
    static final String OTHER_TYPE = "otherType";

    private ContactRules() {}

    static void checkType(ValidationResult result, String type, String otherType) {
        result.addIfPresent(Validators.required(TYPE, type));
        if (TYPE_OTHER.equals(type)) {
            result.addIfPresent(Validators.required(OTHER_TYPE, otherType));
        }
    }

    // the description only means something for an "Other" type, so it isn't sent otherwise
    static String otherTypeToSend(String type, String otherType) {
        return TYPE_OTHER.equals(type) ? trimmed(otherType) : null;
    }

    // an optional field left blank is sent as nothing rather than as spaces
    static String trimmed(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    // a missing value shown in a form field
    static String text(String value) {
        return value == null ? "" : value;
    }
}
