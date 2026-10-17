package lavs.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class TemporaryNumbersTests {

    @Test
    void createdNumbersAreTemporaryAndUnique() {
        String first = TemporaryNumbers.create();

        assertTrue(TemporaryNumbers.isTemporary(first));
        assertNotEquals(first, TemporaryNumbers.create());
    }

    @Test
    void realAndMissingNumbersAreNotTemporary() {
        assertFalse(TemporaryNumbers.isTemporary("C123"));
        assertFalse(TemporaryNumbers.isTemporary(null));
    }
}
