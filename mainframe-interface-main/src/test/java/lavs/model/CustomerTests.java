package lavs.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// AI-assisted: tests generated with Claude (team-approved).
class CustomerTests {

    @Test
    void constructorSetsFieldsAndLeavesMainframeFieldsEmpty() {
        Customer customer = new Customer("Mr", "Aroha", "Smith", LocalDate.of(2001, 2, 3), "NZ", 1L);

        assertAll(
                () -> assertEquals("Mr", customer.getTitle()),
                () -> assertEquals("Aroha", customer.getFirstName()),
                () -> assertEquals("Smith", customer.getFamilyName()),
                () -> assertEquals(LocalDate.of(2001, 2, 3), customer.getDateOfBirth()),
                () -> assertEquals("NZ", customer.getNationality()),
                () -> assertEquals(1L, customer.getOrganisation()),
                () -> assertNull(customer.getNumber()),
                () -> assertNull(customer.getStatus()));
    }

    @Test
    void settersUpdateFields() {
        Customer customer = new Customer();
        customer.setNumber("C123");
        customer.setTitle("Ms");
        customer.setFirstName("Mere");
        customer.setFamilyName("Brown");
        customer.setDateOfBirth(LocalDate.of(1990, 12, 31));
        customer.setNationality("AU");
        customer.setStatus("Active");
        customer.setOrganisation(7L);

        assertAll(
                () -> assertEquals("C123", customer.getNumber()),
                () -> assertEquals("Ms", customer.getTitle()),
                () -> assertEquals("Mere", customer.getFirstName()),
                () -> assertEquals("Brown", customer.getFamilyName()),
                () -> assertEquals(LocalDate.of(1990, 12, 31), customer.getDateOfBirth()),
                () -> assertEquals("AU", customer.getNationality()),
                () -> assertEquals("Active", customer.getStatus()),
                () -> assertEquals(7L, customer.getOrganisation()));
    }

    @Test
    void fullNameJoinsFirstAndFamilyName() {
        Customer customer = new Customer();
        customer.setFirstName(" Aroha ");
        customer.setFamilyName("Smith");

        assertEquals("Aroha Smith", customer.getFullName());
    }

    @Test
    void fullNameSkipsMissingFirstName() {
        Customer customer = new Customer();
        customer.setFamilyName("Smith");

        assertEquals("Smith", customer.getFullName());
    }

    @Test
    void fullNameSkipsBlankFamilyName() {
        Customer customer = new Customer();
        customer.setFirstName("Aroha");
        customer.setFamilyName("   ");

        assertEquals("Aroha", customer.getFullName());
    }

    @Test
    void fullNameIsEmptyWhenNoNames() {
        assertEquals("", new Customer().getFullName());
    }
}
