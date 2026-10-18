package lavs.data;
import java.util.List;

// AI-assisted.

// This is likely temporary, may move into Scott's gateway result types once his part's on main.
// Result of searching the mainframe for an existing customer before uploading a new one
public final class DuplicateCheckResult {

    public enum Outcome { NONE, FOUND, NETWORK_FAILURE }

    private final Outcome outcome;
    private final List<String> matchingNumbers;
    private final String message;

    private DuplicateCheckResult(Outcome outcome, List<String> matchingNumbers, String message) {
        this.outcome = outcome;
        this.matchingNumbers = matchingNumbers;
        this.message = message;
    }

    public static DuplicateCheckResult none() {
        return new DuplicateCheckResult(Outcome.NONE, List.of(), null);
    }

    public static DuplicateCheckResult found(List<String> matchingNumbers) {
        return new DuplicateCheckResult(Outcome.FOUND, List.copyOf(matchingNumbers), null);
    }

    public static DuplicateCheckResult networkFailure(String message) {
        return new DuplicateCheckResult(Outcome.NETWORK_FAILURE, List.of(), message);
    }

    public Outcome getOutcome() {
        return outcome;
    }

    // Customer numbers that look like the same person (eg same name and dob), empty unless found
    public List<String> getMatchingNumbers() {
        return matchingNumbers;
    }

    // Error message, is null unless the search failed.
    public String getMessage() {
        return message;
    }
}
