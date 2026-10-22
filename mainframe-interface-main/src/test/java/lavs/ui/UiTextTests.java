// AI-assisted: tests the small text checks shared by the UI controls.
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiTextTests {
    @Test
    void nullEmptyAndBlankHaveNoText() {
        assertFalse(UiText.hasText(null));
        assertFalse(UiText.hasText(""));
        assertFalse(UiText.hasText("   "));
    }

    @Test
    void anythingElseHasText() {
        assertTrue(UiText.hasText("Find a customer"));
    }

    @Test
    void endWithFullStopAddsOneWhenMissing() {
        assertEquals("Add a mailing address.", UiText.endWithFullStop("Add a mailing address"));
    }

    @Test
    void endWithFullStopLeavesAnExistingOne() {
        assertEquals("Add a mailing address.", UiText.endWithFullStop("Add a mailing address."));
    }
}
