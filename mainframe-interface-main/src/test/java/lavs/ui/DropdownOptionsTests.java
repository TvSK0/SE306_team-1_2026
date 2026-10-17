// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.data.ConstantsLoadResult;
import lavs.logic.ConstantCategories;
import lavs.logic.ConstantsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class DropdownOptionsTests {
    private static final String RATE_TYPE = ConstantCategories.LOAN_RATE_TYPE;

    @Test
    void listsTheCategoryValuesInOrderForANewRecord() {
        // Arrange
        ConstantsService service = loadedService(List.of("Fixed", "Floating", "InterestOnly"));

        // Act
        DropdownOptions options = DropdownOptions.forCategory(service, RATE_TYPE, null);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of("Fixed", "Floating", "InterestOnly"), options.getItems()),
                () -> Assertions.assertNull(options.getSelected()),
                () -> Assertions.assertEquals(DropdownOptions.CHOOSE_PROMPT, options.getPromptText()),
                () -> Assertions.assertTrue(options.isEnabled())
        );
    }

    @Test
    void selectsTheSavedValueWhenEditing() {
        // Arrange
        ConstantsService service = loadedService(List.of("Fixed", "Floating"));

        // Act
        DropdownOptions options = DropdownOptions.forCategory(service, RATE_TYPE, "Floating");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of("Fixed", "Floating"), options.getItems()),
                () -> Assertions.assertEquals("Floating", options.getSelected())
        );
    }

    @Test
    void keepsASavedValueThatIsNoLongerInTheList() {
        // Arrange
        ConstantsService service = loadedService(List.of("Fixed", "Floating"));

        // Act
        DropdownOptions options = DropdownOptions.forCategory(service, RATE_TYPE, "Capped");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of("Fixed", "Floating", "Capped"), options.getItems()),
                () -> Assertions.assertEquals("Capped", options.getSelected()),
                () -> Assertions.assertTrue(options.isEnabled())
        );
    }

    @Test
    void treatsABlankSavedValueAsNoSelection() {
        // Arrange
        ConstantsService service = loadedService(List.of("Fixed"));

        // Act
        DropdownOptions options = DropdownOptions.forCategory(service, RATE_TYPE, "  ");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of("Fixed"), options.getItems()),
                () -> Assertions.assertNull(options.getSelected())
        );
    }

    @Test
    void isDisabledBeforeTheValuesHaveLoaded() {
        // Arrange
        ConstantsService service = new ConstantsService(() -> ConstantsLoadResult.networkFailure("offline"));
        service.refresh();

        // Act
        DropdownOptions options = DropdownOptions.forCategory(service, RATE_TYPE, null);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(), options.getItems()),
                () -> Assertions.assertEquals(DropdownOptions.NOT_LOADED_PROMPT, options.getPromptText()),
                () -> Assertions.assertFalse(options.isEnabled())
        );
    }

    @Test
    void stillShowsTheSavedValueBeforeTheValuesHaveLoaded() {
        // Arrange
        ConstantsService service = new ConstantsService(() -> ConstantsLoadResult.networkFailure("offline"));

        // Act
        DropdownOptions options = DropdownOptions.forCategory(service, RATE_TYPE, "Fixed");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of("Fixed"), options.getItems()),
                () -> Assertions.assertEquals("Fixed", options.getSelected()),
                () -> Assertions.assertTrue(options.isEnabled())
        );
    }

    @Test
    void isDisabledWhenTheCategoryHasNoValues() {
        // Arrange
        ConstantsService service = loadedService(List.of("Fixed"));

        // Act
        DropdownOptions options = DropdownOptions.forCategory(service, "NoSuchCategory", null);

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(List.of(), options.getItems()),
                () -> Assertions.assertEquals(DropdownOptions.NO_VALUES_PROMPT, options.getPromptText()),
                () -> Assertions.assertFalse(options.isEnabled())
        );
    }

    @Test
    void itemsCannotBeModified() {
        // Arrange
        DropdownOptions options = DropdownOptions.forCategory(loadedService(List.of("Fixed")), RATE_TYPE, "Capped");

        // Act and Assert
        Assertions.assertThrows(UnsupportedOperationException.class, () -> options.getItems().add("Floating"));
    }

    private static ConstantsService loadedService(List<String> rateTypes) {
        ConstantsService service = new ConstantsService(
                () -> ConstantsLoadResult.success(Map.of(RATE_TYPE, rateTypes)));
        service.refresh();
        return service;
    }
}
