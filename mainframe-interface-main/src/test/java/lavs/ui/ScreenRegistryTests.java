// AI-assisted: tests that a registered view is built with the parameter, and the placeholder otherwise.
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreenRegistryTests {
    private final ScreenRegistry<String> registry = new ScreenRegistry<>(screen -> "Not built yet: " + screen);

    @Test
    void aRegisteredScreenIsBuiltWithTheParameter() {
        registry.register(Screen.CUSTOMER_RECORD, parameter -> "record " + parameter);

        assertEquals("record C-10234", registry.create(Screen.CUSTOMER_RECORD, "C-10234"));
    }

    @Test
    void anUnregisteredScreenShowsThePlaceholder() {
        assertEquals("Not built yet: LOAN", registry.create(Screen.LOAN, "L-9"));
    }

    @Test
    void registeringTheSameScreenAgainReplacesTheFirstView() {
        registry.register(Screen.LOGIN, parameter -> "first");
        registry.register(Screen.LOGIN, parameter -> "second");

        assertEquals("second", registry.create(Screen.LOGIN, null));
    }

    @Test
    void registeringOneScreenLeavesTheOthersAsPlaceholders() {
        registry.register(Screen.LOGIN, parameter -> "login");

        assertEquals("Not built yet: CUSTOMER_SEARCH", registry.create(Screen.CUSTOMER_SEARCH, null));
    }
}
