// AI-assisted: tests for when the add or edit document form is open.
package lavs.ui;

import lavs.logic.DocumentRow;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DocumentFormStateTests {
    private static final DocumentRow PASSPORT = new DocumentRow(7, "Proof of Identity", "passport.pdf", null, 0);

    // DOC-R2
    @Test
    void theFormStartsClosed() {
        Assertions.assertFalse(new DocumentFormState().isOpen());
    }

    // DOC-R2
    @Test
    void addOpensTheFormTitledAddDocument() {
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
    void editOpensTheFormTitledEditDocumentForThatDocument() {
        // Arrange
        DocumentFormState form = new DocumentFormState();

        // Act
        form.startEdit(PASSPORT);

        // Assert
        Assertions.assertTrue(form.isOpen());
        Assertions.assertEquals("Edit document", form.title());
        Assertions.assertEquals(PASSPORT, form.editing().orElseThrow());
    }

    // DOC-R2
    @Test
    void closingForgetsWhichDocumentWasBeingEdited() {
        // Arrange
        DocumentFormState form = new DocumentFormState();
        form.startEdit(PASSPORT);

        // Act
        form.close();

        // Assert
        Assertions.assertFalse(form.isOpen());
        Assertions.assertTrue(form.editing().isEmpty());
    }

    // DOC-R3
    @Test
    void addAfterEditStartsAFreshForm() {
        // Arrange
        DocumentFormState form = new DocumentFormState();
        form.startEdit(PASSPORT);

        // Act
        form.startAdd();

        // Assert
        Assertions.assertEquals("Add document", form.title());
        Assertions.assertTrue(form.editing().isEmpty());
    }
}
