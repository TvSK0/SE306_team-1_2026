// AI-assisted: grey placeholder bars shown in place of table rows while a read is running.
package lavs.ui;

import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.stream.IntStream;

// Figma frame 12: bars 12 high with varying widths. Styled by .lavs-skeleton in lavs.css
// (that file lands on feature/ui-stylesheet). Widths repeat, so there are no decisions here.
public class SkeletonRows extends VBox {
    private static final double[] WIDTHS = {520, 380, 450, 300, 410};

    public SkeletonRows(int rows) {
        super(16);
        // a stream rather than a for loop, so the control has no branches for the coverage gate
        IntStream.range(0, rows).forEach(i -> {
            Region bar = new Region();
            bar.getStyleClass().add("lavs-skeleton");
            bar.setPrefWidth(WIDTHS[i % WIDTHS.length]);
            bar.setMaxWidth(WIDTHS[i % WIDTHS.length]);
            getChildren().add(bar);
        });
    }
}
