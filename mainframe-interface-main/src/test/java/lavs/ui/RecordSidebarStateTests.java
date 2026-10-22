// AI-assisted: tests for the customer record sidebar.
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.OptionalLong;

public class RecordSidebarStateTests {
    // CR-R2
    @Test
    void detailsIsSelectedToStartWith() {
        Assertions.assertEquals(Section.DETAILS, new RecordSidebarState(true).selected());
    }

    // CR-R2
    @Test
    void clickingASectionSelectsIt() {
        // Arrange
        RecordSidebarState sidebar = new RecordSidebarState(true);

        // Act
        sidebar.select(Section.LOANS);

        // Assert
        Assertions.assertEquals(Section.LOANS, sidebar.selected());
    }

    // CR-R2 (the selected item gets the primary-soft look)
    @Test
    void onlyTheSelectedItemIsHighlighted() {
        // Arrange
        RecordSidebarState sidebar = new RecordSidebarState(true);
        sidebar.select(Section.NOTES);

        // Assert
        Assertions.assertEquals(List.of("lavs-nav-item", "selected"), sidebar.styleClasses(Section.NOTES));
        Assertions.assertEquals(List.of("lavs-nav-item"), sidebar.styleClasses(Section.LOANS));
    }

    // CR-R5 (no number until the count is known)
    @Test
    void thereIsNoCountUntilItIsKnown() {
        Assertions.assertTrue(new RecordSidebarState(true).countText(Section.ADDRESSES).isEmpty());
    }

    // CR-R5
    @Test
    void aKnownCountShowsItsNumberIncludingZero() {
        // Arrange
        RecordSidebarState sidebar = new RecordSidebarState(true);

        // Act
        sidebar.setCount(Section.ADDRESSES, OptionalLong.of(3));
        sidebar.setCount(Section.NOTES, OptionalLong.of(0));

        // Assert
        Assertions.assertEquals("3", sidebar.countText(Section.ADDRESSES).orElseThrow());
        Assertions.assertEquals("0", sidebar.countText(Section.NOTES).orElseThrow());
    }

    // CR-R5 (a count that could not be read is never guessed)
    @Test
    void aCountThatCouldNotBeReadShowsNothing() {
        // Arrange
        RecordSidebarState sidebar = new RecordSidebarState(true);

        // Act
        sidebar.setCount(Section.EMAILS, OptionalLong.empty());

        // Assert
        Assertions.assertTrue(sidebar.countText(Section.EMAILS).isEmpty());
    }

    // CR-R2 (Details has no count)
    @Test
    void detailsNeverShowsACount() {
        // Arrange
        RecordSidebarState sidebar = new RecordSidebarState(true);

        // Act
        sidebar.setCount(Section.DETAILS, OptionalLong.of(5));

        // Assert
        Assertions.assertTrue(sidebar.countText(Section.DETAILS).isEmpty());
    }

    // CR-R4
    @Test
    void aNewCustomerCanOnlyOpenDetails() {
        // Arrange
        RecordSidebarState sidebar = new RecordSidebarState(false);

        // Act
        sidebar.select(Section.ADDRESSES);

        // Assert
        Assertions.assertTrue(sidebar.isEnabled(Section.DETAILS));
        Assertions.assertFalse(sidebar.isEnabled(Section.ADDRESSES));
        Assertions.assertFalse(sidebar.isEnabled(Section.LOANS));
        Assertions.assertEquals(Section.DETAILS, sidebar.selected());
    }

    // CR-R4
    @Test
    void aSavedCustomerCanOpenEverySection() {
        // Arrange
        RecordSidebarState sidebar = new RecordSidebarState(true);

        // Act and Assert
        for (Section section : Section.values()) {
            Assertions.assertTrue(sidebar.isEnabled(section));
        }
    }
}
