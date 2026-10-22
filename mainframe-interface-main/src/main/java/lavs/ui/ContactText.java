package lavs.ui;

import lavs.model.Address;
import lavs.model.Email;
import lavs.model.PhoneNumber;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// AI-assisted: US4, how the Addresses, Phone numbers and Emails tabs name things and what
// they say after each change. No JavaFX, so it is unit tested.
public final class ContactText {
    private static final String OTHER = "Other";

    // the three kinds of contact detail, with the words each tab uses for its own
    public enum Kind {
        ADDRESS("address", "addresses", "an"),
        PHONE_NUMBER("phone number", "phone numbers", "a"),
        EMAIL("email", "emails", "an");

        private final String _singular;
        private final String _plural;
        private final String _article;

        Kind(String singular, String plural, String article) {
            _singular = singular;
            _plural = plural;
            _article = article;
        }
    }

    private ContactText() {}

    // --- the tab's form and messages ---

    // "Add an address", or "Edit address" while one is being edited
    public static String formTitle(Kind kind, boolean editing) {
        return editing ? "Edit " + kind._singular : "Add " + kind._article + " " + kind._singular;
    }

    // "Add address", or "Save changes" while one is being edited
    public static String saveButton(Kind kind, boolean editing) {
        return editing ? "Save changes" : "Add " + kind._singular;
    }

    // "Address added." or "Phone number saved."
    public static String saved(Kind kind, boolean editing) {
        return capitalise(kind._singular) + (editing ? " saved." : " added.");
    }

    public static String removed(Kind kind) {
        return capitalise(kind._singular) + " removed.";
    }

    // "Remove the address 1 Queen St, Auckland?"
    public static String confirmRemove(Kind kind, String what) {
        return "Remove the " + kind._singular + " " + what + "?";
    }

    public static String noneYet(Kind kind) {
        return "No " + kind._plural + " yet.";
    }

    public static String loading(Kind kind) {
        return "Loading " + kind._plural + "...";
    }

    public static String notLoaded(Kind kind) {
        return "No " + kind._plural + " loaded.";
    }

    // "The addresses", for "The addresses could not be loaded: ..."
    public static String whatCouldNotLoad(Kind kind) {
        return "The " + kind._plural;
    }

    // --- the table's columns ---

    // "Residential", or "Other: Bach" when the type is Other and has a description
    public static String type(String type, String otherType) {
        if (OTHER.equals(type) && !isBlank(otherType)) {
            return OTHER + ": " + otherType.trim();
        }
        return type == null ? "" : type;
    }

    // "1 Queen St, Unit 2, CBD, Auckland 1010, New Zealand", leaving out what is missing
    public static String oneLine(Address address) {
        String cityAndPostcode = Stream.of(address.getCity(), address.getPostCode())
                .filter(part -> !isBlank(part)).map(String::trim).collect(Collectors.joining(" "));
        return Stream.of(address.getLine1(), address.getLine2(), address.getSuburb(), cityAndPostcode, address.getCountry())
                .filter(part -> !isBlank(part)).map(String::trim).collect(Collectors.joining(", "));
    }

    // "+64 21 123 4567", or just the number when there is no country code
    public static String number(PhoneNumber phone) {
        return isBlank(phone.getCountryCode()) ? phone.getNumber() : phone.getCountryCode().trim() + " " + phone.getNumber();
    }

    // "Primary, Mailing" (empty when neither)
    public static String flags(Address address) {
        return flags(address.isPrimary(), "Mailing", address.isMailing());
    }

    // "Primary, Can text" (empty when neither)
    public static String flags(PhoneNumber phone) {
        return flags(phone.isPrimary(), "Can text", phone.canTxt());
    }

    // "Primary" or empty
    public static String flags(Email email) {
        return flags(email.isPrimary(), null, false);
    }

    private static String flags(boolean primary, String otherFlag, boolean other) {
        List<String> flags = new ArrayList<>();
        if (primary) {
            flags.add("Primary");
        }
        if (other) {
            flags.add(otherFlag);
        }
        return String.join(", ", flags);
    }

    private static String capitalise(String text) {
        return text.substring(0, 1).toUpperCase() + text.substring(1);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
