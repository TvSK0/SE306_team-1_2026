// AI-assisted: tests for which view builds each customer record section.
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SectionRegistryTests {
    private static final SectionContext CONTEXT = new SectionContext("C-1", () -> null, section -> { }, () -> { });

    // CR-R6
    @Test
    void aRegisteredSectionIsBuiltByItsFactoryWithTheContext() {
        // Arrange
        SectionRegistry<String> registry = new SectionRegistry<>(section -> "placeholder " + section.label());
        registry.register(Section.NOTES, context -> "notes for " + context.customerNumber());

        // Act and Assert
        Assertions.assertEquals("notes for C-1", registry.create(Section.NOTES, CONTEXT));
    }

    // CR-R6 (a section nobody has built yet shows a placeholder naming its owner)
    @Test
    void anUnregisteredSectionShowsThePlaceholder() {
        // Arrange
        SectionRegistry<String> registry = new SectionRegistry<>(Section::placeholderText);

        // Act and Assert
        Assertions.assertEquals("Not built yet: Notes. Owner: to be confirmed.",
                registry.create(Section.NOTES, CONTEXT));
    }

    // CR-R6
    @Test
    void registeringASectionAgainReplacesTheEarlierFactory() {
        // Arrange
        SectionRegistry<String> registry = new SectionRegistry<>(Section::placeholderText);
        registry.register(Section.LOANS, context -> "first");
        registry.register(Section.LOANS, context -> "second");

        // Act and Assert
        Assertions.assertEquals("second", registry.create(Section.LOANS, CONTEXT));
    }
}
