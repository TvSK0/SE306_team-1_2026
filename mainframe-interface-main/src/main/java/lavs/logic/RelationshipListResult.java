package lavs.logic;

import lavs.model.CustomerRelationship;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

import java.util.List;

// The outcome of listing a customer's relationships: field errors caught before the
// mainframe was contacted, or the Status it returned along with every relationship
// (empty unless successful).
// TEMPORARY: swap this for Scott's gateway result type once it exists.
public final class RelationshipListResult {
    private final ValidationResult _validationErrors;
    private final Status _mainframeStatus;
    private final List<CustomerRelationship> _relationships;

    private RelationshipListResult(ValidationResult validationErrors, Status mainframeStatus,
                                   List<CustomerRelationship> relationships) {
        _validationErrors = validationErrors;
        _mainframeStatus = mainframeStatus;
        _relationships = List.copyOf(relationships);
    }

    static RelationshipListResult invalid(ValidationResult validationErrors) {
        return new RelationshipListResult(validationErrors, null, List.of());
    }

    static RelationshipListResult fromStatus(Status status, List<CustomerRelationship> relationships) {
        return new RelationshipListResult(new ValidationResult(), status, relationships);
    }

    // True only when there were no field errors and the mainframe answered every request.
    public boolean isSuccessful() {
        return _validationErrors.isValid() && _mainframeStatus.getWasSuccessful();
    }

    public ValidationResult getValidationErrors() {
        return _validationErrors;
    }

    // null when field errors stopped the request before the mainframe was contacted
    public Status getMainframeStatus() {
        return _mainframeStatus;
    }

    // in the order the mainframe returned them; cannot be modified
    public List<CustomerRelationship> getRelationships() {
        return _relationships;
    }
}
