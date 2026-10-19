// AI-assisted: tests the navigation rules (current screen, parameters, stored views, Back, reset).
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NavigatorStateTests {
    private final NavigatorState<String> state = new NavigatorState<>(Screen.LOGIN, null);

    @Test
    void startsOnTheFirstScreenWithNoViewAndNothingToGoBackTo() {
        assertEquals(Screen.LOGIN, state.getScreen());
        assertNull(state.getParameter());
        assertNull(state.getView());
        assertFalse(state.canGoBack());
    }

    @Test
    void setViewStoresTheViewForTheCurrentScreen() {
        state.setView("login view");

        assertEquals("login view", state.getView());
        assertEquals(Screen.LOGIN, state.getScreen());
    }

    @Test
    void goToShowsTheScreenWithItsParameterAndNoViewYet() {
        state.goTo(Screen.CUSTOMER_RECORD, "C-10234");

        assertEquals(Screen.CUSTOMER_RECORD, state.getScreen());
        assertEquals("C-10234", state.getParameter());
        assertNull(state.getView());
        assertTrue(state.canGoBack());
    }

    @Test
    void backReturnsToThePreviousScreenWithItsParameterAndTheSameView() {
        state.resetTo(Screen.CUSTOMER_SEARCH, "jane");
        state.setView("search view");
        state.goTo(Screen.CUSTOMER_RECORD, "C-10234");
        state.setView("record view");

        assertTrue(state.back());

        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertEquals("jane", state.getParameter());
        assertEquals("search view", state.getView());
    }

    @Test
    void theStoredViewIsTheSameObjectNotACopy() {
        String view = new String("search view");
        state.setView(view);
        state.goTo(Screen.CUSTOMER_RECORD, null);

        state.back();

        assertSame(view, state.getView());
    }

    @Test
    void backWalksTheWholeStackInOrderWithEachScreensView() {
        state.setView("login");
        state.goTo(Screen.CUSTOMER_SEARCH, null);
        state.setView("search");
        state.goTo(Screen.CUSTOMER_RECORD, "C-1");
        state.setView("record");
        state.goTo(Screen.LOAN, "L-9");
        state.setView("loan");

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
        state.setView("login");

        assertFalse(state.back());

        assertEquals(Screen.LOGIN, state.getScreen());
        assertEquals("login", state.getView());
    }

    @Test
    void goingToTheSameScreenAgainBuildsAFreshEntry() {
        state.goTo(Screen.CUSTOMER_RECORD, "C-1");
        state.setView("first");
        state.goTo(Screen.CUSTOMER_RECORD, "C-2");
        state.setView("second");

        assertEquals("second", state.getView());
        state.back();
        assertEquals("first", state.getView());
        assertEquals("C-1", state.getParameter());
    }

    @Test
    void resetToClearsTheBackStackAndDropsTheStoredViews() {
        state.setView("login");
        state.goTo(Screen.CUSTOMER_SEARCH, null);
        state.setView("search");

        state.resetTo(Screen.CUSTOMER_SEARCH, null);

        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertNull(state.getView());
        assertFalse(state.canGoBack());
        assertFalse(state.back());
    }

    @Test
    void afterResetBackCannotReturnToTheLoginScreen() {
        state.resetTo(Screen.CUSTOMER_SEARCH, null);
        state.setView("search");
        state.goTo(Screen.CUSTOMER_RECORD, "C-1");

        state.back();
        assertEquals(Screen.CUSTOMER_SEARCH, state.getScreen());
        assertEquals("search", state.getView());
        assertFalse(state.back());
    }
}
