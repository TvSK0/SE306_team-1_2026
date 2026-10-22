package lavs.logic;

import lavs.data.GatewayResult;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

// AI-assisted: the outcome of a service call built on the MainframeGateway. Either field
// errors caught before the mainframe was contacted, or the gateway's result (success,
// network failure or rejected) with the value it produced. New services return this
// instead of a result class each.
public final class ServiceResult<T> {
    // the gateway's three outcomes, repeated here so a screen can check them without
    // importing the data layer
    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED, WAITING, PAUSED }

    private final ValidationResult validationErrors;
    private final GatewayResult gatewayResult;
    private final T value;

    private ServiceResult(ValidationResult validationErrors, GatewayResult gatewayResult, T value) {
        this.validationErrors = validationErrors;
        this.gatewayResult = gatewayResult;
        this.value = value;
    }

    static <T> ServiceResult<T> invalid(ValidationResult validationErrors) {
        return new ServiceResult<>(validationErrors, null, null);
    }

    // value is what a successful call produced; it is dropped for any other outcome
    static <T> ServiceResult<T> from(GatewayResult gatewayResult, T value) {
        return new ServiceResult<>(new ValidationResult(), gatewayResult, (gatewayResult.isSuccess() || gatewayResult.isQueued()) ? value : null);
    }

    // true only when there were no field errors and the mainframe accepted the request
    public boolean isQueued() { return gatewayResult != null && gatewayResult.isQueued(); }

    public boolean isSuccessful() {
        return gatewayResult != null && gatewayResult.isSuccess();
    }

    public ValidationResult getValidationErrors() {
        return validationErrors;
    }

    // SUCCESS, NETWORK_FAILURE or REJECTED; null when field errors stopped the call first
    public Outcome getOutcome() {
        return gatewayResult == null ? null : Outcome.valueOf(gatewayResult.getOutcome().name());
    }

    // why the mainframe or the network said no; null on success or when field errors stopped it
    public String getMessage() {
        return gatewayResult == null ? null : gatewayResult.getMessage();
    }

    // the Status underneath, for screens that already word their messages from one; null
    // when field errors stopped the call
    public Status getMainframeStatus() {
        return gatewayResult == null ? null : gatewayResult.getStatus();
    }

    // AI-assisted: the same answer for the screens, which don't use mainframe.client.Status
    public MainframeReply getReply() {
        return MainframeReply.of(getMainframeStatus());
    }

    // what the call produced, e.g. the saved customer; null unless successful
    public T getValue() {
        return value;
    }
}
