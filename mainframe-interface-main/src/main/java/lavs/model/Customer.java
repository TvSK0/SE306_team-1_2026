package lavs.model;

import java.time.LocalDate;

// A bank customer, held by the mainframe customer.* messages. The number is automatically assigned by the mainframe
// or a temporary number while offline/poor connection.
public class Customer {

    private String number;
    private String title;
    private String firstName;
    private String familyName;
    private LocalDate dateOfBirth;
    private String nationality;
    private String status;
    private long organisation;

    public Customer() {
    }

    public Customer(String title, String firstName, String familyName,
                    LocalDate dateOfBirth, String nationality, long organisation) {
        this.title = title;
        this.firstName = firstName;
        this.familyName = familyName;
        this.dateOfBirth = dateOfBirth;
        this.nationality = nationality;
        this.organisation = organisation;
    }

    // Joins the customer's first name and family name with a space in between.
    public String getFullName() {
        StringBuilder name = new StringBuilder();
        appendPart(name, firstName);
        appendPart(name, familyName);
        return name.toString();
    }

    private static void appendPart(StringBuilder name, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (name.length() > 0) {
            name.append(' ');
        }
        name.append(part.trim());
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getOrganisation() {
        return organisation;
    }

    public void setOrganisation(long organisation) {
        this.organisation = organisation;
    }
}
