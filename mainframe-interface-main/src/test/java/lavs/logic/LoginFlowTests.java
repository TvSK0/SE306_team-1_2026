// AI-assisted: tests for LoginFlow (login, then the dropdown values), one per US1 requirement.
package lavs.logic;

import lavs.data.ConstantsCache;
import lavs.data.ConstantsLoadResult;
import lavs.data.ConstantsSource;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LoginFlowTests {
    private static final ConstantsLoadResult RATE_TYPES = ConstantsLoadResult.success(Map.of(
            ConstantCategories.LOAN_RATE_TYPE, List.of("Fixed", "Floating")));

    // US1-R1
    @Test
    void logsInThenRefreshesTheDropdownValues() {
        // Arrange
        AtomicInteger refreshes = new AtomicInteger();
        LoginFlow flow = flow(new FakeConnection(), () -> {
            refreshes.incrementAndGet();
            return RATE_TYPES;
        }, ConstantsCache.NONE);

        // Act
        LoginAttempt attempt = flow.run("admin", "secret");

        // Assert
        Assertions.assertTrue(attempt.auth().isSuccessful());
        Assertions.assertTrue(attempt.constantsAvailable());
        Assertions.assertEquals(1, refreshes.get());
    }

    // US1-R2
    @Test
    void aRejectedLoginDoesNotRefreshTheDropdownValues() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setLoginStatus(FakeConnection.rejected("Invalid username or password").getStatus());
        AtomicInteger refreshes = new AtomicInteger();
        LoginFlow flow = flow(connection, () -> {
            refreshes.incrementAndGet();
            return RATE_TYPES;
        }, ConstantsCache.NONE);

        // Act
        LoginAttempt attempt = flow.run("admin", "wrong");

        // Assert
        Assertions.assertEquals(AuthResult.Outcome.REJECTED, attempt.auth().getOutcome());
        Assertions.assertEquals(0, refreshes.get());
    }

    // US1-R3
    @Test
    void anIncompleteFormDoesNotRefreshTheDropdownValues() {
        // Arrange
        AtomicInteger refreshes = new AtomicInteger();
        LoginFlow flow = flow(new FakeConnection(), () -> {
            refreshes.incrementAndGet();
            return RATE_TYPES;
        }, ConstantsCache.NONE);

        // Act
        LoginAttempt attempt = flow.run("", "");

        // Assert
        Assertions.assertEquals(AuthResult.Outcome.INVALID, attempt.auth().getOutcome());
        Assertions.assertEquals(0, refreshes.get());
    }

    // US1-R1 (Yu's rule: values saved from last run are enough to carry on with)
    @Test
    void carriesOnWhenTheRefreshFailsButValuesWereSaved() {
        // Arrange
        ConstantsCache saved = new ConstantsCache() {
            @Override
            public void saveAll(Map<String, List<String>> values) {
            }

            @Override
            public Map<String, List<String>> loadAll() {
                return Map.of(ConstantCategories.LOAN_RATE_TYPE, List.of("Fixed"));
            }
        };
        FakeConnection connection = new FakeConnection();
        ConstantsService constants = new ConstantsService(() -> ConstantsLoadResult.networkFailure("down"), saved);
        constants.loadSaved();
        AuthService auth = new AuthService(connection);

        // Act
        LoginAttempt attempt = new LoginFlow(auth, constants).run("admin", "secret");

        // Assert
        Assertions.assertTrue(attempt.auth().isSuccessful());
        Assertions.assertTrue(attempt.constantsAvailable());
        Assertions.assertEquals("admin", auth.currentUser());
    }

    // US1-R1
    @Test
    void staysSignedOutWhenTheRefreshFailsAndNothingWasSaved() {
        // Arrange
        AuthService auth = new AuthService(new FakeConnection());
        ConstantsService constants = new ConstantsService(() -> ConstantsLoadResult.rejected("No such category"),
                ConstantsCache.NONE);

        // Act
        LoginAttempt attempt = new LoginFlow(auth, constants).run("admin", "secret");

        // Assert
        Assertions.assertTrue(attempt.auth().isSuccessful());
        Assertions.assertFalse(attempt.constantsAvailable());
        Assertions.assertEquals("No such category", attempt.constantsMessage());
        Assertions.assertNull(auth.currentUser());
    }

    private static LoginFlow flow(FakeConnection connection, ConstantsSource source, ConstantsCache cache) {
        return new LoginFlow(new AuthService(connection), new ConstantsService(source, cache));
    }
}
