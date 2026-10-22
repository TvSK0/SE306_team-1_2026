// AI-assisted: tests for the Documents panel's Figma 11 wording and save decisions.
package lavs.ui;

import lavs.data.MainframeGateway;
import lavs.logic.RecordDocumentsService;
import lavs.logic.RecordOwner;
import lavs.logic.ServiceResult;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DocumentsTextTests {
    // DOC-R1, DOC-R5
    @Test
    void theHeadingSubtitleButtonAndEmptyTextAreFromFigma11() {
        Assertions.assertEquals("Documents", DocumentsText.HEADING);
        Assertions.assertEquals(
                "Document details only. The file itself stays with the customer and is not uploaded.",
                DocumentsText.SUBTITLE);
        Assertions.assertEquals("+ Add document", DocumentsText.ADD_BUTTON);
        Assertions.assertEquals("No documents yet.", DocumentsText.EMPTY);
    }

    // DOC-R2
    @Test
    void aSavedDocumentClosesTheCardWithNoBanner() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addDocument", FakeConnection.success(Map.of("id", "9")));

        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(save(connection, "a.pdf"));

        // Assert
        Assertions.assertTrue(display.closeForm());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertFalse(display.validationBanner());
    }

    // DOC-R2
    @Test
    void aBlankFieldShowsInlineErrorsAndTheValidationBanner() {
        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(service(new FakeConnection())
                .save(RecordOwner.CUSTOMER, "C-1", null, "", "", "", "x"));

        // Assert
        Assertions.assertFalse(display.closeForm());
        Assertions.assertTrue(display.validationBanner());
        Assertions.assertTrue(display.banner().isEmpty());
        Assertions.assertFalse(display.fieldErrors().isValid());
    }

    // DOC-R6: "The mainframe didn't accept this document" / "Reason given: <message>. Nothing has
    // been lost. Fix the highlighted field and save again." with the field highlighted
    @Test
    void aRefusalShowsTheFigma11BannerAndHighlightsTheFieldItNames() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addDocument", FakeConnection.rejected("File name already exists"));

        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(save(connection, "dup.pdf"));

        // Assert
        Assertions.assertFalse(display.closeForm());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "The mainframe didn't accept this document",
                "Reason given: File name already exists. Nothing has been lost. Fix the highlighted field and save again."),
                display.banner().orElseThrow());
        Assertions.assertEquals(List.of(new FieldError("fileName", "File name already exists")),
                display.fieldErrors().getErrors());
    }

    // DOC-R6 (a reason that names no field highlights nothing and says to check the details)
    @Test
    void aRefusalThatNamesNoFieldHighlightsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.addDocument", FakeConnection.rejected("Duplicate document"));

        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(save(connection, "a.pdf"));

        // Assert
        Assertions.assertTrue(display.fieldErrors().isValid());
        Assertions.assertTrue(display.banner().orElseThrow().body().endsWith("Check the details and save again."));
    }

    // DOC-R6
    @Test
    void theFieldIsGuessedFromWhatTheReasonNames() {
        Assertions.assertEquals(Optional.of("fileType"), DocumentsText.fieldFor("Invalid file type"));
        Assertions.assertEquals(Optional.of("fileType"), DocumentsText.fieldFor("bad fileType"));
        Assertions.assertEquals(Optional.of("fileName"), DocumentsText.fieldFor("file_name is too long"));
        Assertions.assertEquals(Optional.of("fileName"), DocumentsText.fieldFor("Bad filename"));
        Assertions.assertEquals(Optional.of("size"), DocumentsText.fieldFor("Size must be positive"));
        Assertions.assertEquals(Optional.of("type"), DocumentsText.fieldFor("Unknown document type"));
        Assertions.assertEquals(Optional.empty(), DocumentsText.fieldFor("Something else"));
        Assertions.assertEquals(Optional.empty(), DocumentsText.fieldFor(" "));
    }

    // DOC-R6 (without the upload queue, a save needs a connection)
    @Test
    void aNetworkFailureSaysTheMainframeCouldNotBeReached() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        DocumentsText.SaveDisplay display = DocumentsText.saveDisplay(save(connection, "a.pdf"));

        // Assert
        Assertions.assertFalse(display.closeForm());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't save the document",
                DocumentsText.UNREACHABLE), display.banner().orElseThrow());
    }

    private static RecordDocumentsService service(FakeConnection connection) {
        return new RecordDocumentsService(new MainframeGateway(connection));
    }

    private static ServiceResult<Void> save(FakeConnection connection, String fileName) {
        return service(connection).save(RecordOwner.CUSTOMER, "C-1", null, "Identity", fileName, "application/pdf", "120");
    }
}
