// AI-assisted: tests for the customer search wording and formatting rules.
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

public class CustomerSearchTextTests {
    // US2-R1
    @Test
    void theCountLineUsesTheSingularForOne() {
        Assertions.assertEquals("1 customer found", CustomerSearchText.countLine(1));
    }

    // US2-R1
    @Test
    void theCountLineUsesThePluralForManyCustomers() {
        Assertions.assertEquals("25 customers found", CustomerSearchText.countLine(25));
    }

    // US2-R6
    @Test
    void theCountLineSaysZeroCustomersFound() {
        Assertions.assertEquals("0 customers found", CustomerSearchText.countLine(0));
    }

    // US2-R6
    @Test
    void theEmptyTextNamesTheQuery() {
        Assertions.assertEquals("No customers match \"smith\". Check the spelling, or add a new customer.",
                CustomerSearchText.emptyText("smith"));
    }

    // US2-R1
    @Test
    void activeIsASuccessTag() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.SUCCESS, "Active"),
                CustomerSearchText.statusTag("Active"));
    }

    // US2-R1
    @Test
    void activeInAnyCaseIsStillASuccessTagReadingActive() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.SUCCESS, "Active"),
                CustomerSearchText.statusTag("ACTIVE"));
    }

    // US2-R1
    @Test
    void inactiveIsANeutralTag() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Inactive"),
                CustomerSearchText.statusTag("Inactive"));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Inactive"),
                CustomerSearchText.statusTag("inactive"));
    }

    // US2-R1
    @Test
    void anyOtherStatusIsNeutralAndShownAsTheMainframeSentIt() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "On hold"),
                CustomerSearchText.statusTag("On hold"));
    }

    // US2-R1
    @Test
    void aMissingStatusIsANeutralTagWithNoText() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, ""),
                CustomerSearchText.statusTag(null));
    }

    // US2-R1
    @Test
    void theDateOfBirthShowsAsDayMonthYear() {
        Assertions.assertEquals("12/04/1990", CustomerSearchText.dateOfBirth(LocalDate.of(1990, 4, 12)));
    }

    // US2-R1
    @Test
    void anUnknownDateOfBirthIsBlank() {
        Assertions.assertEquals("", CustomerSearchText.dateOfBirth(null));
    }

    // US2-R8
    @Test
    void theShowMoreButtonSaysHowManyAreShown() {
        Assertions.assertEquals("Show more (10 of 25 shown)", CustomerSearchText.showMoreLabel(10, 25));
    }

    // US2-R5
    @Test
    void searchCannotBePressedWhileOffline() {
        Assertions.assertTrue(CustomerSearchText.searchDisabled(false, false));
        Assertions.assertTrue(CustomerSearchText.searchDisabled(false, true));
    }

    // US2-R7
    @Test
    void searchCannotBePressedWhileASearchRuns() {
        Assertions.assertTrue(CustomerSearchText.searchDisabled(true, true));
    }

    // US2-R5, US2-R7
    @Test
    void searchCanBePressedWhenOnlineAndIdle() {
        Assertions.assertFalse(CustomerSearchText.searchDisabled(true, false));
    }

    // US2-R5
    @Test
    void theOfflineBannerShowsOnlyWhileOffline() {
        Assertions.assertEquals(BannerContent.offline(), CustomerSearchText.offlineBanner(false).orElseThrow());
        Assertions.assertTrue(CustomerSearchText.offlineBanner(true).isEmpty());
    }

    // US2-R1
    @Test
    void aRefusalShowsTheMainframesReason() {
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't search",
                "Reason given: Search term too short. Try again."), CustomerSearchText.rejected("Search term too short"));
    }

    // US2-R1
    @Test
    void aRefusalDoesNotAddASecondFullStop() {
        Assertions.assertEquals("Reason given: Too short. Try again.",
                CustomerSearchText.rejected("Too short.").body());
    }

    // US2-R1
    @Test
    void aRefusalWithNoReasonSaysSo() {
        Assertions.assertEquals("The mainframe gave no reason. Try again.", CustomerSearchText.rejected(" ").body());
    }

    // US2-R1
    @Test
    void anUnexpectedFailureShowsItsMessage() {
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't search", "Something went wrong: boom."),
                CustomerSearchText.unexpected(new IllegalStateException("boom")));
    }

    // US2-R1
    @Test
    void anUnexpectedFailureWithoutAMessageStillShowsABanner() {
        Assertions.assertEquals("Something went wrong.",
                CustomerSearchText.unexpected(new IllegalStateException()).body());
    }
}
