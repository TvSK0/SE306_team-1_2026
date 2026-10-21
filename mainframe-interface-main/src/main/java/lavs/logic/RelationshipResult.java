// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.CustomerRelationship;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

// The outcome of adding, updating or deleting a relationship: field errors caught before
// the mainframe was contacted, or the Status it returned, plus the saved relationship when
// an add or update worked.
// TEMPORARY: swap this for Scott's gateway result type once it exists.
public final class RelationshipResult {
    private final ValidationResult _validationErrors;
    private final Status _mainframeStatus;
    private final CustomerRelationship _relationship;

    private RelationshipResult(ValidationResult validationErrors, Status mainframeStatus,
                               CustomerRelationship relationship) {
        _validationErrors = validationErrors;
        _mainframeStatus = mainframeStatus;
        _relationship = relationship;
    }

    static RelationshipResult invalid(ValidationResult validationErrors) {
        return new RelationshipResult(validationErrors, null, null);
    }

    static RelationshipResult fromStatus(Status status, CustomerRelationship relationship) {
        return new RelationshipResult(new ValidationResult(), status, relationship);
    }

    // True only when there were no field errors and the mainframe accepted the change.
    public boolean isSuccessful() {
        return _validationErrors.isValid() && _mainframeStatus.getWasSuccessful();
    }

    public ValidationResult getValidationErrors() {
        return _validationErrors;
    }

    // null when field errors stopped the change before the mainframe was contacted
    public Status getMainframeStatus() {
        return _mainframeStatus;
    }

    // the relationship as the mainframe saved it; null after a delete or a failure
    public CustomerRelationship getRelationship() {
        return _relationship;
    }
}
