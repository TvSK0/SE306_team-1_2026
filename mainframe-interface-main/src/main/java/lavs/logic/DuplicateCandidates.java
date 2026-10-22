package lavs.logic;

import lavs.model.PendingChange;
import java.util.Arrays;
import java.util.List;

// AI-assisted: one format for the persisted duplicate decision and its allowed choices.
public final class DuplicateCandidates {
    private static final String PREFIX = "Possible existing customer: ";
    private DuplicateCandidates() {}
    public static String message(List<String> numbers) { return PREFIX + String.join(", ", numbers); }
    public static List<String> forChange(PendingChange change) {
        if (!"customer.create".equals(change.getMessageType()) || change.getStatus() != PendingChange.Status.PAUSED
                || change.getStatusMessage() == null || !change.getStatusMessage().startsWith(PREFIX)) return List.of();
        return Arrays.stream(change.getStatusMessage().substring(PREFIX.length()).split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).distinct().toList();
    }
}
