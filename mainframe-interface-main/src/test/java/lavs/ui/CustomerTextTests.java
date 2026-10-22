package lavs.ui;

import lavs.model.Customer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class CustomerTextTests {
    @Test
    void titlesTheRecordWithTheNameAndNumber() {
        assertEquals("Jane Smith (C-10234)", CustomerText.title(customer("C-10234", "Jane", "Smith", "Active")));
    }

    @Test
    void usesJustTheNumberWhenThereIsNoNameYet() {
        assertEquals("C-10234", CustomerText.title(customer("C-10234", null, null, "Active")));
    }

    @Test
    void callsAnUnsavedCustomerNew() {
        assertEquals(CustomerText.NEW_CUSTOMER, CustomerText.title(null));
        assertEquals(CustomerText.NEW_CUSTOMER, CustomerText.title(new Customer()));
    }

    @Test
    void showsTheStatus() {
        assertEquals("Status: Active", CustomerText.status(customer("C-1", "Jane", "Smith", "Active")));
        assertEquals("Status: unknown", CustomerText.status(customer("C-1", "Jane", "Smith", null)));
        assertEquals("Status: New (not saved yet)", CustomerText.status(null));
        assertEquals("Status: New (not saved yet)", CustomerText.status(new Customer()));
    }

    @Test
    void theSaveButtonSaysWhetherItCreatesOrSaves() {
        assertEquals("Create customer", CustomerText.saveButton(null));
        assertEquals("Save changes", CustomerText.saveButton(customer("C-1", "Jane", "Smith", "Active")));
    }

    @Test
    void tellsTheMmmTheNewCustomersNumber() {
        Customer saved = customer("C-10234", "Jane", "Smith", "Active");

        assertEquals("Customer created. Their number is C-10234.", CustomerText.saved(null, saved));
        assertEquals("Changes saved.", CustomerText.saved(saved, saved));
    }

    @Test
    void readsTheRecordsNavigationParameter() {
        assertEquals("C-1", CustomerScreens.customerNumber("C-1"));
        assertNull(CustomerScreens.customerNumber(null));
        assertThrows(IllegalArgumentException.class, () -> CustomerScreens.customerNumber(42));
    }

    private static Customer customer(String number, String firstName, String familyName, String status) {
        Customer customer = new Customer();
        customer.setNumber(number);
        customer.setFirstName(firstName);
        customer.setFamilyName(familyName);
        customer.setStatus(status);
        return customer;
    }
}
