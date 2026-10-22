// AI-assisted: tests for the customer record sections and their placeholders.
package lavs.ui;

import lavs.logic.CustomerCountsService.Kind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

public class SectionTests {
    // CR-R2 (Figma 03a order; the proposal list leaves out Emails and Figma wins)
    @Test
    void theSectionsAreInTheSidebarOrder() {
        Assertions.assertEquals(
                List.of("Details", "Addresses", "Phone numbers", "Emails", "Employers", "Relationships", "Notes",
                        "Documents", "Loans"),
                Arrays.stream(Section.values()).map(Section::label).toList());
    }

    // CR-R2
    @Test
    void onlyDetailsHasNoCount() {
        Assertions.assertFalse(Section.DETAILS.hasCount());
        Arrays.stream(Section.values()).filter(section -> section != Section.DETAILS)
                .forEach(section -> Assertions.assertTrue(section.hasCount()));
    }

    // CR-R5
    @Test
    void everyKindOfCountBelongsToItsSection() {
        Assertions.assertEquals(Section.ADDRESSES, Section.forKind(Kind.ADDRESSES));
        Assertions.assertEquals(Section.PHONE_NUMBERS, Section.forKind(Kind.PHONE_NUMBERS));
        Assertions.assertEquals(Section.EMAILS, Section.forKind(Kind.EMAILS));
        Assertions.assertEquals(Section.EMPLOYERS, Section.forKind(Kind.EMPLOYERS));
        Assertions.assertEquals(Section.RELATIONSHIPS, Section.forKind(Kind.RELATIONSHIPS));
        Assertions.assertEquals(Section.NOTES, Section.forKind(Kind.NOTES));
        Assertions.assertEquals(Section.DOCUMENTS, Section.forKind(Kind.DOCUMENTS));
        Assertions.assertEquals(Section.LOANS, Section.forKind(Kind.LOANS));
    }

    // CR-R6
    @Test
    void thePlaceholderNamesTheSectionAndItsOwner() {
        Assertions.assertEquals("Not built yet: Relationships. Owner: Yu.", Section.RELATIONSHIPS.placeholderText());
        Assertions.assertEquals("Yu", Section.RELATIONSHIPS.owner());
        Assertions.assertEquals("Not built yet: Notes. Owner: to be confirmed.", Section.NOTES.placeholderText());
    }
}
