// AI-assisted.
package lavs.logic;

import lavs.data.LocalSaveStatus;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// What the screens read about a mainframe answer instead of mainframe.client.Status.
class MainframeReplyTests {

    @Test
    void noStatusMeansTheMainframeWasNeverAsked() {
        MainframeReply reply = MainframeReply.of(null);

        assertFalse(reply.wasAnswered());
        assertFalse(reply.isQueued());
        assertFalse(reply.isNetworkFailure());
        assertNull(reply.getErrorMessage());
    }

    @Test
    void aRefusalCarriesTheMainframesReason() {
        MainframeReply reply = MainframeReply.of(new Status(42, "Unknown customer", 1));

        assertTrue(reply.wasAnswered());
        assertFalse(reply.isQueued());
        assertFalse(reply.isNetworkFailure());
        assertEquals("Unknown customer", reply.getErrorMessage());
    }

    @Test
    void anUnreachableMainframeIsANetworkFailure() {
        StatusMessage unavailable = StatusMessage.NETWORK_FAILURE_UNAVAILABLE;
        MainframeReply reply = MainframeReply.of(new Status(unavailable.getCode(), unavailable.getMessage(), 1));

        assertTrue(reply.isNetworkFailure());
    }

    @Test
    void aLocalSaveIsQueuedWithItsMessage() {
        MainframeReply waiting = MainframeReply.of(new Status(LocalSaveStatus.WAITING, "waiting", 1));
        MainframeReply paused = MainframeReply.of(new Status(LocalSaveStatus.PAUSED, "paused", 1));

        assertTrue(waiting.isQueued());
        assertEquals("Saved locally. Waiting to upload.", waiting.getQueuedMessage());
        assertTrue(paused.isQueued());
        assertEquals("Saved locally. Resolve the possible duplicate in Pending uploads.", paused.getQueuedMessage());
    }
}
