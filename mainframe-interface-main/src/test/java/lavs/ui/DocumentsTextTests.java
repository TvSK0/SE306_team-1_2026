// AI-assisted: tests for the Documents section's wording and decisions (Figma 11).
package lavs.ui;

import lavs.logic.CustomerDocumentService;
import lavs.logic.DocumentInput;
import lavs.logic.LookupResult;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DocumentsTextTests {
    private static final DocumentInput VALID = new DocumentInput("Proof of Identity", "a.pdf", "", "");

    // DOC-R1, DOC-R5
    @Test
    void theSectionWordingIsFromFigma11() {
        Assertions.assertEquals("Documents", DocumentsText.HEADING);
        Assertions.assertEquals(
                "Document details only. The file itself stays with the customer and is not uploaded.",
                DocumentsText.SUBTITLE);
        Assertions.assertEquals("+ Add document", DocumentsText.ADD_BUTTON);
        Assertions.assertEquals("Save document", DocumentsText.SAVE);
        Assertions.assertEquals("No documents yet.", DocumentsText.EMPTY);
    }

    // DOC-R2
    @Test
    void anEditableDropdownGivesItsTypedTextOtherwiseItsSelectedValue() {
        Assertions.assertEquals("typed", DocumentsText.chosen("typed", "picked"));
        Assertions.assertEquals("picked", DocumentsText.chosen("  ", "picked"));
        Assertions.assertEquals("picked", DocumentsText.chosen(null, "picked"));
        Assertions.assertEquals("", DocumentsText.chosen("", null));
    }

    // DOC-R2
    @Test
    void aSavedDocumentClosesTheFormWithNoBanner() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addDocument", FakeConnection.success(Map.of("id", "9", "size", "0")));

        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(
                new CustomerDocumentService(connection).add("C-1", VALID));

        // Assert
        Assertions.assertTrue(display.saved());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertFalse(display.validationBanner());
        Assertions.assertTrue(display.fieldErrors().isValid());
    }

    // DOC-R2
    @Test
    void aBlankFieldShowsInlineErrorsAndTheValidationBannerAndNothingIsSaved() {
        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(
                new CustomerDocumentService(new FakeConnection()).add("C-1", new DocumentInput("", "", "", "")));

        // Assert
        Assertions.assertFalse(display.saved());
        Assertions.assertTrue(display.validationBanner());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertEquals(List.of(DocumentInput.TYPE, DocumentInput.FILE_NAME),
                display.fieldErrors().getErrors().stream().map(FieldError::getFieldName).toList());
    }

    // DOC-R6: "The mainframe didn't accept this document" / "Reason given: <message>. Nothing has
    // been lost. Fix the highlighted field and save again." with the field highlighted
    @Test
    void aRefusalShowsTheFigma11BannerAndHighlightsTheFieldItNames() {
        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(refused("File name already exists"));

        // Assert
        Assertions.assertFalse(display.saved());
        Assertions.assertFalse(display.validationBanner());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "The mainframe didn't accept this document",
                "Reason given: File name already exists. Nothing has been lost. Fix the highlighted field and save again."),
                display.banner().orElseThrow());
        Assertions.assertEquals(List.of(new FieldError(DocumentInput.FILE_NAME, "File name already exists")),
                display.fieldErrors().getErrors());
    }

    // DOC-R6 (a reason that names no field highlights nothing and says to check the details)
    @Test
    void aRefusalThatNamesNoFieldHighlightsNothing() {
        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(refused("Duplicate document"));

        // Assert
        Assertions.assertTrue(display.fieldErrors().isValid());
        Assertions.assertTrue(display.banner().orElseThrow().body().endsWith("Check the details and save again."));
    }

    // DOC-R6
    @Test
    void aRefusalWithNoReasonSaysSoAndHighlightsNothing() {
        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(refused(" "));

        // Assert
        Assertions.assertTrue(display.fieldErrors().isValid());
        Assertions.assertTrue(display.banner().orElseThrow().body().startsWith("The mainframe gave no reason."));
    }

    // DOC-R6
    @Test
    void theFieldIsGuessedFromWhatTheReasonNames() {
        Assertions.assertEquals(Optional.of(DocumentInput.FILE_TYPE), DocumentsText.fieldFor("Invalid file type"));
        Assertions.assertEquals(Optional.of(DocumentInput.FILE_TYPE), DocumentsText.fieldFor("bad fileType"));
        Assertions.assertEquals(Optional.of(DocumentInput.FILE_NAME), DocumentsText.fieldFor("file_name is too long"));
        Assertions.assertEquals(Optional.of(DocumentInput.FILE_NAME), DocumentsText.fieldFor("Bad filename"));
        Assertions.assertEquals(Optional.of(DocumentInput.SIZE), DocumentsText.fieldFor("Size must be positive"));
        Assertions.assertEquals(Optional.of(DocumentInput.TYPE), DocumentsText.fieldFor("Unknown document type"));
        Assertions.assertEquals(Optional.empty(), DocumentsText.fieldFor("Something else"));
        Assertions.assertEquals(Optional.empty(), DocumentsText.fieldFor(null));
    }

    // DOC-R6 (saving needs a connection)
    @Test
    void aNetworkFailureOnSaveSaysTheMainframeCouldNotBeReached() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(
                new CustomerDocumentService(connection).add("C-1", VALID));

        // Assert
        Assertions.assertFalse(display.saved());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't save the document",
                "The mainframe could not be reached. Check your internet connection and try again."),
                display.banner().orElseThrow());
        Assertions.assertTrue(display.fieldErrors().isValid());
    }

    // DOC-R1
    @Test
    void aNetworkFailureOnTheListShowsTheOfflineBanner() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act and Assert
        Assertions.assertEquals(BannerContent.offline(), DocumentsText.loadFailure(
                new CustomerDocumentService(connection).list("C-1")).orElseThrow());
    }

    // DOC-R1
    @Test
    void aRefusalOnTheListShowsTheMainframesReason() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerDocument.list", FakeConnection.rejected("No such customer"));

        // Act and Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't load the documents",
                "Reason given: No such customer. Try again."),
                DocumentsText.loadFailure(new CustomerDocumentService(connection).list("C-1")).orElseThrow());
    }

    // DOC-R1
    @Test
    void aSuccessfulReadHasNoFailureBanner() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerDocument.list", FakeConnection.success(Map.of("total", "0", "items", "0")));

        // Act and Assert
        Assertions.assertTrue(DocumentsText.loadFailure(new CustomerDocumentService(connection).list("C-1")).isEmpty());
    }

    // DOC-R1 (a blank number never reaches the screen, but the banner still says something)
    @Test
    void anInvalidReadShowsABannerWithNoReason() {
        Assertions.assertEquals("The mainframe gave no reason. Try again.", DocumentsText.loadFailure(
                new CustomerDocumentService(new FakeConnection()).list(" ")).orElseThrow().body());
    }

    // DOC-R4
    @Test
    void aFailedDeleteShowsABannerAndASuccessfulOneShowsNone() {
        // Arrange
        FakeConnection refused = new FakeConnection();
        refused.whenRequested("customer.deleteDocument", FakeConnection.rejected("No such document"));
        FakeConnection offline = new FakeConnection();
        offline.setIsConnected(false);
        FakeConnection fine = new FakeConnection();
        fine.whenRequested("customer.deleteDocument", FakeConnection.success(Map.of("id", "7")));

        // Act and Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't delete the document",
                "Reason given: No such document. Try again."),
                DocumentsText.deleteFailure(new CustomerDocumentService(refused).delete("C-1", 7)).orElseThrow());
        Assertions.assertEquals("The mainframe could not be reached. Check your internet connection and try again.",
                DocumentsText.deleteFailure(new CustomerDocumentService(offline).delete("C-1", 7)).orElseThrow().body());
        Assertions.assertTrue(DocumentsText.deleteFailure(new CustomerDocumentService(fine).delete("C-1", 7)).isEmpty());
        Assertions.assertTrue(DocumentsText.deleteFailure(
                new CustomerDocumentService(new FakeConnection()).delete("", 7)).isPresent());
    }

    // DOC-R2
    @Test
    void anUnexpectedFailureShowsItsMessageUnderTheGivenTitle() {
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't save the document",
                "Something went wrong: boom."), DocumentsText.unexpected("Couldn't save the document",
                new IllegalStateException("boom")));
        Assertions.assertEquals("Something went wrong.",
                DocumentsText.unexpected("Couldn't save the document", new IllegalStateException()).body());
    }

    // the LookupResult factories are not public, so each refusal comes from the real service
    private static LookupResult<?> refused(String message) {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addDocument", FakeConnection.rejected(message));
        return new CustomerDocumentService(connection).add("C-1", VALID);
    }
}
