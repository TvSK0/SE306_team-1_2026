// AI-assisted: tests for the Contact details column of select mode.
package lavs.ui;

import lavs.logic.CustomerContactService.Check;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class ContactDetailsColumnTests {
    // US2-S4
    @Test
    void aRowWithNoAnswerYetSaysChecking() {
        // Act
        ContactDetailsColumn column = new ContactDetailsColumn();

        // Assert
        Assertions.assertEquals(new ContactDetailsColumn.ContactTag(TagVariant.NEUTRAL, "Checking…"),
                column.tag("C-1"));
    }

    // US2-S4
    @Test
    void aCompleteRowIsASuccessTag() {
        // Arrange
        ContactDetailsColumn column = new ContactDetailsColumn();

        // Act
        column.finish(column.generation(), "C-1", Check.COMPLETE);

        // Assert
        Assertions.assertEquals(new ContactDetailsColumn.ContactTag(TagVariant.SUCCESS, "Complete"),
                column.tag("C-1"));
    }

    // US2-S4
    @Test
    void aMissingRowIsADangerTag() {
        // Arrange
        ContactDetailsColumn column = new ContactDetailsColumn();

        // Act
        column.finish(column.generation(), "C-1", Check.MISSING);

        // Assert
        Assertions.assertEquals(new ContactDetailsColumn.ContactTag(TagVariant.DANGER, "Missing phone or email"),
                column.tag("C-1"));
    }

    // US2-S4
    @Test
    void aRowThatCouldNotBeCheckedIsNeutral() {
        // Arrange
        ContactDetailsColumn column = new ContactDetailsColumn();

        // Act
        column.finish(column.generation(), "C-1", Check.FAILED);

        // Assert
        Assertions.assertEquals(new ContactDetailsColumn.ContactTag(TagVariant.NEUTRAL, "Couldn't check"),
                column.tag("C-1"));
    }

    // US2-S4, US2-R7 (an answer for rows a newer search replaced must not change the screen)
    @Test
    void anAnswerFromBeforeANewSearchIsDropped() {
        // Arrange
        ContactDetailsColumn column = new ContactDetailsColumn();
        int old = column.generation();
        column.reset();

        // Act
        column.finish(old, "C-1", Check.COMPLETE);

        // Assert
        Assertions.assertEquals("Checking…", column.tag("C-1").text());
    }

    // US2-S4
    @Test
    void aNewSearchForgetsEarlierAnswers() {
        // Arrange
        ContactDetailsColumn column = new ContactDetailsColumn();
        column.finish(column.generation(), "C-1", Check.COMPLETE);

        // Act
        column.reset();

        // Assert
        Assertions.assertEquals("Checking…", column.tag("C-1").text());
        Assertions.assertEquals(1, column.generation());
    }

    // US2-S4 (checking stopped unexpectedly: no row is left saying "Checking…" for ever)
    @Test
    void unfinishedRowsBecomeCouldntCheckButAnswersAlreadyInStay() {
        // Arrange
        ContactDetailsColumn column = new ContactDetailsColumn();
        column.finish(column.generation(), "C-1", Check.COMPLETE);

        // Act
        column.failUnfinished(column.generation(), List.of("C-1", "C-2"));

        // Assert
        Assertions.assertEquals("Complete", column.tag("C-1").text());
        Assertions.assertEquals("Couldn't check", column.tag("C-2").text());
    }

    // US2-S4
    @Test
    void aFailureFromAnOlderSearchDoesNotTouchTheNewRows() {
        // Arrange
        ContactDetailsColumn column = new ContactDetailsColumn();
        int old = column.generation();
        column.reset();

        // Act
        column.failUnfinished(old, List.of("C-2"));

        // Assert
        Assertions.assertEquals("Checking…", column.tag("C-2").text());
    }
}
