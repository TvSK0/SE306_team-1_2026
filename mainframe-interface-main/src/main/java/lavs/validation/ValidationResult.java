package lavs.validation;

import java.util.ArrayList;
import java.util.List;

// AI-assisted: the outcome of validating a whole form - zero or more FieldErrors,
// one per invalid field. Build one with the no-arg constructor and add() as each
// field is checked, or combine the results of several checks with merge().
public final class ValidationResult {
    private final List<FieldError> errors = new ArrayList<>();

    // true if every field passed
    public boolean isValid() {
        return errors.isEmpty();
    }

    // every failure, in the order they were added
    public List<FieldError> getErrors() {
        return List.copyOf(errors);
    }

    public void add(FieldError error) {
        errors.add(error);
    }

    // records a failure only if the validator actually returned one
    public void addIfPresent(java.util.Optional<FieldError> error) {
        error.ifPresent(this::add);
    }

    // folds another result's errors into this one, e.g. when validating a
    // request made up of several sub-sections
    public void merge(ValidationResult other) {
        errors.addAll(other.errors);
    }
}
