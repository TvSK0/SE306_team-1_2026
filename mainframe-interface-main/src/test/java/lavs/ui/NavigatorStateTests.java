// AI-assisted: tests the navigation rules (current screen, parameters, Back, reset).
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigatorStateTests {
    private final NavigatorState state = new NavigatorState(Screen.LOGIN, null);

    @Test
    void startsOnTheFirstScreenWithNothingToGoBackTo() {
        assertEquals(Screen.LOGIN, state.getScreen());
        assertNull(state.getParameter());
        assertFalse(state.canGoBack());
    }

    @Test
    void goToShowsTheScreenWithItsParameter() {
        state.goTo(Screen.CUSTOMER_RECORD, "C-10234");

        assertEquals(Screen.CUSTOMER_RECORD, state.getScreen());
        assertEquals("C-10234", state.getParameter());
        assertTrue(state.canGoBack());
    }

    @Test
    void backReturnsToThePreviousScreenWithItsOwnParameter() {
        state.goTo(Screen.CUSTOMER_SEARCH, "jane");
        state.goTo(Screen.CUSTOMER_RECORD, "C-10234");

        assertTrue(state.back());

        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertEquals("jane", state.getParameter());
    }

    @Test
    void backWalksTheWholeStackInOrder() {
        state.goTo(Screen.CUSTOMER_SEARCH, null);
        state.goTo(Screen.CUSTOMER_RECORD, "C-1");
        state.goTo(Screen.LOAN, "L-9");

        state.back();
        assertEquals(Screen.CUSTOMER_RECORD, state.getScreen());
        state.back();
        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        state.back();
        assertEquals(Screen.LOGIN, state.getScreen());
        assertFalse(state.canGoBack());
    }

    @Test
    void backWithNothingToGoBackToStaysPutAndSaysSo() {
        assertFalse(state.back());

        assertEquals(Screen.LOGIN, state.getScreen());
    }

    @Test
    void resetToClearsTheBackStack() {
        state.goTo(Screen.CUSTOMER_SEARCH, null);
        state.goTo(Screen.CUSTOMER_RECORD, "C-1");

        state.resetTo(Screen.CUSTOMER_SEARCH, null);

        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertFalse(state.canGoBack());
        assertFalse(state.back());
    }

    @Test
    void afterResetBackCannotReturnToTheLoginScreen() {
        state.resetTo(Screen.CUSTOMER_SEARCH, null);
        state.goTo(Screen.CUSTOMER_RECORD, "C-1");

        state.back();
        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertFalse(state.back());
    }
}
