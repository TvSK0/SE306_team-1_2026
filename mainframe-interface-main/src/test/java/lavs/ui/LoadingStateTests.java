// AI-assisted: tests the loading look for reads (Search) and saves.
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadingStateTests {
    private final LoadingState read = LoadingState.forRead("Search", "Searching...");
    private final LoadingState save = LoadingState.forSave("Save document", "Saving...");

    @Test
    void aReadStartsIdle() {
        assertFalse(read.isLoading());
        assertEquals("Search", read.getButtonText());
        assertFalse(read.isButtonDisabled());
        assertFalse(read.isReadInProgress());
    }

    @Test
    void aRunningReadShowsBusyTextDisabledButtonCancelAndSkeleton() {
        read.start();

        assertTrue(read.isLoading());
        assertEquals("Searching...", read.getButtonText());
        assertTrue(read.isButtonDisabled());
        assertTrue(read.isReadInProgress());
    }

    @Test
    void finishingAReadGoesBackToIdle() {
        read.start();
        read.finish();

        assertFalse(read.isLoading());
        assertEquals("Search", read.getButtonText());
        assertFalse(read.isButtonDisabled());
        assertFalse(read.isReadInProgress());
    }

    @Test
    void startingTwiceIsTheSameAsStartingOnce() {
        read.start();
        read.start();
        read.finish();

        assertFalse(read.isLoading());
    }

    @Test
    void aSaveStartsIdle() {
        assertEquals("Save document", save.getButtonText());
        assertFalse(save.isButtonDisabled());
        assertFalse(save.isReadInProgress());
    }

    @Test
    void aRunningSaveIsBusyAndDisabledButHasNoCancelOrSkeleton() {
        save.start();

        assertEquals("Saving...", save.getButtonText());
        assertTrue(save.isButtonDisabled());
        assertFalse(save.isReadInProgress());
    }

    @Test
    void finishingASaveGoesBackToIdle() {
        save.start();
        save.finish();

        assertEquals("Save document", save.getButtonText());
        assertFalse(save.isButtonDisabled());
    }
}
