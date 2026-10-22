package lavs.logic;

import lavs.data.*;
import lavs.model.PendingChange;
import java.util.stream.Collectors;

// AI-assisted: recheck eligibility against current records immediately before uploading.
// Earlier queued addresses and contacts have already uploaded by this point.
public final class VerifiedUploads implements UploadGateway {
    private final MainframeGateway gateway;
    private final CustomerEligibilityService eligibility;
    public VerifiedUploads(MainframeGateway gateway) {
        this.gateway = gateway;
        eligibility = new CustomerEligibilityService(gateway);
    }
    @Override public boolean isConnected() { return gateway.isConnected(); }
    @Override public DuplicateCheckResult findExistingCustomers(PendingChange change) {
        return gateway.findExistingCustomers(change);
    }
    @Override public SendResult send(PendingChange change) {
        if ("loan.create".equals(change.getMessageType())) {
            var checked = eligibility.check(change.getFields().get("customerNumber"));
            if (!checked.isSuccessful()) {
                if (checked.getOutcome() == ServiceResult.Outcome.NETWORK_FAILURE) return SendResult.networkFailure(checked.getMessage());
                String reason = checked.getValidationErrors().isValid() ? checked.getMessage()
                        : checked.getValidationErrors().getErrors().stream().map(error -> error.getMessage()).collect(Collectors.joining("; "));
                return SendResult.rejected(reason);
            }
        }
        if ("loan.updateStatus".equals(change.getMessageType())) {
            try {
                var checked = new LoanStatusService(gateway).checkForVerification(change.getFields().get("loanNumber"));
                if (!checked.isSuccessful()) {
                    if (!checked.getValidationErrors().isValid()) return SendResult.rejected(
                            checked.getValidationErrors().getErrors().stream().map(error -> error.getMessage()).collect(Collectors.joining("; ")));
                    var result = GatewayResult.from(checked.getMainframeStatus());
                    return result.getOutcome() == GatewayResult.Outcome.NETWORK_FAILURE
                            ? SendResult.networkFailure(result.getMessage()) : SendResult.rejected(result.getMessage());
                }
            } catch (java.io.IOException error) { return SendResult.networkFailure(error.getMessage()); }
        }
        return gateway.send(change);
    }
}
