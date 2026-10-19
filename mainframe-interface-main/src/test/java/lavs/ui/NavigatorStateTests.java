// AI-assisted: tests the navigation rules (current screen, parameters, stored views, Back, reset).
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigatorStateTests {
    private final NavigatorState<String> state = new NavigatorState<>(Screen.LOGIN, null, "login");

    @Test
    void startsOnTheFirstScreenWithNothingToGoBackTo() {
        assertEquals(Screen.LOGIN, state.getScreen());
        assertNull(state.getParameter());
        assertEquals("login", state.getView());
        assertFalse(state.canGoBack());
    }

    @Test
    void goToShowsTheScreenWithItsParameterAndView() {
        state.goTo(Screen.CUSTOMER_RECORD, "C-10234", "record");

        assertEquals(Screen.CUSTOMER_RECORD, state.getScreen());
        assertEquals("C-10234", state.getParameter());
        assertEquals("record", state.getView());
        assertTrue(state.canGoBack());
    }

    @Test
    void backReturnsToThePreviousScreenWithItsParameterAndTheSameView() {
        state.resetTo(Screen.CUSTOMER_SEARCH, "jane", "search view");
        state.goTo(Screen.CUSTOMER_RECORD, "C-10234", "record view");

        assertTrue(state.back());

        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertEquals("jane", state.getParameter());
        assertEquals("search view", state.getView());
    }

    @Test
    void theStoredViewIsTheSameObjectNotACopy() {
        String view = new String("search view");
        state.resetTo(Screen.CUSTOMER_SEARCH, null, view);
        state.goTo(Screen.CUSTOMER_RECORD, null, "record");

        state.back();

        assertSame(view, state.getView());
    }

    @Test
    void backWalksTheWholeStackInOrderWithEachScreensView() {
        state.goTo(Screen.CUSTOMER_SEARCH, null, "search");
        state.goTo(Screen.CUSTOMER_RECORD, "C-1", "record");
        state.goTo(Screen.LOAN, "L-9", "loan");

        state.back();
        assertEquals(Screen.CUSTOMER_RECORD, state.getScreen());
        assertEquals("record", state.getView());
        state.back();
        assertEquals("search", state.getView());
        state.back();
        assertEquals(Screen.LOGIN, state.getScreen());
        assertEquals("login", state.getView());
        assertFalse(state.canGoBack());
    }

    @Test
    void backWithNothingToGoBackToStaysPutAndSaysSo() {
        assertFalse(state.back());

        assertEquals(Screen.LOGIN, state.getScreen());
        assertEquals("login", state.getView());
    }

    @Test
    void goingToTheSameScreenAgainKeepsBothEntries() {
        state.goTo(Screen.CUSTOMER_RECORD, "C-1", "first");
        state.goTo(Screen.CUSTOMER_RECORD, "C-2", "second");

        assertEquals("second", state.getView());
        state.back();
        assertEquals("first", state.getView());
        assertEquals("C-1", state.getParameter());
    }

    @Test
    void resetToClearsTheBackStackAndDropsTheStoredViews() {
        state.goTo(Screen.CUSTOMER_SEARCH, null, "search");

        state.resetTo(Screen.CUSTOMER_SEARCH, null, "fresh search");

        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertEquals("fresh search", state.getView());
        assertFalse(state.canGoBack());
        assertFalse(state.back());
    }

    @Test
    void afterResetBackCannotReturnToTheLoginScreen() {
        state.resetTo(Screen.CUSTOMER_SEARCH, null, "search");
        state.goTo(Screen.CUSTOMER_RECORD, "C-1", "record");

        state.back();
        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertEquals("search", state.getView());
        assertFalse(state.back());
    }
}
