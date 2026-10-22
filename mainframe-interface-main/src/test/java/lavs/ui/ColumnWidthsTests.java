// AI-assisted: tests the column width shares used to line up a table's header and rows.
package lavs.ui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColumnWidthsTests {
    @Test
    void weightsBecomeTheirSharesOfOneHundredPercent() {
        assertEquals(List.of(60.0, 20.0, 20.0), ColumnWidths.percentages(List.of(3.0, 1.0, 1.0)));
    }

    @Test
    void equalWeightsGiveEqualColumns() {
        assertEquals(List.of(25.0, 25.0, 25.0, 25.0), ColumnWidths.percentages(List.of(1.0, 1.0, 1.0, 1.0)));
    }

    @Test
    void aSingleColumnTakesTheWholeWidth() {
        assertEquals(List.of(100.0), ColumnWidths.percentages(List.of(2.0)));
    }

    @Test
    void theSharesAddUpToOneHundred() {
        double sum = ColumnWidths.percentages(List.of(2.0, 3.0, 1.5, 2.0)).stream().mapToDouble(Double::doubleValue).sum();

        assertEquals(100.0, sum, 1e-9);
    }

    @Test
    void noColumnsGivesNoWidths() {
        assertTrue(ColumnWidths.percentages(List.of()).isEmpty());
    }
}
