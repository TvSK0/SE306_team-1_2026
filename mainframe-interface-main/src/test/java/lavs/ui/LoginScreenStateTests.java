// AI-assisted: tests for what the login screen shows for each outcome, one per US1 requirement.
package lavs.ui;

import lavs.logic.AuthService;
import lavs.logic.LoginAttempt;
import lavs.logic.LoginForm;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import mainframe.client.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LoginScreenStateTests {
    // US1-R1
    @Test
    void aSuccessfulLoginMovesOnWithTheUserName() {
        // Arrange
        LoginAttempt attempt = attempt(new Status(1), "admin", "secret", true, null);

        // Act
        LoginScreenState.Display display = LoginScreenState.show(attempt);

        // Assert
        Assertions.assertEquals("admin", display.signedInUser().orElseThrow());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertTrue(display.fieldErrors().isValid());
    }

    // US1-R1 (Yu's rule)
    @Test
    void staysOnLoginWithABannerWhenThereAreNoDropdownValues() {
        // Arrange
        LoginAttempt attempt = attempt(new Status(1), "admin", "secret", false, "No such category");

        // Act
        LoginScreenState.Display display = LoginScreenState.show(attempt);

        // Assert
        Assertions.assertTrue(display.signedInUser().isEmpty());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't load the dropdown values",
                "Reason given: No such category. Try logging in again."), display.banner().orElseThrow());
    }

    // US1-R1 (Yu's rule)
    @Test
    void saysSoWhenTheDropdownValuesFailWithNoReason() {
        // Arrange
        LoginAttempt attempt = attempt(new Status(1), "admin", "secret", false, " ");

        // Act
        BannerContent banner = LoginScreenState.show(attempt).banner().orElseThrow();

        // Assert
        Assertions.assertEquals("The mainframe gave no reason. Try logging in again.", banner.body());
    }

    // US1-R2
    @Test
    void aRejectedLoginShowsTheMainframesReasonInABanner() {
        // Arrange
        LoginAttempt attempt = attempt(FakeConnection.rejected("Invalid username or password").getStatus(),
                "admin", "wrong", true, null);

        // Act
        LoginScreenState.Display display = LoginScreenState.show(attempt);

        // Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't log in",
                        "Reason given: Invalid username or password."
                                + " Check your username and password and try again."),
                display.banner().orElseThrow());
        Assertions.assertTrue(display.signedInUser().isEmpty());
        Assertions.assertTrue(display.fieldErrors().isValid());
    }

    // US1-R2
    @Test
    void doesNotAddASecondFullStopToTheReason() {
        // Arrange
        LoginAttempt attempt = attempt(FakeConnection.rejected("Account locked.").getStatus(),
                "admin", "wrong", true, null);

        // Act
        BannerContent banner = LoginScreenState.show(attempt).banner().orElseThrow();

        // Assert
        Assertions.assertTrue(banner.body().startsWith("Reason given: Account locked. Check"));
    }

    // US1-R2
    @Test
    void aRejectedLoginWithNoReasonSaysSo() {
        // Arrange
        LoginAttempt attempt = attempt(FakeConnection.rejected(null).getStatus(), "admin", "wrong", true, null);

        // Act
        BannerContent banner = LoginScreenState.show(attempt).banner().orElseThrow();

        // Assert
        Assertions.assertEquals("The mainframe gave no reason. Check your username and password and try again.",
                banner.body());
    }

    // US1-R3
    @Test
    void anIncompleteFormShowsInlineFieldErrorsAndNoBanner() {
        // Arrange
        LoginAttempt attempt = attempt(new Status(1), "", " ", true, null);

        // Act
        LoginScreenState.Display display = LoginScreenState.show(attempt);

        // Assert
        Assertions.assertEquals(List.of(LoginForm.USERNAME, LoginForm.PASSWORD),
                display.fieldErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertTrue(display.signedInUser().isEmpty());
    }

    // US1-R4
    @Test
    void noConnectionShowsTheNetworkFailureBanner() {
        // Arrange
        LoginAttempt attempt = attempt(FakeConnection.networkDown().getStatus(), "admin", "secret", true, null);

        // Act
        LoginScreenState.Display display = LoginScreenState.show(attempt);

        // Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Can't reach the mainframe",
                "Check your internet connection and try again."), display.banner().orElseThrow());
        Assertions.assertTrue(display.signedInUser().isEmpty());
        Assertions.assertTrue(display.fieldErrors().isValid());
    }

    // US1-R4 (the permanent note under the button, Figma 01)
    @Test
    void theNoteUnderTheButtonHasTheFigmaWording() {
        Assertions.assertEquals(
                "An internet connection is needed to log in. Once signed in, you can keep working offline.",
                LoginScreenState.CONNECTION_NOTE);
    }

    // US1-R2 (something nobody planned for must still leave the user on Login with a message)
    @Test
    void anUnexpectedFailureShowsABannerWithItsMessage() {
        // Act
        LoginScreenState.Display display = LoginScreenState.showUnexpected(new IllegalStateException("boom"));

        // Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't log in",
                "Something went wrong: boom."), display.banner().orElseThrow());
        Assertions.assertTrue(display.signedInUser().isEmpty());
    }

    // US1-R2
    @Test
    void anUnexpectedFailureWithoutAMessageStillShowsABanner() {
        // Act
        BannerContent banner = LoginScreenState.showUnexpected(new IllegalStateException()).banner().orElseThrow();

        // Assert
        Assertions.assertEquals("Something went wrong.", banner.body());
    }

    // the AuthResult factories are not public, so the result comes from the real AuthService
    private static LoginAttempt attempt(Status loginStatus, String username, String password,
                                        boolean constantsAvailable, String constantsMessage) {
        FakeConnection connection = new FakeConnection();
        connection.setLoginStatus(loginStatus);
        return new LoginAttempt(new AuthService(connection).login(username, password), constantsAvailable,
                constantsMessage);
    }
}
