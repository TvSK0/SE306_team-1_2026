package lavs.logic;

import lavs.model.Customer;

// AI-assisted: US3, what the MMM typed into the customer details form, before it is
// checked. The field names are what CustomerService uses in its FieldErrors.
public final class CustomerInput {
    public static final String TITLE = "title";
    public static final String FIRST_NAME = "firstName";
    public static final String FAMILY_NAME = "familyName";
    public static final String DATE_OF_BIRTH = "dateOfBirth";
    public static final String NATIONALITY = "nationality";

    private final String _title;
    private final String _firstName;
    private final String _familyName;
    private final String _dateOfBirth;
    private final String _nationality;

    // dateOfBirth is as typed, e.g. "2001-02-03"
    public CustomerInput(String title, String firstName, String familyName, String dateOfBirth, String nationality) {
        _title = title;
        _firstName = firstName;
        _familyName = familyName;
        _dateOfBirth = dateOfBirth;
        _nationality = nationality;
    }

    // the form filled in from a saved customer; a missing value becomes an empty field
    public static CustomerInput fromCustomer(Customer customer) {
        return new CustomerInput(text(customer.getTitle()), text(customer.getFirstName()),
                text(customer.getFamilyName()),
                customer.getDateOfBirth() == null ? "" : customer.getDateOfBirth().toString(),
                text(customer.getNationality()));
    }

    public String getTitle() {
        return _title;
    }

    public String getFirstName() {
        return _firstName;
    }

    public String getFamilyName() {
        return _familyName;
    }

    public String getDateOfBirth() {
        return _dateOfBirth;
    }

    public String getNationality() {
        return _nationality;
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }
}
