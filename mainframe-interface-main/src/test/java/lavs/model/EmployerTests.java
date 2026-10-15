package lavs.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class EmployerTests {

    @Test
    void constructorSetsFields() {
        Employer employer = new Employer("C1", "Primary", "Countrywide", "Teller", new BigDecimal("65000.00"));

        assertAll(
                () -> assertEquals("C1", employer.getCustomerNumber()),
                () -> assertEquals("Primary", employer.getType()),
                () -> assertEquals("Countrywide", employer.getName()),
                () -> assertEquals("Teller", employer.getPosition()),
                () -> assertEquals(new BigDecimal("65000.00"), employer.getSalary()),
                () -> assertNull(employer.getId()));
    }

    @Test
    void settersUpdateFields() {
        Employer employer = new Employer();
        employer.setId(4L);
        employer.setCustomerNumber("C2");
        employer.setType("Secondary");
        employer.setName("Cafe");
        employer.setPosition("Barista");
        employer.setSalary(new BigDecimal("20000"));

        assertAll(
                () -> assertEquals(4L, employer.getId()),
                () -> assertEquals("C2", employer.getCustomerNumber()),
                () -> assertEquals("Secondary", employer.getType()),
                () -> assertEquals("Cafe", employer.getName()),
                () -> assertEquals("Barista", employer.getPosition()),
                () -> assertEquals(new BigDecimal("20000"), employer.getSalary()));
    }

    @Test
    void salaryCanBeMissing() {
        Employer employer = new Employer("C1", "Primary", "Countrywide", "Teller", null);

        assertNull(employer.getSalary());
    }

    @Test
    void isNewUntilIdAssigned() {
        Employer employer = new Employer();
        assertTrue(employer.isNew());

        employer.setId(1L);
        assertFalse(employer.isNew());
    }
}
