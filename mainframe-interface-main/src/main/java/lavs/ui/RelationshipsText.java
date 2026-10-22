package lavs.ui;

import lavs.logic.RelationshipInput;
import lavs.logic.RelationshipResult;
import lavs.model.CustomerRelationship;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

// The text the Relationships view shows, kept free of JavaFX so it can be unit tested.
public final class RelationshipsText {
    private static final String NO_CONNECTION = "Couldn't reach the mainframe. Check your connection and try again.";

    private RelationshipsText() {}

    // "Spouse", "Legal authority", or "Other: Business partner"
    public static String type(CustomerRelationship relationship) {
        String type = relationship.getType();
        if (RelationshipInput.TYPE_OTHER.equals(type) && !isBlank(relationship.getOtherType())) {
            return type + ": " + relationship.getOtherType().trim();
        }
        // the mainframe's types are single words in CamelCase, e.g. "LegalAuthority"
        String words = type.replaceAll("([a-z])([A-Z])", "$1 $2");
        return words.charAt(0) + words.substring(1).toLowerCase();
    }

    // "Sam Smith (C-20456)", or just the number when the mainframe gave no name
    public static String otherCustomer(CustomerRelationship relationship) {
        String name = relationship.getOtherCustomerName();
        if (isBlank(name)) {
            return relationship.getOtherCustomerNumber();
        }
        return name.trim() + " (" + relationship.getOtherCustomerNumber() + ")";
    }

    // the message shown after Save or Remove; doneText is what worked, e.g. "Relationship added."
    public static String forResult(RelationshipResult result, String doneText) {
        if (result.isSuccessful()) {
            return doneText;
        }
        // AI-assisted: a save kept on the upload queue (US11)
        if (result.isWaitingToUpload()) {
            return doneText + " " + ResultText.WAITING_TO_UPLOAD;
        }
        Status status = result.getMainframeStatus();
        // no status means the field checks stopped the change before the mainframe was contacted
        if (status == null) {
            return "Please fix the highlighted fields.";
        }
        if (isNetworkFailure(status)) {
            return NO_CONNECTION;
        }
        return "The mainframe rejected the change: " + status.getErrorMessage();
    }

    // the message shown when the list could not be loaded
    public static String forLoadFailure(Status status) {
        if (isNetworkFailure(status)) {
            return NO_CONNECTION;
        }
        return "The relationships could not be loaded: " + status.getErrorMessage();
    }

    private static boolean isNetworkFailure(Status status) {
        return StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
