// AI-assisted: table columns that show a Tag, or a link that acts on the row.
package lavs.ui;

import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.util.function.Consumer;
import java.util.function.Function;

// Thin JavaFX helpers next to TableColumns, for the Figma tables: a status or role column drawn
// as a Tag, and a row link such as "Open record" (Figma 02, 03a).
final class TableCells {
    private TableCells() {
    }

    static <T> void addTag(TableView<T> table, String title, Function<T, CustomerSearchText.StatusTag> tag) {
        TableColumn<T, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(unused -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : tagFor(tag.apply(item)));
            }
        });
        column.setPrefWidth(160);
        table.getColumns().add(column);
    }

    static <T> void addLink(TableView<T> table, String label, Consumer<T> action) {
        TableColumn<T, T> column = new TableColumn<>("");
        column.setCellValueFactory(cell -> new javafx.beans.property.ReadOnlyObjectWrapper<>(cell.getValue()));
        column.setCellFactory(unused -> new TableCell<>() {
            private final Button link = new Button(label);

            {
                link.getStyleClass().addAll("lavs-button-link", "lavs-row-action");
                link.setOnAction(event -> action.accept(getItem()));
            }

            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(null);
                setGraphic(empty || item == null ? null : link);
            }
        });
        column.setSortable(false);
        column.setPrefWidth(140);
        table.getColumns().add(column);
    }

    private static Tag tagFor(CustomerSearchText.StatusTag tag) {
        return new Tag(tag.variant(), tag.text());
    }
}
