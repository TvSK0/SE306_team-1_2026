package lavs.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class FieldErrorTests {
    @Test
    void exposesFieldNameAndMessage() {
        FieldError error = new FieldError("email", "is required");

        assertEquals("email", error.getFieldName());
        assertEquals("is required", error.getMessage());
    }

    @Test
    void equalWhenFieldNameAndMessageMatch() {
        assertEquals(new FieldError("email", "is required"), new FieldError("email", "is required"));
        assertNotEquals(new FieldError("email", "is required"), new FieldError("phone", "is required"));
        assertNotEquals(new FieldError("email", "is required"), new FieldError("email", "must be valid"));
    }

    @Test
    void rejectsBlankFieldName() {
        assertThrows(IllegalArgumentException.class, () -> new FieldError("  ", "is required"));
        assertThrows(IllegalArgumentException.class, () -> new FieldError(null, "is required"));
    }

    @Test
    void rejectsBlankMessage() {
        assertThrows(IllegalArgumentException.class, () -> new FieldError("email", "  "));
        assertThrows(IllegalArgumentException.class, () -> new FieldError("email", null));
    }
}
