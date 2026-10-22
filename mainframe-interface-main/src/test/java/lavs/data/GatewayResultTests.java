package lavs.data;

import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class GatewayResultTests {
    @Test
    void aSuccessfulStatusIsASuccess() {
        GatewayResult result = GatewayResult.from(new Status(1));

        assertEquals(GatewayResult.Outcome.SUCCESS, result.getOutcome());
        assertTrue(result.isSuccess());
        assertNull(result.getMessage());
    }

    @Test
    void anUnreachableMainframeIsANetworkFailure() {
        GatewayResult result = GatewayResult.from(
                StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1).getStatus());

        assertEquals(GatewayResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertFalse(result.isSuccess());
    }

    @Test
    void anUnreadableReplyIsANetworkFailure() {
        GatewayResult result = GatewayResult.from(StatusMessage.UNABLE_TO_PARSE.generateMessageResponse(1).getStatus());

        assertEquals(GatewayResult.Outcome.NETWORK_FAILURE, result.getOutcome());
    }

    @ParameterizedTest
    @EnumSource(value = StatusMessage.class, names = {"SERVER_FAILURE", "INTERNAL_FAILURE", "NOT_IMPLEMENTED"})
    void anyOtherFailureIsARejection(StatusMessage failure) {
        GatewayResult result = GatewayResult.from(new Status(failure.getCode(), "nope", 1));

        assertEquals(GatewayResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("nope", result.getMessage());
    }

    @Test
    void aFailureWithoutAStatusIsANetworkFailureCarryingItsReason() {
        GatewayResult result = GatewayResult.networkFailure("timed out");

        assertEquals(GatewayResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertEquals("timed out", result.getMessage());
        assertEquals(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), result.getStatus().getErrorCode());
    }
}
