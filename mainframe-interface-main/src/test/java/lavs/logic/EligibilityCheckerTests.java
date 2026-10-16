package lavs.logic;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted
class EligibilityCheckerTests {

    // every combination of the three booleans: eligible only when there is a
    // mailing address AND (a phone number OR an email)
    @ParameterizedTest
    @CsvSource({
            "true,  true,  true,  true",
            "true,  true,  false, true",
            "true,  false, true,  true",
            "true,  false, false, false",
            "false, true,  true,  false",
            "false, true,  false, false",
            "false, false, true,  false",
            "false, false, false, false",
    })
    void followsTheTruthTable(boolean hasMailingAddress, boolean hasPhoneNumber, boolean hasEmail, boolean expected) {
        boolean actual = EligibilityChecker.isEligible(hasMailingAddress, hasPhoneNumber, hasEmail);

        assertEqualsBoolean(expected, actual);
    }

    private static void assertEqualsBoolean(boolean expected, boolean actual) {
        if (expected) {
            assertTrue(actual);
        } else {
            assertFalse(actual);
        }
    }

    @Test
    void collectionOverloadIsEligibleWithAddressAndPhoneOnly() {
        boolean eligible = EligibilityChecker.isEligible(
                List.of("123 Queen St"), List.of("021 123 4567"), List.of());

        assertTrue(eligible);
    }

    @Test
    void collectionOverloadIsEligibleWithAddressAndEmailOnly() {
        boolean eligible = EligibilityChecker.isEligible(
                List.of("123 Queen St"), List.of(), List.of("jane@example.com"));

        assertTrue(eligible);
    }

    @Test
    void collectionOverloadIsIneligibleWithoutAMailingAddress() {
        boolean eligible = EligibilityChecker.isEligible(
                List.of(), List.of("021 123 4567"), List.of("jane@example.com"));

        assertFalse(eligible);
    }

    @Test
    void collectionOverloadIsIneligibleWithoutAnyWayToReachThem() {
        boolean eligible = EligibilityChecker.isEligible(
                List.of("123 Queen St"), List.of(), List.of());

        assertFalse(eligible);
    }

    @Test
    void collectionOverloadTreatsNullCollectionsAsAbsent() {
        boolean eligible = EligibilityChecker.isEligible(null, null, List.of("jane@example.com"));

        assertFalse(eligible);
    }
}
