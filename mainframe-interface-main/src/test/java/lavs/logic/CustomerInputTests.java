package lavs.logic;

import lavs.model.Customer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class CustomerInputTests {
    @Test
    void fillsTheFormFromASavedCustomer() {
        Customer customer = new Customer("Ms", "Jane", "Smith", LocalDate.of(2001, 2, 3), "New Zealand", 1);

        CustomerInput input = CustomerInput.fromCustomer(customer);

        assertAll(
                () -> assertEquals("Ms", input.getTitle()),
                () -> assertEquals("Jane", input.getFirstName()),
                () -> assertEquals("Smith", input.getFamilyName()),
                () -> assertEquals("2001-02-03", input.getDateOfBirth()),
                () -> assertEquals("New Zealand", input.getNationality()));
    }

    @Test
    void showsAMissingValueAsAnEmptyField() {
        CustomerInput input = CustomerInput.fromCustomer(new Customer());

        assertAll(
                () -> assertEquals("", input.getTitle()),
                () -> assertEquals("", input.getFirstName()),
                () -> assertEquals("", input.getFamilyName()),
                () -> assertEquals("", input.getDateOfBirth()),
                () -> assertEquals("", input.getNationality()));
    }
}
