package lavs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class EmailTests {

    @Test
    void constructorSetsFields() {
        Email email = new Email("C1", "Personal", "aroha@example.com", true);

        assertAll(
                () -> assertEquals("C1", email.getCustomerNumber()),
                () -> assertEquals("Personal", email.getType()),
                () -> assertEquals("aroha@example.com", email.getAddress()),
                () -> assertTrue(email.isPrimary()),
                () -> assertNull(email.getId()),
                () -> assertNull(email.getOtherType()));
    }

    @Test
    void settersUpdateFields() {
        Email email = new Email();
        email.setId(9L);
        email.setCustomerNumber("C2");
        email.setType("Other");
        email.setOtherType("Shared");
        email.setAddress("family@example.com");
        email.setPrimary(false);

        assertAll(
                () -> assertEquals(9L, email.getId()),
                () -> assertEquals("C2", email.getCustomerNumber()),
                () -> assertEquals("Other", email.getType()),
                () -> assertEquals("Shared", email.getOtherType()),
                () -> assertEquals("family@example.com", email.getAddress()),
                () -> assertFalse(email.isPrimary()));
    }

    @Test
    void isNewUntilIdAssigned() {
        Email email = new Email();
        assertTrue(email.isNew());

        email.setId(1L);
        assertFalse(email.isNew());
    }
}
