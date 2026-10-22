// AI-assisted: the login form's field names and its required-field check.
package lavs.logic;

import lavs.validation.ValidationResult;
import lavs.validation.Validators;

// Checks the login form without contacting the mainframe. A field error's fieldName is how
// the login screen finds the field to highlight.
public final class LoginForm {
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    private LoginForm() {
    }

    // both fields are required; spaces only counts as blank
    public static ValidationResult validate(String username, String password) {
        ValidationResult result = new ValidationResult();
        result.addIfPresent(Validators.required(USERNAME, username));
        result.addIfPresent(Validators.required(PASSWORD, password));
        return result;
    }
}
