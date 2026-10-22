// AI-assisted: tests for the customer search wording and formatting rules (Figma 02, 09).
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class CustomerSearchTextTests {
    // US2-R1
    @Test
    void theCountLineUsesTheSingularForOne() {
        Assertions.assertEquals("1 customer found", CustomerSearchText.countLine(1, 1, 25));
    }

    // US2-R1, US2-R8 (the count is always the total, with the page once there is more than one)
    @Test
    void theCountLineShowsTheTotalAndThePageWhenThereAreSeveral() {
        Assertions.assertEquals("25 customers found", CustomerSearchText.countLine(25, 1, 25));
        Assertions.assertEquals("60 customers found · Page 2 of 3", CustomerSearchText.countLine(60, 2, 25));
    }

    // US2-R6
    @Test
    void noMatchesSaysZeroCustomersFoundAndNamesTheQuery() {
        Assertions.assertEquals("0 customers found", CustomerSearchText.countLine(0, 1, 25));
        Assertions.assertEquals("No customers match \"smith\". Check the spelling, or add a new customer.",
                CustomerSearchText.emptyText("smith"));
    }

    // US2-R1 (Figma 02 wording)
    @Test
    void theHeadingFieldAndButtonUseTheFigmaWording() {
        Assertions.assertEquals("Find a customer", CustomerSearchText.TITLE);
        Assertions.assertEquals("Search before adding a new customer to avoid creating a duplicate.",
                CustomerSearchText.SUBTITLE);
        Assertions.assertEquals("Search by name or customer number", CustomerSearchText.FIELD_LABEL);
        Assertions.assertEquals("+ Add New Customer", CustomerSearchText.ADD_NEW);
    }

    // US2-R1
    @Test
    void activeIsASuccessTagInAnyCase() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.SUCCESS, "Active"),
                CustomerSearchText.statusTag("ACTIVE"));
    }

    // US2-R1
    @Test
    void inactiveIsANeutralTag() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Inactive"),
                CustomerSearchText.statusTag("inactive"));
    }

    // US2-R1
    @Test
    void anyOtherStatusIsNeutralAsSentAndNoStatusIsBlank() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "On hold"),
                CustomerSearchText.statusTag("On hold"));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, ""),
                CustomerSearchText.statusTag(null));
    }

    // US2-R1
    @Test
    void theDateOfBirthShowsAsDayMonthYearOrBlank() {
        Assertions.assertEquals("12/04/1990", CustomerSearchText.dateOfBirth(LocalDate.of(1990, 4, 12)));
        Assertions.assertEquals("", CustomerSearchText.dateOfBirth(null));
    }

    // US2-R5, US2-R7
    @Test
    void searchIsDisabledWhileOfflineOrWhileASearchRuns() {
        Assertions.assertTrue(CustomerSearchText.searchDisabled(false, false));
        Assertions.assertTrue(CustomerSearchText.searchDisabled(true, true));
        Assertions.assertFalse(CustomerSearchText.searchDisabled(true, false));
    }

    // US2-R5
    @Test
    void theOfflineBannerShowsOnlyWhileOffline() {
        Assertions.assertEquals(BannerContent.offline(), CustomerSearchText.offlineBanner(false).orElseThrow());
        Assertions.assertTrue(CustomerSearchText.offlineBanner(true).isEmpty());
    }
}
