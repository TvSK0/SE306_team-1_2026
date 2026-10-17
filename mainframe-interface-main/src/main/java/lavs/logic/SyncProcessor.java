package lavs.logic;

import lavs.data.ChangeQueue;
import lavs.data.DuplicateCheckResult;
import lavs.data.SendResult;
import lavs.data.UploadGateway;
import lavs.model.PendingChange;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Sync rules (no database or threads used, so can unit test):
// - Changes are sent oldest first
// - If the network's down, stop and try again later
// - If the mainframe rejects a change, mark it and never retry it (anything that depends on it is rejected too)
// - Once a new customer or loan is uploaded, swap its temp number for the real one in every queued change
// - Before uploading a new customer, check if they already exist. If they might, pause their changes until the MMM decides

public class SyncProcessor {

    static final String CREATE_CUSTOMER = "customer.create";

    private final ChangeQueue queue;
    private final UploadGateway gateway;
    private final Map<String, String> knownNumbers = new ConcurrentHashMap<>();

    public SyncProcessor(ChangeQueue queue, UploadGateway gateway) {
        this.queue = queue;
        this.gateway = gateway;
    }

    // Handles the oldest pending change, is empty when nothing's waiting to be sent.
    public Optional<SyncStep> processNext() {
        Optional<PendingChange> next = queue.next();
        if (next.isEmpty()) {
            return Optional.empty();
        }
        PendingChange change = next.get();
        if (needsDuplicateCheck(change)) {
            DuplicateCheckResult search = gateway.findExistingCustomers(change);
            if (search.getOutcome() == DuplicateCheckResult.Outcome.NETWORK_FAILURE) {
                return Optional.of(new SyncStep(change, SyncStep.Outcome.WAITING, search.getMessage()));
            }
            if (search.getOutcome() == DuplicateCheckResult.Outcome.FOUND) {
                String message = "Possible existing customer: " + String.join(", ", search.getMatchingNumbers());
                setStatusOfPending(change.getCreatesNumber(), PendingChange.Status.PAUSED, message);
                return Optional.of(new SyncStep(change, SyncStep.Outcome.PAUSED, message));
            }
        }
        return Optional.of(send(change));
    }

    private SyncStep send(PendingChange change) {
        SendResult result = gateway.send(change);
        switch (result.getOutcome()) {
            case SUCCESS:
                queue.delete(change.getId());
                if (change.getCreatesNumber() != null && result.getAssignedNumber() != null) {
                    swapNumber(change.getCreatesNumber(), result.getAssignedNumber());
                }
                return new SyncStep(change, SyncStep.Outcome.SENT, result.getAssignedNumber());
            case NETWORK_FAILURE:
                return new SyncStep(change, SyncStep.Outcome.WAITING, result.getMessage());
            default:
                queue.markRejected(change.getId(), result.getMessage());
                if (change.getCreatesNumber() != null) {
                    setStatusOfPending(change.getCreatesNumber(), PendingChange.Status.REJECTED,
                            "Not sent because change " + change.getId() + " was rejected");
                }
                return new SyncStep(change, SyncStep.Outcome.REJECTED, result.getMessage());
        }
    }

    // The MMM chose the existing customer, will drop the queued create and point everything else at it.
    public void useExistingCustomer(String temporaryNumber, String existingNumber) {
        for (PendingChange change : queue.all()) {
            if (!change.references(temporaryNumber)) {
                continue;
            }
            if (isCreateOf(change, temporaryNumber)) {
                queue.delete(change.getId());
            } else {
                change.replaceNumber(temporaryNumber, existingNumber);
                resumeIfPaused(change);
                queue.update(change);
            }
        }
        knownNumbers.put(temporaryNumber, existingNumber);
    }

    // The MMM confirmed it as a new customer, so upload without searching again.
    public void confirmNewCustomer(String temporaryNumber) {
        for (PendingChange change : queue.all()) {
            if (change.references(temporaryNumber) && change.getStatus() == PendingChange.Status.PAUSED) {
                if (isCreateOf(change, temporaryNumber)) {
                    change.setSkipDuplicateCheck(true);
                }
                resumeIfPaused(change);
                queue.update(change);
            }
        }
    }

    // Gets a change ready to be queued by swapping any temp number that's already been replaced
    // and if it depends on a new record that's paused or rejected, it's given the same status so
    // it's not sent with a temporary number.
    public void prepareNew(PendingChange change) {
        knownNumbers.forEach((temporary, real) -> {
            if (change.references(temporary)) {
                change.replaceNumber(temporary, real);
            }
        });
        for (PendingChange other : queue.all()) {
            String created = other.getCreatesNumber();
            if (created != null && other.getStatus() != PendingChange.Status.PENDING && change.references(created)) {
                change.setStatus(other.getStatus());
                change.setStatusMessage(other.getStatus() == PendingChange.Status.PAUSED
                        ? other.getStatusMessage()
                        : "Not sent because change " + other.getId() + " was rejected");
            }
        }
    }

    // The real number for a temporary one once it's known, otherwise returns the number unchanged
    public String resolveNumber(String number) {
        return knownNumbers.getOrDefault(number, number);
    }

    private void swapNumber(String temporaryNumber, String realNumber) {
        knownNumbers.put(temporaryNumber, realNumber);
        for (PendingChange change : queue.all()) {
            if (change.references(temporaryNumber)) {
                change.replaceNumber(temporaryNumber, realNumber);
                queue.update(change);
            }
        }
    }

    private void setStatusOfPending(String number, PendingChange.Status status, String message) {
        for (PendingChange change : queue.all()) {
            if (change.references(number) && change.getStatus() == PendingChange.Status.PENDING) {
                change.setStatus(status);
                change.setStatusMessage(message);
                queue.update(change);
            }
        }
    }

    private static void resumeIfPaused(PendingChange change) {
        if (change.getStatus() == PendingChange.Status.PAUSED) {
            change.setStatus(PendingChange.Status.PENDING);
            change.setStatusMessage(null);
        }
    }

    private static boolean needsDuplicateCheck(PendingChange change) {
        return CREATE_CUSTOMER.equals(change.getMessageType())
                && TemporaryNumbers.isTemporary(change.getCreatesNumber())
                && !change.isSkipDuplicateCheck();
    }

    private static boolean isCreateOf(PendingChange change, String number) {
        return CREATE_CUSTOMER.equals(change.getMessageType()) && number.equals(change.getCreatesNumber());
    }
}
