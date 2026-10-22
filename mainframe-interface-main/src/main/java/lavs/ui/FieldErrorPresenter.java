// AI-assisted: shows a ValidationResult on the right fields and works out the validation banner.
package lavs.ui;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// A FieldError's fieldName is how a screen finds the field to highlight. A screen builds one
// presenter with its fields (keyed by those names), then calls present(...) with each
// validation result. Errors for a name the screen has no field for are not lost: they come
// back in the Result so the screen can show them in the banner.
public final class FieldErrorPresenter {
    private static final String BODY_START = "Everything else you entered has been kept. Fix the highlighted ";
    private static final String BODY_END = " and save again.";

    private final Map<String, FieldView> _fields;

    public FieldErrorPresenter(Map<String, ? extends FieldView> fields) {
        _fields = Map.copyOf(fields);
    }

    // Clears every field, then flags the ones the result names. A field with several errors
    // shows the first.
    public Result present(ValidationResult validation) {
        clear();
        Map<String, FieldError> firstPerField = new LinkedHashMap<>();
        validation.getErrors().forEach(error -> firstPerField.putIfAbsent(error.getFieldName(), error));

        List<FieldError> unmatched = new ArrayList<>();
        firstPerField.forEach((name, error) -> {
            FieldView field = _fields.get(name);
            if (field == null) {
                unmatched.add(error);
            } else {
                field.setError(error.getMessage());
            }
        });
        return new Result(firstPerField.size(), unmatched);
    }

    public void clear() {
        _fields.values().forEach(FieldView::clearError);
    }

    // What present(...) found, and the banner text that goes with it (Figma frame 07).
    public static final class Result {
        private final int _fieldCount;
        private final List<FieldError> _unmatched;

        private Result(int fieldCount, List<FieldError> unmatched) {
            _fieldCount = fieldCount;
            _unmatched = List.copyOf(unmatched);
        }

        // true when the screen should show the validation banner
        public boolean hasErrors() {
            return _fieldCount > 0;
        }

        // errors whose field name the screen has no field for
        public List<FieldError> getUnmatched() {
            return _unmatched;
        }

        // "3 fields need attention" (or "1 field needs attention")
        public String getBannerTitle() {
            return _fieldCount == 1 ? "1 field needs attention" : _fieldCount + " fields need attention";
        }

        // the standard sentence (field or fields to match the title), plus the messages that
        // had no field to sit under, e.g. "... Also: Add a mailing address."
        public String getBannerBody() {
            String body = BODY_START + (_fieldCount == 1 ? "field" : "fields") + BODY_END;
            if (_unmatched.isEmpty()) {
                return body;
            }
            String also = _unmatched.stream().map(FieldError::getMessage).collect(Collectors.joining("; "));
            return body + " Also: " + also + (also.endsWith(".") ? "" : ".");
        }
    }
}
