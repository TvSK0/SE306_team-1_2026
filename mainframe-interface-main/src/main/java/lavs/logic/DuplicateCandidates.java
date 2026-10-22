package lavs.logic;

import lavs.model.PendingChange;
import java.util.List;

// AI-assisted: one format for the duplicate message, and the choices offered for a paused new
// customer. The choices come from PendingChange.getDuplicateMatches (stored in their own column),
// not from parsing the message text, so rewording the message can't break the panel.
public final class DuplicateCandidates {
    private static final String PREFIX = "Possible existing customer: ";
    private DuplicateCandidates() {}
    public static String message(List<String> numbers) { return PREFIX + String.join(", ", numbers); }
    public static List<String> forChange(PendingChange change) {
        if (!"customer.create".equals(change.getMessageType()) || change.getStatus() != PendingChange.Status.PAUSED) {
            return List.of();
        }
        return change.getDuplicateMatches().stream().distinct().toList();
    }
}
