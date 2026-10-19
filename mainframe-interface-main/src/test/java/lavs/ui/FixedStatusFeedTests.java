// AI-assisted: tests the TEMPORARY fixed status feed.
package lavs.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStatusFeedTests {
    @Test
    void reportsTheValuesItWasGiven() {
        StatusFeed online = new FixedStatusFeed(true, 0);
        StatusFeed offline = new FixedStatusFeed(false, 5);

        assertTrue(online.isOnline());
        assertEquals(0, online.pendingCount());
        assertFalse(offline.isOnline());
        assertEquals(5, offline.pendingCount());
    }

    @Test
    void neverCallsItsListeners() {
        AtomicInteger calls = new AtomicInteger();

        new FixedStatusFeed(true, 0).onChange(calls::incrementAndGet);

        assertEquals(0, calls.get());
    }
}
