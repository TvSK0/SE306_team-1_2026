// AI-assisted: tests how a ValidationResult is shown on fields and turned into the banner text.
package lavs.ui;

import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldErrorPresenterTests {
    // remembers what it was told
    private static final class RecordingField implements FieldView {
        String error;

        @Override
        public void setError(String message) {
            error = message;
        }

        @Override
        public void clearError() {
            error = null;
        }
    }

    private final RecordingField type = new RecordingField();
    private final RecordingField fileName = new RecordingField();
    private final FieldErrorPresenter presenter = new FieldErrorPresenter(Map.of("type", type, "fileName", fileName));

    private static ValidationResult errors(FieldError... errors) {
        ValidationResult result = new ValidationResult();
        for (FieldError error : errors) {
            result.add(error);
        }
        return result;
    }

    @Test
    void aValidResultLeavesEveryFieldClearAndNeedsNoBanner() {
        FieldErrorPresenter.Result result = presenter.present(new ValidationResult());

        assertNull(type.error);
        assertNull(fileName.error);
        assertFalse(result.hasErrors());
        assertTrue(result.getUnmatched().isEmpty());
    }

    @Test
    void anErrorIsShownOnTheFieldWithThatName() {
        presenter.present(errors(new FieldError("fileName", "is required")));

        assertEquals("is required", fileName.error);
        assertNull(type.error);
    }

    @Test
    void severalFieldsCanBeFlaggedAtOnce() {
        presenter.present(errors(new FieldError("type", "is required"), new FieldError("fileName", "is required")));

        assertEquals("is required", type.error);
        assertEquals("is required", fileName.error);
    }

    @Test
    void presentingAgainClearsErrorsFromThePreviousAttempt() {
        presenter.present(errors(new FieldError("type", "is required")));

        presenter.present(errors(new FieldError("fileName", "is required")));

        assertNull(type.error);
        assertEquals("is required", fileName.error);
    }

    @Test
    void aValidResultAfterAnInvalidOneClearsEverything() {
        presenter.present(errors(new FieldError("type", "is required")));

        presenter.present(new ValidationResult());

        assertNull(type.error);
    }

    @Test
    void aFieldWithSeveralErrorsShowsTheFirstOne() {
        presenter.present(errors(new FieldError("type", "is required"), new FieldError("type", "is not a known type")));

        assertEquals("is required", type.error);
    }

    @Test
    void aFieldWithSeveralErrorsCountsOnce() {
        FieldErrorPresenter.Result result = presenter.present(
                errors(new FieldError("type", "is required"), new FieldError("type", "is not a known type")));

        assertEquals("1 field needs attention", result.getBannerTitle());
    }

    @Test
    void anErrorForAnUnknownFieldIsReturnedNotLost() {
        FieldError stray = new FieldError("mailingAddress", "the customer needs a mailing address");

        FieldErrorPresenter.Result result = presenter.present(errors(stray));

        assertEquals(java.util.List.of(stray), result.getUnmatched());
        assertTrue(result.hasErrors());
        assertNull(type.error);
        assertNull(fileName.error);
    }

    @Test
    void matchedAndUnmatchedErrorsAreSeparated() {
        FieldError stray = new FieldError("contactDetails", "the customer needs a phone number or email");

        FieldErrorPresenter.Result result = presenter.present(errors(new FieldError("type", "is required"), stray));

        assertEquals("is required", type.error);
        assertEquals(java.util.List.of(stray), result.getUnmatched());
        assertEquals("2 fields need attention", result.getBannerTitle());
    }

    @Test
    void bannerTitleCountsFieldsNotErrors() {
        FieldErrorPresenter.Result result = presenter.present(
                errors(new FieldError("type", "is required"), new FieldError("fileName", "is required"),
                        new FieldError("size", "must be a number")));

        assertEquals("3 fields need attention", result.getBannerTitle());
    }

    @Test
    void bannerBodyIsTheStandardSentenceWhenEveryErrorHasAField() {
        FieldErrorPresenter.Result result = presenter.present(errors(
                new FieldError("type", "is required"), new FieldError("fileName", "is required")));

        assertEquals("Everything else you entered has been kept. Fix the highlighted fields and save again.",
                result.getBannerBody());
    }

    @Test
    void bannerBodyUsesFieldInTheSingularForOneField() {
        FieldErrorPresenter.Result result = presenter.present(errors(new FieldError("type", "is required")));

        assertEquals("Everything else you entered has been kept. Fix the highlighted field and save again.",
                result.getBannerBody());
    }

    @Test
    void bannerBodyAddsOnlyTheHumanMessageOfAnErrorThatHadNoField() {
        FieldErrorPresenter.Result result = presenter.present(errors(
                new FieldError("type", "is required"),
                new FieldError("mailingAddress", "Add a mailing address")));

        assertEquals("Everything else you entered has been kept. Fix the highlighted fields and save again."
                        + " Also: Add a mailing address.",
                result.getBannerBody());
    }

    @Test
    void bannerBodyJoinsSeveralUnmatchedMessagesWithoutFieldNames() {
        FieldErrorPresenter.Result result = presenter.present(errors(
                new FieldError("mailingAddress", "Add a mailing address"),
                new FieldError("contactDetails", "Add a phone number or email")));

        assertEquals("Everything else you entered has been kept. Fix the highlighted fields and save again."
                        + " Also: Add a mailing address; Add a phone number or email.",
                result.getBannerBody());
    }

    @Test
    void bannerBodyDoesNotDoubleTheFullStopOfAMessage() {
        FieldErrorPresenter.Result result = presenter.present(errors(
                new FieldError("mailingAddress", "Add a mailing address.")));

        assertEquals("Everything else you entered has been kept. Fix the highlighted field and save again."
                        + " Also: Add a mailing address.",
                result.getBannerBody());
    }

    @Test
    void unmatchedErrorsStillCarryTheirFieldNameForScreensThatWantIt() {
        FieldErrorPresenter.Result result = presenter.present(errors(
                new FieldError("mailingAddress", "Add a mailing address")));

        assertEquals("mailingAddress", result.getUnmatched().get(0).getFieldName());
    }

    @Test
    void clearRemovesAllErrorsWithoutValidating() {
        presenter.present(errors(new FieldError("type", "is required")));

        presenter.clear();

        assertNull(type.error);
    }

    @Test
    void theFieldMapIsCopiedSoLaterChangesDoNotAffectIt() {
        Map<String, RecordingField> fields = new LinkedHashMap<>();
        fields.put("type", type);
        FieldErrorPresenter copy = new FieldErrorPresenter(fields);
        fields.put("fileName", fileName);

        FieldErrorPresenter.Result result = copy.present(errors(new FieldError("fileName", "is required")));

        assertNull(fileName.error);
        assertEquals(1, result.getUnmatched().size());
    }
}
