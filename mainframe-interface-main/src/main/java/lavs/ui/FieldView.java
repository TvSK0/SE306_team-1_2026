// AI-assisted: what the error presenter needs from a form field, so it can be tested without JavaFX.
package lavs.ui;

// FormField implements this. Tests use a recording fake.
public interface FieldView {
    void setError(String message);

    void clearError();
}
