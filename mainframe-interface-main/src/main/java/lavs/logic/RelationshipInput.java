package lavs.logic;

// What the MMM typed into the relationship form, before it is checked. The field
// names are what CustomerRelationshipService uses in its FieldErrors.
public final class RelationshipInput {
    public static final String OTHER_CUSTOMER_NUMBER = "otherCustomerNumber";
    public static final String TYPE = "type";
    public static final String OTHER_TYPE = "otherType";

    // the relationship type that needs otherType to say what it is
    public static final String TYPE_OTHER = mainframe.client.messages.customer.AddRelationship.TYPE_OTHER;

    private final String _otherCustomerNumber;
    private final String _type;
    private final String _otherType;

    public RelationshipInput(String otherCustomerNumber, String type, String otherType) {
        _otherCustomerNumber = otherCustomerNumber;
        _type = type;
        _otherType = otherType;
    }

    public String getOtherCustomerNumber() {
        return _otherCustomerNumber;
    }

    public String getType() {
        return _type;
    }

    public String getOtherType() {
        return _otherType;
    }
}
