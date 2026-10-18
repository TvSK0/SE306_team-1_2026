package lavs.data;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class ResultTypesTests {

    @Test
    void sendResultFactories() {
        assertAll(
                () -> assertEquals(SendResult.Outcome.SUCCESS, SendResult.success().getOutcome()),
                () -> assertNull(SendResult.success().getAssignedNumber()),
                () -> assertEquals("C9", SendResult.success("C9").getAssignedNumber()),
                () -> assertNull(SendResult.success("C9").getMessage()),
                () -> assertEquals(SendResult.Outcome.NETWORK_FAILURE, SendResult.networkFailure("down").getOutcome()),
                () -> assertEquals("down", SendResult.networkFailure("down").getMessage()),
                () -> assertEquals(SendResult.Outcome.REJECTED, SendResult.rejected("bad").getOutcome()),
                () -> assertEquals("bad", SendResult.rejected("bad").getMessage()));
    }

    @Test
    void duplicateCheckResultFactories() {
        List<String> matches = new ArrayList<>(List.of("C1", "C2"));
        DuplicateCheckResult found = DuplicateCheckResult.found(matches);
        matches.clear();

        assertAll(
                () -> assertEquals(DuplicateCheckResult.Outcome.NONE, DuplicateCheckResult.none().getOutcome()),
                () -> assertTrue(DuplicateCheckResult.none().getMatchingNumbers().isEmpty()),
                () -> assertEquals(DuplicateCheckResult.Outcome.FOUND, found.getOutcome()),
                () -> assertEquals(List.of("C1", "C2"), found.getMatchingNumbers()),
                () -> assertNull(found.getMessage()),
                () -> assertEquals("down", DuplicateCheckResult.networkFailure("down").getMessage()),
                () -> assertEquals(DuplicateCheckResult.Outcome.NETWORK_FAILURE,
                        DuplicateCheckResult.networkFailure("down").getOutcome()));
    }
}
