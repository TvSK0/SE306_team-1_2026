// AI-assisted: tests for the login form's required-field check.
package lavs.logic;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LoginFormTests {
    // US1-R3
    @Test
    void acceptsAUsernameAndPassword() {
        // Act
        ValidationResult result = LoginForm.validate("admin", "secret");

        // Assert
        Assertions.assertTrue(result.isValid());
    }

    // US1-R3
    @Test
    void flagsABlankUsername() {
        // Act
        ValidationResult result = LoginForm.validate("", "secret");

        // Assert
        Assertions.assertEquals(List.of(LoginForm.USERNAME), fieldNames(result));
    }

    // US1-R3
    @Test
    void flagsABlankPassword() {
        // Act
        ValidationResult result = LoginForm.validate("admin", null);

        // Assert
        Assertions.assertEquals(List.of(LoginForm.PASSWORD), fieldNames(result));
    }

    // US1-R3
    @Test
    void treatsSpacesOnlyAsBlankAndFlagsBothFields() {
        // Act
        ValidationResult result = LoginForm.validate("   ", "  ");

        // Assert
        Assertions.assertEquals(List.of(LoginForm.USERNAME, LoginForm.PASSWORD), fieldNames(result));
    }

    private static List<String> fieldNames(ValidationResult result) {
        return result.getErrors().stream().map(FieldError::getFieldName).toList();
    }
}
