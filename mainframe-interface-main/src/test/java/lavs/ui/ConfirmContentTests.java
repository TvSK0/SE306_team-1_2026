// AI-assisted: tests the wording of the delete confirmation.
package lavs.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfirmContentTests {
    @Test
    void deleteNamesTheThingBeingDeleted() {
        ConfirmContent content = ConfirmContent.delete("document");

        assertEquals("Delete this document?", content.title());
        assertEquals("This cannot be undone.", content.message());
        assertEquals("Delete", content.confirmLabel());
    }

    @Test
    void deleteWorksForAnyThing() {
        assertEquals("Delete this phone number?", ConfirmContent.delete("phone number").title());
    }
}
