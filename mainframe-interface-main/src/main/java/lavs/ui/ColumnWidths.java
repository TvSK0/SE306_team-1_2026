// AI-assisted: turns column weights into percentage widths so the header and rows line up.
package lavs.ui;

import java.util.List;

public final class ColumnWidths {
    private ColumnWidths() {
    }

    // Each weight's share of the total, as a percentage: weights 3, 1, 1 give 60, 20, 20.
    // The weights should be positive; the result always adds up to 100 (or is empty).
    public static List<Double> percentages(List<Double> weights) {
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();
        return weights.stream().map(weight -> weight * 100 / total).toList();
    }
}
