// AI-assisted: tests the top bar's status text and style, user text, title and visibility.
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TopBarStateTests {
    // a feed the tests can change, as the real one does
    private static final class MutableFeed implements StatusFeed {
        boolean online = true;
        int pending = 0;

        @Override
        public boolean isOnline() {
            return online;
        }

        @Override
        public int pendingCount() {
            return pending;
        }

        @Override
        public void onChange(Runnable listener) {
        }
    }

    private final MutableFeed feed = new MutableFeed();
    private final TopBarState state = new TopBarState(feed);

    @Test
    void onlineWithNothingWaiting() {
        assertEquals("online", state.getStatusStyle());
        assertEquals("Online", state.getStatusLabel());
        assertEquals("All changes uploaded", state.getStatusDetail());
    }

    @Test
    void onlineWithChangesWaiting() {
        feed.pending = 3;

        assertEquals("online", state.getStatusStyle());
        assertEquals("Online", state.getStatusLabel());
        assertEquals("3 waiting to upload", state.getStatusDetail());
    }

    @Test
    void offlineWithChangesWaiting() {
        feed.online = false;
        feed.pending = 2;

        assertEquals("offline", state.getStatusStyle());
        assertEquals("Offline", state.getStatusLabel());
        assertEquals("2 waiting to upload", state.getStatusDetail());
    }

    @Test
    void offlineWithNothingWaiting() {
        feed.online = false;

        assertEquals("offline", state.getStatusStyle());
        assertEquals("Offline", state.getStatusLabel());
        assertEquals("All changes uploaded", state.getStatusDetail());
    }

    @Test
    void oneChangeWaitingUsesTheSameWording() {
        feed.pending = 1;

        assertEquals("1 waiting to upload", state.getStatusDetail());
    }

    @Test
    void thePillFollowsTheFeedWhenItChanges() {
        assertEquals("Online", state.getStatusLabel());
        assertEquals("All changes uploaded", state.getStatusDetail());

        feed.online = false;
        feed.pending = 4;

        assertEquals("Offline", state.getStatusLabel());
        assertEquals("4 waiting to upload", state.getStatusDetail());
    }

    @Test
    void noUserShowsNothing() {
        assertEquals("", state.getUserText());
    }

    @Test
    void aBlankUserShowsNothing() {
        state.setUserName("   ");

        assertEquals("", state.getUserText());
    }

    @Test
    void aTwoWordNameIsShownAsInitialAndSurname() {
        state.setUserName("Jane Smith");

        assertEquals("Signed in: J. Smith", state.getUserText());
    }

    @Test
    void aLongerNameUsesTheFirstInitialAndTheLastWord() {
        state.setUserName("Jane Mary Smith");

        assertEquals("Signed in: J. Smith", state.getUserText());
    }

    @Test
    void aLowerCaseFirstNameGetsACapitalInitial() {
        state.setUserName("jane smith");

        assertEquals("Signed in: J. smith", state.getUserText());
    }

    @Test
    void extraSpacesAroundAndBetweenWordsAreIgnored() {
        state.setUserName("  Jane \t  Smith ");

        assertEquals("Signed in: J. Smith", state.getUserText());
    }

    @Test
    void aSingleWordNameIsShownAsIs() {
        state.setUserName("jsmith");

        assertEquals("Signed in: jsmith", state.getUserText());
    }

    @Test
    void signingOutClearsTheUser() {
        state.setUserName("Jane Smith");
        state.setUserName(null);

        assertEquals("", state.getUserText());
    }

    @Test
    void theTitleStartsEmptyAndCanBeSet() {
        assertEquals("", state.getTitle());

        state.setTitle("Customer: Jane Smith (C-10234)");

        assertEquals("Customer: Jane Smith (C-10234)", state.getTitle());
    }

    @Test
    void everyScreenHasADefaultTitle() {
        assertEquals("", TopBarState.defaultTitleFor(Screen.LOGIN));
        assertEquals("Customer search", TopBarState.defaultTitleFor(Screen.CUSTOMER_SEARCH));
        assertEquals("Customer", TopBarState.defaultTitleFor(Screen.CUSTOMER_RECORD));
        assertEquals("Loan", TopBarState.defaultTitleFor(Screen.LOAN));
        assertEquals("Select customer", TopBarState.defaultTitleFor(Screen.CUSTOMER_SELECT));
    }

    // US2-S1
    @Test
    void theTitleComesFromAParameterThatNamesOne() {
        TitledParameter titled = () -> "Loan L-5521 · Adding a co-borrower or guarantor";

        assertEquals("Loan L-5521 · Adding a co-borrower or guarantor",
                TopBarState.titleFor(Screen.CUSTOMER_SELECT, titled));
    }

    // US2-S1
    @Test
    void theDefaultTitleIsUsedWhenTheParameterNamesNone() {
        assertEquals("Select customer", TopBarState.titleFor(Screen.CUSTOMER_SELECT, (TitledParameter) () -> null));
        assertEquals("Select customer", TopBarState.titleFor(Screen.CUSTOMER_SELECT, (TitledParameter) () -> "  "));
    }

    @Test
    void theDefaultTitleIsUsedForAParameterThatIsNotTitled() {
        assertEquals("Customer", TopBarState.titleFor(Screen.CUSTOMER_RECORD, "C-10234"));
        assertEquals("Customer", TopBarState.titleFor(Screen.CUSTOMER_RECORD, null));
    }

    @Test
    void theTopBarIsHiddenOnLoginAndShownEverywhereElse() {
        assertFalse(TopBarState.isShownOn(Screen.LOGIN));
        assertTrue(TopBarState.isShownOn(Screen.CUSTOMER_SEARCH));
        assertTrue(TopBarState.isShownOn(Screen.CUSTOMER_RECORD));
        assertTrue(TopBarState.isShownOn(Screen.LOAN));
        assertTrue(TopBarState.isShownOn(Screen.CUSTOMER_SELECT));
    }
}
