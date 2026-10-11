package lavs.validation;

// AI-assisted: one validation failure. Validators never throw - they report
// failures as FieldErrors instead, so a screen can show every problem with a
// form at once rather than stopping at the first one.
public final class FieldError {
    private final String fieldName;
    private final String message;

    public FieldError(String fieldName, String message) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("fieldName is required");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("message is required");
        }
        this.fieldName = fieldName;
        this.message = message;
    }

    // the name of the field that failed, e.g. "email"
    public String getFieldName() {
        return fieldName;
    }

    // a human-readable explanation of why it failed
    public String getMessage() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof FieldError that)) return false;
        return fieldName.equals(that.fieldName) && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(fieldName, message);
    }

    @Override
    public String toString() {
        return fieldName + ": " + message;
    }
}
