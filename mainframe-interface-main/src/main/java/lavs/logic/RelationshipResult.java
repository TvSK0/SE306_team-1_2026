package lavs.logic;

import lavs.model.CustomerRelationship;
import lavs.validation.ValidationResult;
import mainframe.client.Status;

// The outcome of adding, updating or deleting a relationship: field errors caught before
// the mainframe was contacted, or the Status it returned, plus the saved relationship when
// an add or update worked.
// AI-assisted: a save goes through the upload queue (US11), so it can also be waiting to upload.
public final class RelationshipResult {
    private final ValidationResult _validationErrors;
    private final Status _mainframeStatus;
    private final CustomerRelationship _relationship;
    private final boolean _waiting;

    private RelationshipResult(ValidationResult validationErrors, Status mainframeStatus,
                               CustomerRelationship relationship, boolean waiting) {
        _validationErrors = validationErrors;
        _mainframeStatus = mainframeStatus;
        _relationship = relationship;
        _waiting = waiting;
    }

    static RelationshipResult invalid(ValidationResult validationErrors) {
        return new RelationshipResult(validationErrors, null, null, false);
    }

    static RelationshipResult fromStatus(Status status, CustomerRelationship relationship) {
        return new RelationshipResult(new ValidationResult(), status, relationship, false);
    }

    // AI-assisted: a save that went through the upload queue; relationship is what was saved
    static RelationshipResult queued(SyncStep step, CustomerRelationship relationship) {
        return new RelationshipResult(new ValidationResult(), QueuedWrites.statusFor(step),
                QueuedWrites.keptValue(step, relationship), QueuedWrites.isWaiting(step));
    }

    // True only when there were no field errors and the mainframe accepted the change.
    public boolean isSuccessful() {
        return _validationErrors.isValid() && _mainframeStatus.getWasSuccessful();
    }

    // AI-assisted: true when the change is kept on this laptop to upload once the mainframe can be reached
    public boolean isWaitingToUpload() {
        return _waiting;
    }

    // AI-assisted: true when the change is safe: on the mainframe, or on the upload queue
    public boolean isSaved() {
        return _waiting || isSuccessful();
    }

    public ValidationResult getValidationErrors() {
        return _validationErrors;
    }

    // null when field errors stopped the change before the mainframe was contacted
    public Status getMainframeStatus() {
        return _mainframeStatus;
    }

    // the relationship as it was saved; null after a delete or a failure
    public CustomerRelationship getRelationship() {
        return _relationship;
    }
}
