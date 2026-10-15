package lavs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class AddressTests {

    @Test
    void constructorSetsFields() {
        Address address = new Address("C1", "Residential", "1 Queen St", "Unit 2", "CBD",
                "Auckland", "1010", "NZ", true, false);

        assertAll(
                () -> assertEquals("C1", address.getCustomerNumber()),
                () -> assertEquals("Residential", address.getType()),
                () -> assertEquals("1 Queen St", address.getLine1()),
                () -> assertEquals("Unit 2", address.getLine2()),
                () -> assertEquals("CBD", address.getSuburb()),
                () -> assertEquals("Auckland", address.getCity()),
                () -> assertEquals("1010", address.getPostCode()),
                () -> assertEquals("NZ", address.getCountry()),
                () -> assertTrue(address.isPrimary()),
                () -> assertFalse(address.isMailing()),
                () -> assertNull(address.getId()),
                () -> assertNull(address.getOtherType()));
    }

    @Test
    void settersUpdateFields() {
        Address address = new Address();
        address.setId(5L);
        address.setCustomerNumber("C2");
        address.setType("Other");
        address.setOtherType("Bach");
        address.setPrimary(false);
        address.setMailing(true);
        address.setLine1("2 Beach Rd");
        address.setLine2("");
        address.setSuburb("Piha");
        address.setCity("Auckland");
        address.setPostCode("0772");
        address.setCountry("NZ");

        assertAll(
                () -> assertEquals(5L, address.getId()),
                () -> assertEquals("C2", address.getCustomerNumber()),
                () -> assertEquals("Other", address.getType()),
                () -> assertEquals("Bach", address.getOtherType()),
                () -> assertFalse(address.isPrimary()),
                () -> assertTrue(address.isMailing()),
                () -> assertEquals("2 Beach Rd", address.getLine1()),
                () -> assertEquals("", address.getLine2()),
                () -> assertEquals("Piha", address.getSuburb()),
                () -> assertEquals("Auckland", address.getCity()),
                () -> assertEquals("0772", address.getPostCode()),
                () -> assertEquals("NZ", address.getCountry()));
    }

    @Test
    void isNewUntilIdAssigned() {
        Address address = new Address();
        assertTrue(address.isNew());

        address.setId(1L);
        assertFalse(address.isNew());
    }
}
