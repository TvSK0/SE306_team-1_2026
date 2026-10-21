package lavs.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class LoanCoborrowerTests {

    @Test
    void constructorSetsFields() {
        LoanCoborrower link = new LoanCoborrower("L1", "C2", "Guarantor");

        assertAll(
                () -> assertEquals("L1", link.getLoanNumber()),
                () -> assertEquals("C2", link.getCoborrowerNumber()),
                () -> assertEquals("Guarantor", link.getType()),
                () -> assertNull(link.getId()),
                () -> assertNull(link.getCoborrowerName()));
    }

    @Test
    void settersUpdateFields() {
        LoanCoborrower link = new LoanCoborrower();
        link.setId(8L);
        link.setLoanNumber("L2");
        link.setCoborrowerNumber("C3");
        link.setCoborrowerName("Mere Brown");
        link.setType("Coborrower");

        assertAll(
                () -> assertEquals(8L, link.getId()),
                () -> assertEquals("L2", link.getLoanNumber()),
                () -> assertEquals("C3", link.getCoborrowerNumber()),
                () -> assertEquals("Mere Brown", link.getCoborrowerName()),
                () -> assertEquals("Coborrower", link.getType()));
    }

    @Test
    void isNewUntilIdAssigned() {
        LoanCoborrower link = new LoanCoborrower();
        assertTrue(link.isNew());

        link.setId(1L);
        assertFalse(link.isNew());
    }
}
