// AI-assisted: tests when the component demo screen is switched on.
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoScreenTests {
    @Test
    void offWhenNeitherSwitchIsSet() {
        assertFalse(DemoScreen.isEnabled(null, null));
    }

    @Test
    void onWhenThePropertyIsTrue() {
        assertTrue(DemoScreen.isEnabled("true", null));
    }

    @Test
    void onWhenTheEnvironmentVariableIsTrue() {
        assertTrue(DemoScreen.isEnabled(null, "true"));
    }

    @Test
    void anyCaseOfTrueCounts() {
        assertTrue(DemoScreen.isEnabled("TRUE", null));
        assertTrue(DemoScreen.isEnabled(null, "True"));
    }

    @Test
    void anythingElseIsOff() {
        assertFalse(DemoScreen.isEnabled("false", "no"));
        assertFalse(DemoScreen.isEnabled("1", ""));
    }
}
