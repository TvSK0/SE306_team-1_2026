package lavs.ui;

import lavs.model.Address;
import lavs.model.Email;
import lavs.model.PhoneNumber;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class ContactTextTests {
    @Test
    void wordsTheFormForEachKind() {
        assertEquals("Add an address", ContactText.formTitle(ContactText.Kind.ADDRESS, false));
        assertEquals("Add a phone number", ContactText.formTitle(ContactText.Kind.PHONE_NUMBER, false));
        assertEquals("Edit email", ContactText.formTitle(ContactText.Kind.EMAIL, true));
        assertEquals("Add phone number", ContactText.saveButton(ContactText.Kind.PHONE_NUMBER, false));
        assertEquals("Save changes", ContactText.saveButton(ContactText.Kind.ADDRESS, true));
    }

    @Test
    void wordsWhatHappened() {
        assertEquals("Address added.", ContactText.saved(ContactText.Kind.ADDRESS, false));
        assertEquals("Phone number saved.", ContactText.saved(ContactText.Kind.PHONE_NUMBER, true));
        assertEquals("Email removed.", ContactText.removed(ContactText.Kind.EMAIL));
        assertEquals("Remove the email jane@example.com?", ContactText.confirmRemove(ContactText.Kind.EMAIL, "jane@example.com"));
    }

    @Test
    void wordsTheEmptyAndLoadingTable() {
        assertEquals("No addresses yet.", ContactText.noneYet(ContactText.Kind.ADDRESS));
        assertEquals("Loading phone numbers...", ContactText.loading(ContactText.Kind.PHONE_NUMBER));
        assertEquals("No emails loaded.", ContactText.notLoaded(ContactText.Kind.EMAIL));
        assertEquals("The addresses", ContactText.whatCouldNotLoad(ContactText.Kind.ADDRESS));
    }

    @Test
    void showsAnOtherTypesDescription() {
        assertEquals("Residential", ContactText.type("Residential", null));
        assertEquals("Other: Bach", ContactText.type("Other", " Bach "));
        assertEquals("Other", ContactText.type("Other", " "));
        assertEquals("", ContactText.type(null, null));
    }

    @Test
    void writesAnAddressOnOneLineLeavingOutWhatIsMissing() {
        Address full = new Address("C-1", "Residential", "1 Queen St", "Unit 2", "CBD", "Auckland", "1010",
                "New Zealand", true, true);
        Address sparse = new Address("C-1", "Postal", "PO Box 5", null, " ", "Wellington", null, "New Zealand",
                false, false);

        assertEquals("1 Queen St, Unit 2, CBD, Auckland 1010, New Zealand", ContactText.oneLine(full));
        assertEquals("PO Box 5, Wellington, New Zealand", ContactText.oneLine(sparse));
    }

    @Test
    void writesANumberWithItsCountryCode() {
        assertEquals("+64 21 123 4567", ContactText.number(new PhoneNumber("C-1", "Mobile", " +64 ", "21 123 4567", false, false)));
        assertEquals("09 123 4567", ContactText.number(new PhoneNumber("C-1", "Home", null, "09 123 4567", false, false)));
        assertEquals("09 123 4567", ContactText.number(new PhoneNumber("C-1", "Home", "", "09 123 4567", false, false)));
    }

    @Test
    void listsTheFlagsThatAreSet() {
        assertEquals("Primary, Mailing", ContactText.flags(new Address("C-1", "R", "1", null, null, "A", null, "NZ", true, true)));
        assertEquals("Mailing", ContactText.flags(new Address("C-1", "R", "1", null, null, "A", null, "NZ", false, true)));
        assertEquals("Primary, Can text", ContactText.flags(new PhoneNumber("C-1", "Mobile", "+64", "21", true, true)));
        assertEquals("", ContactText.flags(new PhoneNumber("C-1", "Home", "+64", "9", false, false)));
        assertEquals("Primary", ContactText.flags(new Email("C-1", "Personal", "a@b.co", true)));
        assertEquals("", ContactText.flags(new Email("C-1", "Work", "a@b.co", false)));
    }
}
