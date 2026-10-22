package lavs.ui;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import java.util.function.Function;

// AI-assisted: consistent text columns for customer, loan and employer lists.
final class TableColumns {
    private TableColumns() {}
    static <T> void add(TableView<T> table, String title, Function<T, ?> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                java.util.Objects.toString(value.apply(cell.getValue()), "")));
        column.setPrefWidth(160);
        table.getColumns().add(column);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }
}
