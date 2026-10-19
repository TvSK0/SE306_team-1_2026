// AI-assisted: tests that a registered view is built with the parameter, the placeholder otherwise,
// and that a view that fails to build leaves the navigation state untouched.
package lavs.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScreenRegistryTests {
    private final ScreenRegistry<String> registry = new ScreenRegistry<>(screen -> "Not built yet: " + screen);
    private final List<Throwable> errors = new ArrayList<>();

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

    @Test
    void tryCreateGivesTheViewAndReportsNothingWhenItWorks() {
        registry.register(Screen.LOGIN, parameter -> "login");

        Optional<String> view = registry.tryCreate(Screen.LOGIN, null, errors::add);

        assertEquals(Optional.of("login"), view);
        assertTrue(errors.isEmpty());
    }

    @Test
    void tryCreateReportsAFactoryThatThrowsAndGivesNothing() {
        RuntimeException boom = new IllegalStateException("boom");
        registry.register(Screen.CUSTOMER_RECORD, parameter -> {
            throw boom;
        });

        Optional<String> view = registry.tryCreate(Screen.CUSTOMER_RECORD, "C-1", errors::add);

        assertFalse(view.isPresent());
        assertEquals(List.of(boom), errors);
    }

    // the same build-then-commit flow Navigator.goTo uses
    @Test
    void aFailedBuildLeavesTheNavigationStateUntouched() {
        NavigatorState<String> state = new NavigatorState<>(Screen.CUSTOMER_SEARCH, "jane", "search view");
        registry.register(Screen.CUSTOMER_RECORD, parameter -> {
            throw new IllegalStateException("boom");
        });

        registry.tryCreate(Screen.CUSTOMER_RECORD, "C-1", errors::add)
                .ifPresent(view -> state.goTo(Screen.CUSTOMER_RECORD, "C-1", view));

        assertEquals(1, errors.size());
        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertEquals("jane", state.getParameter());
        assertEquals("search view", state.getView());
        assertFalse(state.canGoBack());
    }

    @Test
    void aSuccessfulBuildIsCommittedToTheNavigationState() {
        NavigatorState<String> state = new NavigatorState<>(Screen.CUSTOMER_SEARCH, "jane", "search view");
        registry.register(Screen.CUSTOMER_RECORD, parameter -> "record " + parameter);

        registry.tryCreate(Screen.CUSTOMER_RECORD, "C-1", errors::add)
                .ifPresent(view -> state.goTo(Screen.CUSTOMER_RECORD, "C-1", view));

        assertEquals(Screen.CUSTOMER_RECORD, state.getScreen());
        assertEquals("record C-1", state.getView());
        assertTrue(state.canGoBack());
    }
}
