package lavs.data;

import java.util.List;
import java.util.Map;

public final class ConstantsLoadResult {
    public enum Outcome { SUCCESS, NETWORK_FAILURE, REJECTED }

    private final Outcome _outcome;
    private final Map<String, List<String>> _values;
    private final String _message;

    private ConstantsLoadResult(Outcome outcome, Map<String, List<String>> values, String message) {
        _outcome = outcome;
        _values = values;
        _message = message;
    }

    public static ConstantsLoadResult success(Map<String, List<String>> values) {
        return new ConstantsLoadResult(Outcome.SUCCESS, Map.copyOf(values), null);
    }

    public static ConstantsLoadResult networkFailure(String message) {
        return new ConstantsLoadResult(Outcome.NETWORK_FAILURE, Map.of(), message);
    }

    public static ConstantsLoadResult rejected(String message) {
        return new ConstantsLoadResult(Outcome.REJECTED, Map.of(), message);
    }

    public Outcome getOutcome() {
        return _outcome;
    }

    // category name to its values in display order; empty unless the load succeeded
    public Map<String, List<String>> getValues() {
        return _values;
    }

    // the error message; null when the load succeeded
    public String getMessage() {
        return _message;
    }
}
