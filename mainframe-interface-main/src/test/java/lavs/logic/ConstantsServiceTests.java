// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.data.CategoryConstantStore;
import lavs.data.ConstantsCache;
import lavs.data.ConstantsLoadResult;
import lavs.data.ConstantsSource;
import lavs.data.LocalDatabase;
import lavs.data.LocalStoreException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
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

    // --- saved copy on this laptop ---

    @Test
    void loadSavedFillsTheValuesFromTheCache() {
        // Arrange
        FakeCache cache = new FakeCache(Map.of(ConstantCategories.LOAN_RATE_TYPE, List.of("Fixed", "Floating")));
        ConstantsService service = new ConstantsService(sourceReturning(), cache);

        // Act
        boolean loaded = service.loadSaved();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(loaded),
                () -> Assertions.assertTrue(service.isLoaded()),
                () -> Assertions.assertEquals(List.of("Fixed", "Floating"),
                        service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    @Test
    void loadSavedWithNothingSavedStaysEmpty() {
        // Arrange
        ConstantsService service = new ConstantsService(sourceReturning(), new FakeCache(Map.of()));

        // Act
        boolean loaded = service.loadSaved();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(loaded),
                () -> Assertions.assertFalse(service.isLoaded())
        );
    }

    @Test
    void loadSavedDoesNotReplaceValuesFromTheMainframe() {
        // Arrange
        FakeCache cache = new FakeCache(Map.of(ConstantCategories.LOAN_RATE_TYPE, List.of("Old")));
        ConstantsService service = new ConstantsService(sourceReturning(RATE_TYPES), cache);
        service.refresh();

        // Act
        boolean loaded = service.loadSaved();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(loaded),
                () -> Assertions.assertEquals(List.of("Fixed", "Floating", "InterestOnly"),
                        service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    @Test
    void refreshSavesTheValuesToTheCache() {
        // Arrange
        FakeCache cache = new FakeCache(Map.of());
        ConstantsService service = new ConstantsService(sourceReturning(RATE_TYPES), cache);

        // Act
        service.refresh();

        // Assert
        Assertions.assertEquals(List.of("Fixed", "Floating", "InterestOnly"),
                cache._saved.get(ConstantCategories.LOAN_RATE_TYPE));
    }

    @Test
    void failedRefreshKeepsTheSavedValuesAndSavesNothing() {
        // Arrange
        FakeCache cache = new FakeCache(Map.of(ConstantCategories.LOAN_RATE_TYPE, List.of("Fixed")));
        ConstantsService service = new ConstantsService(
                sourceReturning(ConstantsLoadResult.networkFailure("offline")), cache);
        service.loadSaved();

        // Act
        ConstantsLoadResult result = service.refresh();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.NETWORK_FAILURE, result.getOutcome()),
                () -> Assertions.assertEquals(List.of("Fixed"), service.getValues(ConstantCategories.LOAN_RATE_TYPE)),
                () -> Assertions.assertEquals(0, cache._saveCount)
        );
    }

    @Test
    void cacheThatCannotBeWrittenDoesNotFailTheRefresh() {
        // Arrange
        FakeCache cache = new FakeCache(Map.of());
        cache._broken = true;
        ConstantsService service = new ConstantsService(sourceReturning(RATE_TYPES), cache);

        // Act
        ConstantsLoadResult result = service.refresh();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(ConstantsLoadResult.Outcome.SUCCESS, result.getOutcome()),
                () -> Assertions.assertEquals(List.of("Fixed", "Floating", "InterestOnly"),
                        service.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    @Test
    void cacheThatCannotBeReadLeavesTheServiceEmpty() {
        // Arrange
        FakeCache cache = new FakeCache(Map.of(ConstantCategories.LOAN_RATE_TYPE, List.of("Fixed")));
        cache._broken = true;
        ConstantsService service = new ConstantsService(sourceReturning(), cache);

        // Act
        boolean loaded = service.loadSaved();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertFalse(loaded),
                () -> Assertions.assertFalse(service.isLoaded())
        );
    }

    @Test
    void valuesSavedToTheLocalDatabaseComeBackAfterARestart(@TempDir Path folder) {
        // Arrange: one run of the app loads from the mainframe and saves to the SQLite file
        LocalDatabase database = new LocalDatabase(folder.resolve("lavs.db"));
        new ConstantsService(sourceReturning(RATE_TYPES), new CategoryConstantStore(database)).refresh();
        ConstantsService nextRun = new ConstantsService(sourceReturning(), new CategoryConstantStore(database));

        // Act
        boolean loaded = nextRun.loadSaved();

        // Assert
        Assertions.assertAll(
                () -> Assertions.assertTrue(loaded),
                () -> Assertions.assertEquals(List.of("Fixed", "Floating", "InterestOnly"),
                        nextRun.getValues(ConstantCategories.LOAN_RATE_TYPE))
        );
    }

    // an in-memory cache; _broken makes both methods fail like a locked SQLite file
    private static final class FakeCache implements ConstantsCache {
        private Map<String, List<String>> _saved;
        private int _saveCount;
        private boolean _broken;

        FakeCache(Map<String, List<String>> saved) {
            _saved = saved;
        }

        @Override
        public void saveAll(Map<String, List<String>> values) {
            if (_broken) throw new LocalStoreException("locked", null);
            _saved = values;
            _saveCount++;
        }

        @Override
        public Map<String, List<String>> loadAll() {
            if (_broken) throw new LocalStoreException("locked", null);
            return _saved;
        }
    }

    // a source that returns the given results in turn, one per refresh
    private static ConstantsSource sourceReturning(ConstantsLoadResult... results) {
        Queue<ConstantsLoadResult> remaining = new ArrayDeque<>(Arrays.asList(results));
        return () -> remaining.remove();
    }
}
