// AI-assisted: tests for when the add or edit document card is open (Figma 11).
package lavs.ui;

import lavs.model.RecordDocument;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DocumentFormStateTests {
    private static final RecordDocument PASSPORT = new RecordDocument(7, "Identity", "passport.pdf", "application/pdf", 120);

    // DOC-R2
    @Test
    void theCardStartsClosed() {
        Assertions.assertFalse(new DocumentFormState().isOpen());
    }

    // DOC-R2
    @Test
    void addOpensTheCardTitledAddDocument() {
        // Arrange
        DocumentFormState form = new DocumentFormState();

        // Act
        form.startAdd();

        // Assert
        Assertions.assertTrue(form.isOpen());
        Assertions.assertEquals("Add document", form.title());
        Assertions.assertTrue(form.editing().isEmpty());
    }

    // DOC-R3
    @Test
    void editOpensTheCardTitledEditDocument() {
        // Arrange
        DocumentFormState form = new DocumentFormState();

        // Act
        form.startEdit(PASSPORT);

        // Assert
        Assertions.assertEquals("Edit document", form.title());
        Assertions.assertEquals(PASSPORT, form.editing().orElseThrow());
    }

    // DOC-R2
    @Test
    void closingForgetsWhatWasBeingEdited() {
        // Arrange
        DocumentFormState form = new DocumentFormState();
        form.startEdit(PASSPORT);

        // Act
        form.close();

        // Assert
        Assertions.assertFalse(form.isOpen());
        Assertions.assertTrue(form.editing().isEmpty());
        Assertions.assertEquals("Add document", form.title());
    }
}
