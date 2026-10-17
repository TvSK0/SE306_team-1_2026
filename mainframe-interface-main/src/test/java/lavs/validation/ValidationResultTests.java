package lavs.validation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class ValidationResultTests {
    @Test
    void validWhenNothingWasAdded() {
        ValidationResult result = new ValidationResult();

        assertTrue(result.isValid());
        assertEquals(0, result.getErrors().size());
    }

    @Test
    void invalidOnceAnErrorIsAdded() {
        ValidationResult result = new ValidationResult();

        result.add(new FieldError("email", "is required"));

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("email", result.getErrors().get(0).getFieldName());
    }

    @Test
    void addIfPresentIgnoresEmptyOptionals() {
        ValidationResult result = new ValidationResult();

        result.addIfPresent(Optional.empty());

        assertTrue(result.isValid());
    }

    @Test
    void addIfPresentRecordsPresentOptionals() {
        ValidationResult result = new ValidationResult();

        result.addIfPresent(Optional.of(new FieldError("phone", "is required")));

        assertFalse(result.isValid());
        assertEquals("phone", result.getErrors().get(0).getFieldName());
    }

    @Test
    void mergeCombinesErrorsFromBothResultsInOrder() {
        ValidationResult first = new ValidationResult();
        first.add(new FieldError("email", "is required"));
        ValidationResult second = new ValidationResult();
        second.add(new FieldError("phone", "is required"));

        first.merge(second);

        assertEquals(2, first.getErrors().size());
        assertEquals("email", first.getErrors().get(0).getFieldName());
        assertEquals("phone", first.getErrors().get(1).getFieldName());
        // the result that was merged in is untouched
        assertEquals(1, second.getErrors().size());
    }

    @Test
    void getErrorsCannotBeModifiedByCallers() {
        ValidationResult result = new ValidationResult();
        result.add(new FieldError("email", "is required"));

        assertThrows(UnsupportedOperationException.class, () -> result.getErrors().add(new FieldError("phone", "is required")));
    }
}
