package lavs.logic;

import lavs.data.GatewayResult;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class ServiceResultTests {
    @Test
    void aSuccessCarriesItsValue() {
        ServiceResult<String> result = ServiceResult.from(GatewayResult.from(new Status(1)), "C-1");

        assertTrue(result.isSuccessful());
        assertEquals(ServiceResult.Outcome.SUCCESS, result.getOutcome());
        assertEquals("C-1", result.getValue());
        assertNull(result.getMessage());
        assertTrue(result.getMainframeStatus().getWasSuccessful());
        assertTrue(result.getValidationErrors().isValid());
    }

    @Test
    void aFailureDropsTheValueAndKeepsTheReason() {
        Status refused = new Status(StatusMessage.SERVER_FAILURE.getCode(), "no such customer", 1);

        ServiceResult<String> result = ServiceResult.from(GatewayResult.from(refused), "half-filled");

        assertFalse(result.isSuccessful());
        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertNull(result.getValue());
        assertEquals("no such customer", result.getMessage());
    }

    @Test
    void fieldErrorsMeanTheMainframeWasNeverAsked() {
        ValidationResult errors = new ValidationResult();
        errors.add(new FieldError("firstName", "is required"));

        ServiceResult<String> result = ServiceResult.invalid(errors);

        assertFalse(result.isSuccessful());
        assertFalse(result.isSaved());
        assertFalse(result.isWaitingToUpload());
        assertNull(result.getOutcome());
        assertNull(result.getMessage());
        assertNull(result.getMainframeStatus());
        assertNull(result.getValue());
        assertSame(errors, result.getValidationErrors());
    }

    @Test
    void aNetworkFailureIsNotSaved() {
        Status down = new Status(StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode(), "no route", 1);

        ServiceResult<String> result = ServiceResult.from(GatewayResult.from(down), "C-1");

        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        assertFalse(result.isSaved());
    }

    // --- saves that went through the upload queue (AI-assisted, US11) ---

    @Test
    void aQueuedSaveThatWasSentIsASuccessWithWhatWasSaved() {
        ServiceResult<String> result = ServiceResult.queued(new SyncStep(null, SyncStep.Outcome.SENT, null), "saved");

        assertTrue(result.isSuccessful());
        assertTrue(result.isSaved());
        assertFalse(result.isWaitingToUpload());
        assertEquals(ServiceResult.Outcome.SUCCESS, result.getOutcome());
        assertEquals("saved", result.getValue());
        assertNull(result.getMessage());
        assertNull(result.getMainframeStatus(), "the queue has no mainframe Status");
        assertTrue(result.getValidationErrors().isValid());
    }

    @Test
    void aQueuedSaveStillWaitingIsSavedButNotYetSuccessful() {
        ServiceResult<String> result = ServiceResult.queued(new SyncStep(null, SyncStep.Outcome.WAITING, "offline"),
                "saved");

        assertFalse(result.isSuccessful());
        assertTrue(result.isSaved());
        assertTrue(result.isWaitingToUpload());
        assertEquals(ServiceResult.Outcome.WAITING, result.getOutcome());
        assertEquals("saved", result.getValue(), "what was saved is kept while it waits");
        assertEquals("offline", result.getMessage());
    }

    @Test
    void aSaveHeldBehindAPossibleDuplicateCountsAsWaiting() {
        ServiceResult<String> result = ServiceResult.queued(
                new SyncStep(null, SyncStep.Outcome.PAUSED, "Possible existing customer: C-9"), "saved");

        assertEquals(ServiceResult.Outcome.WAITING, result.getOutcome());
        assertTrue(result.isSaved());
        assertTrue(result.isHeldForReview(), "the MMM, not the network, has to act");
    }

    @Test
    void aSaveWaitingForTheNetworkIsNotHeldForReview() {
        ServiceResult<String> result = ServiceResult.queued(new SyncStep(null, SyncStep.Outcome.WAITING, "offline"), "x");

        assertFalse(result.isHeldForReview());
    }

    @Test
    void aQueuedSaveTheMainframeRefusedDropsTheValue() {
        ServiceResult<String> result = ServiceResult.queued(
                new SyncStep(null, SyncStep.Outcome.REJECTED, "no such customer"), "saved");

        assertFalse(result.isSaved());
        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertNull(result.getValue());
        assertEquals("no such customer", result.getMessage());
    }
}
