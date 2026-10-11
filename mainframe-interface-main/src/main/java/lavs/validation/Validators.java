package lavs.validation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

// AI-assisted: shared field validators. Every method checks one field and
// returns an Optional<FieldError> - empty when the field is fine, present when
// it is not - instead of throwing, so a caller can run every check for a form
// and report every problem at once:
//
//   ValidationResult result = new ValidationResult();
//   result.addIfPresent(Validators.required("email", email));
//   result.addIfPresent(Validators.email("email", email));
//
// The format checks (date/phone/email/currency/postcode) only judge the shape
// of a non-blank value; pair them with required() to also demand the field be
// present. That keeps "is it there" and "is it well-formed" as separate,
// independently reusable checks.
public final class Validators {
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    // loose but practical check: local-part@domain, no whitespace, at least one
    // dot in the domain. Good enough to catch typos without rejecting real
    // addresses with a stricter RFC 5322 pattern.
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    // digits with optional leading '+' and spaces/hyphens/parentheses as
    // separators; 7-15 digits once separators are stripped, matching the ITU
    // E.164 range so both local and international numbers are accepted.
    private static final Pattern PHONE_SHAPE = Pattern.compile("^\\+?[0-9(][0-9\\s()-]*$");

    // a non-negative amount with at most 2 decimal places, e.g. "1500" or "1500.00"
    private static final Pattern CURRENCY = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    // a New Zealand postcode: exactly 4 digits
    private static final Pattern NZ_POSTCODE = Pattern.compile("^\\d{4}$");

    private Validators() {
    }

    // fails when the value is null, empty, or only whitespace
    public static Optional<FieldError> required(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return Optional.of(new FieldError(fieldName, "is required"));
        }
        return Optional.empty();
    }

    // fails when a non-blank value is not a valid calendar date in ISO form
    // (yyyy-MM-dd), e.g. "2001-02-03"
    public static Optional<FieldError> date(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            LocalDate.parse(value, ISO_DATE);
            return Optional.empty();
        } catch (DateTimeParseException e) {
            return Optional.of(new FieldError(fieldName, "must be a valid date in yyyy-MM-dd form"));
        }
    }

    // fails when a non-blank value does not look like a phone number
    public static Optional<FieldError> phone(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        if (!PHONE_SHAPE.matcher(value).matches()) {
            return Optional.of(new FieldError(fieldName, "must contain only digits, spaces, '+', '-' or parentheses"));
        }
        long digitCount = value.chars().filter(Character::isDigit).count();
        if (digitCount < 7 || digitCount > 15) {
            return Optional.of(new FieldError(fieldName, "must have between 7 and 15 digits"));
        }
        return Optional.empty();
    }

    // fails when a non-blank value does not look like an email address
    public static Optional<FieldError> email(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        if (!EMAIL.matcher(value).matches()) {
            return Optional.of(new FieldError(fieldName, "must be a valid email address"));
        }
        return Optional.empty();
    }

    // fails when a non-blank value is not a non-negative amount with at most
    // 2 decimal places
    public static Optional<FieldError> currency(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        if (!CURRENCY.matcher(value).matches()) {
            return Optional.of(new FieldError(fieldName, "must be a non-negative amount with at most 2 decimal places"));
        }
        return Optional.empty();
    }

    // fails when a non-blank value is not a 4-digit New Zealand postcode
    public static Optional<FieldError> postcode(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        if (!NZ_POSTCODE.matcher(value).matches()) {
            return Optional.of(new FieldError(fieldName, "must be a 4-digit New Zealand postcode"));
        }
        return Optional.empty();
    }
}
