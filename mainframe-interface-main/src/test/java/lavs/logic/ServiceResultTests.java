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
        assertNull(result.getOutcome());
        assertNull(result.getMessage());
        assertNull(result.getMainframeStatus());
        assertNull(result.getValue());
        assertSame(errors, result.getValidationErrors());
    }
}
