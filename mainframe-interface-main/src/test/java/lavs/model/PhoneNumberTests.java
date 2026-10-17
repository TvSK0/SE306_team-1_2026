package lavs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class PhoneNumberTests {

    @Test
    void constructorSetsFields() {
        PhoneNumber phone = new PhoneNumber("C1", "Mobile", "+64", "211234567", true, true);

        assertAll(
                () -> assertEquals("C1", phone.getCustomerNumber()),
                () -> assertEquals("Mobile", phone.getType()),
                () -> assertEquals("+64", phone.getCountryCode()),
                () -> assertEquals("211234567", phone.getNumber()),
                () -> assertTrue(phone.isPrimary()),
                () -> assertTrue(phone.canTxt()),
                () -> assertNull(phone.getId()),
                () -> assertNull(phone.getOtherType()));
    }

    @Test
    void settersUpdateFields() {
        PhoneNumber phone = new PhoneNumber();
        phone.setId(3L);
        phone.setCustomerNumber("C2");
        phone.setType("Other");
        phone.setOtherType("Pager");
        phone.setCountryCode("+61");
        phone.setNumber("298765432");
        phone.setPrimary(false);
        phone.setCanTxt(false);

        assertAll(
                () -> assertEquals(3L, phone.getId()),
                () -> assertEquals("C2", phone.getCustomerNumber()),
                () -> assertEquals("Other", phone.getType()),
                () -> assertEquals("Pager", phone.getOtherType()),
                () -> assertEquals("+61", phone.getCountryCode()),
                () -> assertEquals("298765432", phone.getNumber()),
                () -> assertFalse(phone.isPrimary()),
                () -> assertFalse(phone.canTxt()));
    }

    @Test
    void isNewUntilIdAssigned() {
        PhoneNumber phone = new PhoneNumber();
        assertTrue(phone.isNew());

        phone.setId(1L);
        assertFalse(phone.isNew());
    }
}
