package lavs.logic;

import lavs.data.GatewayResult;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

// AI-assisted: the outcome of a service call built on the MainframeGateway. Either field
// errors caught before the mainframe was contacted, or the gateway's result (success,
// network failure or rejected) with the value it produced. New services return this
// instead of a result class each.
//
// AI-assisted: a save that goes through the upload queue (US11) is built with queued() and
// can also be WAITING: kept on this laptop and sent once the mainframe is reachable. A
// queued result has no mainframe Status, so the outcome is kept here rather than read off a
// GatewayResult.
public final class ServiceResult<T> {
    // the gateway's three outcomes, repeated here so a screen can check them without
    // importing the data layer, plus WAITING for a save still on the upload queue
    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED, WAITING }

    private final ValidationResult validationErrors;
    private final Outcome outcome;
    private final String message;
    private final Status mainframeStatus;
    private final T value;
    private final boolean heldForReview;

    private ServiceResult(ValidationResult validationErrors, Outcome outcome, String message, Status mainframeStatus,
                          T value, boolean heldForReview) {
        this.validationErrors = validationErrors;
        this.outcome = outcome;
        this.message = message;
        this.mainframeStatus = mainframeStatus;
        this.value = value;
        this.heldForReview = heldForReview;
    }

    static <T> ServiceResult<T> invalid(ValidationResult validationErrors) {
        return new ServiceResult<>(validationErrors, null, null, null, null, false);
    }

    // value is what a successful call produced; it is dropped for any other outcome
    static <T> ServiceResult<T> from(GatewayResult gatewayResult, T value) {
        return new ServiceResult<>(new ValidationResult(), Outcome.valueOf(gatewayResult.getOutcome().name()),
                gatewayResult.getMessage(), gatewayResult.getStatus(), gatewayResult.isSuccess() ? value : null, false);
    }

    // AI-assisted: a save that went through the upload queue. value is what was saved, built
    // from the form: it is kept when the save was sent or is waiting to upload, and dropped when
    // it was rejected. PAUSED (held behind a possible-duplicate customer) is still saved and
    // will upload once the MMM decides, so it counts as WAITING.
    static <T> ServiceResult<T> queued(SyncStep step, T value) {
        switch (step.getOutcome()) {
            case SENT:
                return new ServiceResult<>(new ValidationResult(), Outcome.SUCCESS, null, null, value, false);
            case REJECTED:
                return new ServiceResult<>(new ValidationResult(), Outcome.REJECTED, step.getMessage(), null, null, false);
            default:
                return new ServiceResult<>(new ValidationResult(), Outcome.WAITING, step.getMessage(), null, value,
                        step.getOutcome() == SyncStep.Outcome.PAUSED);
        }
    }

    // AI-assisted: true when a save waits for the MMM, not the network: a new customer who may
    // already be on the mainframe, held until the MMM picks the existing one or confirms a new one
    public boolean isHeldForReview() {
        return heldForReview;
    }

    // true only when there were no field errors and the mainframe accepted the request
    public boolean isSuccessful() {
        return outcome == Outcome.SUCCESS;
    }

    // AI-assisted: true when the change is safe: accepted by the mainframe, or kept on the
    // upload queue to send later. A screen moves on when this is true.
    public boolean isSaved() {
        return outcome == Outcome.SUCCESS || outcome == Outcome.WAITING;
    }

    // AI-assisted: true when the change is kept on this laptop and not on the mainframe yet
    public boolean isWaitingToUpload() {
        return outcome == Outcome.WAITING;
    }

    public ValidationResult getValidationErrors() {
        return validationErrors;
    }

    // SUCCESS, NETWORK_FAILURE, REJECTED or WAITING; null when field errors stopped the call first
    public Outcome getOutcome() {
        return outcome;
    }

    // why the mainframe or the network said no, or why a save is still waiting; null on
    // success or when field errors stopped it
    public String getMessage() {
        return message;
    }

    // the Status underneath, for screens that already word their messages from one; null
    // when field errors stopped the call, and for a save that went through the upload queue
    public Status getMainframeStatus() {
        return mainframeStatus;
    }

    // what the call produced, e.g. the saved customer; null unless saved
    public T getValue() {
        return value;
    }
}
