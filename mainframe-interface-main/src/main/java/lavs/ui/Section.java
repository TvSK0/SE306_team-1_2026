// AI-assisted: the nine sections of a customer record, in the order of the sidebar (Figma 03a).
package lavs.ui;

import lavs.logic.CustomerCountsService;

// Teammates register the view for their section with CustomerRecordScreen's section registry
// (SectionRegistry.register). Until they do, the section shows a placeholder that names its
// owner. The owner is a note for the team, not behaviour: change it when a section is taken.
public enum Section {
    DETAILS("Details", false, "to be confirmed"),
    ADDRESSES("Addresses", true, "to be confirmed"),
    PHONE_NUMBERS("Phone numbers", true, "to be confirmed"),
    EMAILS("Emails", true, "to be confirmed"),
    EMPLOYERS("Employers", true, "to be confirmed"),
    RELATIONSHIPS("Relationships", true, "Yu"),
    NOTES("Notes", true, "to be confirmed"),
    DOCUMENTS("Documents", true, "Taran (optional component)"),
    LOANS("Loans", true, "Taran");

    private final String _label;
    private final boolean _hasCount;
    private final String _owner;

    Section(String label, boolean hasCount, String owner) {
        _label = label;
        _hasCount = hasCount;
        _owner = owner;
    }

    public String label() {
        return _label;
    }

    // Details is the customer's own fields, so there is nothing to count
    public boolean hasCount() {
        return _hasCount;
    }

    public String owner() {
        return _owner;
    }

    // what an unregistered section shows
    public String placeholderText() {
        return "Not built yet: " + _label + ". Owner: " + _owner + ".";
    }

    // the section whose sidebar count a CustomerCountsService answer is for
    public static Section forKind(CustomerCountsService.Kind kind) {
        return switch (kind) {
            case ADDRESSES -> ADDRESSES;
            case PHONE_NUMBERS -> PHONE_NUMBERS;
            case EMAILS -> EMAILS;
            case EMPLOYERS -> EMPLOYERS;
            case RELATIONSHIPS -> RELATIONSHIPS;
            case NOTES -> NOTES;
            case DOCUMENTS -> DOCUMENTS;
            case LOANS -> LOANS;
        };
    }
}
