// AI-assisted: tests that only views implementing Refreshable are told Back returned to them.
package lavs.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RefreshableTests {
    @Test
    void aRefreshableViewIsToldOnceWhenBackReturnsToIt() {
        AtomicInteger calls = new AtomicInteger();
        Refreshable view = calls::incrementAndGet;

        Refreshable.notifyReturned(view);

        assertEquals(1, calls.get());
    }

    @Test
    void aViewThatIsNotRefreshableIsLeftAlone() {
        assertDoesNotThrow(() -> Refreshable.notifyReturned("a search view"));
    }

    @Test
    void nothingToNotifyIsFine() {
        assertDoesNotThrow(() -> Refreshable.notifyReturned(null));
    }
}
