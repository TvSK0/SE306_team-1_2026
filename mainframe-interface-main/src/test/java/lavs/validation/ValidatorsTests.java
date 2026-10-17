package lavs.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class ValidatorsTests {

    // --- required ---

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void requiredFailsOnNullOrBlank(String value) {
        Optional<FieldError> error = Validators.required("name", value);

        assertTrue(error.isPresent());
        assertEquals("name", error.get().getFieldName());
    }

    @Test
    void requiredPassesOnNonBlankValue() {
        assertTrue(Validators.required("name", "Jane").isEmpty());
    }

    // --- date ---

    @ParameterizedTest
    @NullAndEmptySource
    void dateSkipsNullOrBlank(String value) {
        assertTrue(Validators.date("dateOfBirth", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2001-02-03", "2000-02-29", "1999-12-31"})
    void datePassesOnValidIsoDates(String value) {
        assertTrue(Validators.date("dateOfBirth", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"2001-02-30", "03/02/2001", "not-a-date", "2001-13-01", "2001-02-3"})
    void dateFailsOnInvalidValues(String value) {
        Optional<FieldError> error = Validators.date("dateOfBirth", value);

        assertTrue(error.isPresent());
        assertEquals("dateOfBirth", error.get().getFieldName());
    }

    // --- phone ---

    @ParameterizedTest
    @NullAndEmptySource
    void phoneSkipsNullOrBlank(String value) {
        assertTrue(Validators.phone("phone", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0211234567", "+64 21 123 4567", "(09) 123-4567", "09-123-4567"})
    void phonePassesOnValidShapes(String value) {
        assertTrue(Validators.phone("phone", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"123", "call-me-maybe", "123456789012345678", "++6421234567"})
    void phoneFailsOnInvalidValues(String value) {
        Optional<FieldError> error = Validators.phone("phone", value);

        assertTrue(error.isPresent());
        assertEquals("phone", error.get().getFieldName());
    }

    // --- email ---

    @ParameterizedTest
    @NullAndEmptySource
    void emailSkipsNullOrBlank(String value) {
        assertTrue(Validators.email("email", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"jane@example.com", "jane.doe+test@sub.example.co.nz"})
    void emailPassesOnValidAddresses(String value) {
        assertTrue(Validators.email("email", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"jane@", "@example.com", "jane example.com", "jane@example", "jane @example.com"})
    void emailFailsOnInvalidAddresses(String value) {
        Optional<FieldError> error = Validators.email("email", value);

        assertTrue(error.isPresent());
        assertEquals("email", error.get().getFieldName());
    }

    // --- currency ---

    @ParameterizedTest
    @NullAndEmptySource
    void currencySkipsNullOrBlank(String value) {
        assertTrue(Validators.currency("amount", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "1500", "1500.00", "1500.5", "0.99"})
    void currencyPassesOnValidAmounts(String value) {
        assertTrue(Validators.currency("amount", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-5", "1,500.00", "1500.005", "abc", "1500."})
    void currencyFailsOnInvalidAmounts(String value) {
        Optional<FieldError> error = Validators.currency("amount", value);

        assertTrue(error.isPresent());
        assertEquals("amount", error.get().getFieldName());
    }

    // --- postcode ---

    @ParameterizedTest
    @NullAndEmptySource
    void postcodeSkipsNullOrBlank(String value) {
        assertTrue(Validators.postcode("postcode", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"1010", "0600", "9999"})
    void postcodePassesOnFourDigits(String value) {
        assertTrue(Validators.postcode("postcode", value).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"101", "10100", "ABCD", "10 10"})
    void postcodeFailsOnInvalidValues(String value) {
        Optional<FieldError> error = Validators.postcode("postcode", value);

        assertTrue(error.isPresent());
        assertEquals("postcode", error.get().getFieldName());
    }
}
