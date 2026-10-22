// AI-assisted: tests the banner wording copied from the Figma frames.
package lavs.ui;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BannerContentTests {
    @Test
    void offlineBannerIsAWarningWithTheFigmaWording() {
        BannerContent banner = BannerContent.offline();

        assertEquals(BannerVariant.WARNING, banner.variant());
        assertEquals("You're offline", banner.title());
        assertEquals("Your changes are saved on this laptop and will upload automatically, in order, when the "
                + "connection returns. Customer search is unavailable until then.", banner.body());
    }

    @Test
    void rejectionBannerNamesTheThingAndGivesTheMainframesReason() {
        BannerContent banner = BannerContent.rejection("document", "File name already exists", true);

        assertEquals(BannerVariant.ERROR, banner.variant());
        assertEquals("The mainframe didn't accept this document", banner.title());
        assertEquals("Reason given: File name already exists. Nothing has been lost. "
                + "Fix the highlighted field and save again.", banner.body());
    }

    @Test
    void rejectionWithoutAHighlightedFieldTellsTheUserToCheckTheDetails() {
        BannerContent banner = BannerContent.rejection("document", "File name already exists", false);

        assertEquals("The mainframe didn't accept this document", banner.title());
        assertEquals("Reason given: File name already exists. Nothing has been lost. "
                + "Check the details and save again.", banner.body());
    }

    @Test
    void rejectionReasonKeepsItsOwnFullStopWithoutDoubling() {
        BannerContent banner = BannerContent.rejection("loan", "Principal is too high.", true);

        assertEquals("Reason given: Principal is too high. Nothing has been lost. "
                + "Fix the highlighted field and save again.", banner.body());
    }

    @Test
    void rejectionWithNoMessageSaysThereWasNoReason() {
        assertEquals("The mainframe gave no reason. Nothing has been lost. Fix the highlighted field and save again.",
                BannerContent.rejection("document", null, true).body());
        assertEquals("The mainframe gave no reason. Nothing has been lost. Check the details and save again.",
                BannerContent.rejection("document", "  ", false).body());
    }

    @Test
    void validationBannerUsesTheTitleAndBodyFromThePresenterResult() {
        ValidationResult validation = new ValidationResult();
        validation.add(new FieldError("type", "is required"));
        validation.add(new FieldError("fileName", "is required"));
        FieldErrorPresenter.Result result = new FieldErrorPresenter(Map.<String, FieldView>of()).present(validation);

        BannerContent banner = BannerContent.validation(result);

        assertEquals(BannerVariant.ERROR, banner.variant());
        assertEquals("2 fields need attention", banner.title());
        assertEquals(result.getBannerBody(), banner.body());
    }
}
