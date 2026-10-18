// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.data.ConstantsLoadResult;
import lavs.data.ConstantsSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ConstantsServiceTests {
    private static final ConstantsLoadResult RATE_TYPES = ConstantsLoadResult.success(Map.of(
            ConstantCategories.LOAN_RATE_TYPE, List.of("Fixed", "Floating", "InterestOnly")));

    @Test
    void isEmptyBeforeRefresh() {
        // Arrange
        ConstantsService service = new ConstantsService(sourceReturning(RATE_TYPES));

        // Act and Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(service.isLoaded()),
                () -> Assertions.assertEquals(List.of(), service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    @Test
    void returnsValuesAfterRefresh() {
        // Arrange
        ConstantsService service = new ConstantsService(sourceReturning(RATE_TYPES));

        // Act
        ConstantsLoadResult result = service.refresh();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.SUCCESS, result.getOutcome()),
                () -> Assertions.assertTrue(service.isLoaded()),
                () -> Assertions.assertEquals(
                        List.of("Fixed", "Floating", "InterestOnly"),
                        service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    @Test
    void returnsEmptyListForUnknownCategory() {
        // Arrange
        ConstantsService service = new ConstantsService(sourceReturning(RATE_TYPES));
        service.refresh();

        // Act and Assert
        Assertions.assertEquals(List.of(), service.getValues("NoSuchCategory"));
    }

    @Test
    void keepsPreviousValuesWhenRefreshFails() {
        // Arrange: the first refresh succeeds, the second loses the connection
        ConstantsService service = new ConstantsService(sourceReturning(
                RATE_TYPES,
                ConstantsLoadResult.networkFailure("Unavailable")));
        service.refresh();

        // Act
        service.refresh();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(service.isLoaded()),
                () -> Assertions.assertEquals(
                        List.of("Fixed", "Floating", "InterestOnly"),
                        service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    @Test
    void returnsFailureFromRefresh() {
        // Arrange
        ConstantsService service = new ConstantsService(sourceReturning(
                ConstantsLoadResult.rejected("Invalid page size")));

        // Act
        ConstantsLoadResult result = service.refresh();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.REJECTED, result.getOutcome()),
                () -> Assertions.assertEquals("Invalid page size", result.getMessage())
        );
    }

    @Test
    void staysNotLoadedWhenFirstRefreshFails() {
        // Arrange
        ConstantsService service = new ConstantsService(sourceReturning(
                ConstantsLoadResult.networkFailure("Unavailable")));

        // Act
        service.refresh();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(service.isLoaded()),
                () -> Assertions.assertEquals(List.of(), service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    @Test
    void returnedValuesCannotBeModified() {
        // Arrange: the source hands over a list it could still change
        List<String> sourceValues = new ArrayList<>(List.of("Fixed", "Floating"));
        ConstantsService service = new ConstantsService(sourceReturning(ConstantsLoadResult.success(Map.of(
                ConstantCategories.LOAN_RATE_TYPE, sourceValues))));
        service.refresh();

        // Act
        List<String> values = service.getValues(ConstantCategories.LOAN_RATE_TYPE);
        sourceValues.add("InterestOnly");

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertThrows(UnsupportedOperationException.class, () -> values.add("Other")),
                () -> Assertions.assertEquals(
                        List.of("Fixed", "Floating"),
                        service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    // a source that returns the given results in turn, one per refresh
    private static ConstantsSource sourceReturning(ConstantsLoadResult... results) {
        Queue<ConstantsLoadResult> remaining = new ArrayDeque<>(Arrays.asList(results));
        return () -> remaining.remove();
    }
}
