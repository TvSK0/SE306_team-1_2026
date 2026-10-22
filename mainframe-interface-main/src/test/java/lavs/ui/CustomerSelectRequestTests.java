// AI-assisted: tests for the select-mode request a caller passes to the Navigator.
package lavs.ui;

import lavs.model.Customer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomerSelectRequestTests {
    // US2-S1
    @Test
    void theCallersWordingAndTitleAreKept() {
        // Act
        CustomerSelectRequest request = new CustomerSelectRequest("Loan L-5521 · Adding a co-borrower or guarantor",
                "Select a co-borrower or guarantor", "For loan L-5521.", null, null, null, null);

        // Assert
        Assertions.assertEquals("Loan L-5521 · Adding a co-borrower or guarantor", request.topBarTitle());
        Assertions.assertEquals("Select a co-borrower or guarantor", request.title());
        Assertions.assertEquals("For loan L-5521.", request.subtitle());
    }

    // US2-S1 (extra controls and every callback may be left out)
    @Test
    void theOptionalPartsAreAbsentWhenTheCallerLeavesThemOut() {
        // Act
        CustomerSelectRequest request = new CustomerSelectRequest(null, "Select", null, null, null, null, null);

        // Assert
        Assertions.assertTrue(request.extraControlsNode().isEmpty());
        Assertions.assertTrue(request.selectedHandler().isEmpty());
        Assertions.assertTrue(request.cancelHandler().isEmpty());
        Assertions.assertTrue(request.addNewHandler().isEmpty());
    }

    // US2-S2, US2-S3
    @Test
    void theCallbacksAreHandedBackWhenGiven() {
        // Arrange
        Customer[] chosen = new Customer[1];
        boolean[] ran = new boolean[2];
        CustomerSelectRequest request = new CustomerSelectRequest(null, "Select", null, null,
                customer -> chosen[0] = customer, () -> ran[0] = true, () -> ran[1] = true);
        Customer customer = new Customer();

        // Act
        request.selectedHandler().orElseThrow().accept(customer);
        request.cancelHandler().orElseThrow().run();
        request.addNewHandler().orElseThrow().run();

        // Assert
        Assertions.assertSame(customer, chosen[0]);
        Assertions.assertTrue(ran[0]);
        Assertions.assertTrue(ran[1]);
    }

    // US2-S1
    @Test
    void aRequestWithoutAHeadingIsRefused() {
        Assertions.assertThrows(NullPointerException.class,
                () -> new CustomerSelectRequest(null, null, null, null, null, null, null));
    }

    // US2-S1
    @Test
    void fromAcceptsARequest() {
        // Arrange
        CustomerSelectRequest request = new CustomerSelectRequest(null, "Select", null, null, null, null, null);

        // Act and Assert
        Assertions.assertSame(request, CustomerSelectRequest.from(request));
    }

    // US2-S1
    @Test
    void fromRefusesAnythingElse() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> CustomerSelectRequest.from("C-1"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CustomerSelectRequest.from(null));
    }

    // US2-S1
    @Test
    void theRequestNamesTheTopBarTitleForTheNavigator() {
        // Act
        TitledParameter titled = new CustomerSelectRequest("Loan L-1", "Select", null, null, null, null, null);

        // Assert
        Assertions.assertEquals("Loan L-1", TopBarState.titleFor(Screen.CUSTOMER_SELECT, titled));
    }
}
