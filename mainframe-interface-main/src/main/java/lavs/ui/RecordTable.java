// AI-assisted: a table of records with columns, row actions and an empty state.
package lavs.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

// Thin control (Figma frames 02, 03a, 09, 11). The header stays fixed above a ScrollPane that
// holds the rows, because a search can return many of them. Column widths come from
// ColumnWidths (unit tested). Delete always goes through ConfirmDialog, and the delete
// handler only runs once the user confirms, so a screen cannot skip the confirmation.
// The "+ Add X" button is not part of the table: it goes in the section heading row
// (SectionHeading.setAction), as in every Figma frame.
//
// Set the columns and actions first, then call setRows(...) whenever the data changes:
//   table.column("Loan number", Loan::number, 2).column(...).rowAction("Open loan", this::open);
public class RecordTable<T> extends VBox {
    // the Open / Select / Edit / Delete links share one column; its weight is relative to the data columns
    private static final double ACTIONS_WEIGHT = 2;

    private record Column<T>(String header, Function<T, Node> cell, double weight) {
    }

    private record RowAction<T>(String label, boolean danger, Consumer<T> handler) {
    }

    private final List<Column<T>> _columns = new ArrayList<>();
    private final List<RowAction<T>> _actions = new ArrayList<>();
    private final VBox _table = new VBox();
    private final VBox _rows = new VBox();
    private final ScrollPane _scroll = new ScrollPane(_rows);
    private final Label _empty = new Label();
    private List<T> _items = List.of();
    private String _deleteThing;
    private Consumer<T> _onDelete;

    public RecordTable() {
        super(12);
        _table.getStyleClass().add("lavs-table");
        _scroll.getStyleClass().add("lavs-table-scroll");
        _scroll.setFitToWidth(true);
        _scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(_scroll, Priority.ALWAYS);
        VBox.setVgrow(_table, Priority.ALWAYS);
        _empty.getStyleClass().add("lavs-table-empty");
        _empty.setWrapText(true);

        getChildren().add(_table);
    }

    // A column of plain text. weight is its share of the width, e.g. 2 is twice as wide as 1.
    public RecordTable<T> column(String header, Function<T, String> text, double weight) {
        return nodeColumn(header, item -> {
            Label label = new Label(text.apply(item));
            label.getStyleClass().add("lavs-table-cell");
            label.setWrapText(true);
            return label;
        }, weight);
    }

    // A column whose cell is any node, e.g. a Tag.
    public RecordTable<T> nodeColumn(String header, Function<T, Node> cell, double weight) {
        _columns.add(new Column<>(header, cell, weight));
        return this;
    }

    // A blue link at the end of each row: "Open record", "Select", "Edit".
    public RecordTable<T> rowAction(String label, Consumer<T> handler) {
        _actions.add(new RowAction<>(label, false, handler));
        return this;
    }

    // A red "Delete" link. thing names what is deleted ("document") for the confirmation.
    public RecordTable<T> deleteAction(String thing, Consumer<T> onConfirmed) {
        _deleteThing = thing;
        _onDelete = onConfirmed;
        _actions.add(new RowAction<>("Delete", true, this::confirmDelete));
        return this;
    }

    // shown instead of the table when there are no rows, e.g. "No documents yet."
    public void setEmptyText(String text) {
        _empty.setText(text);
    }

    public void setRows(List<T> items) {
        _items = List.copyOf(items);
        redraw();
    }

    private void confirmDelete(T item) {
        ConfirmDialog.ask(getScene().getWindow(), ConfirmContent.delete(_deleteThing), () -> _onDelete.accept(item));
    }

    private void redraw() {
        if (_items.isEmpty()) {
            _table.getChildren().setAll(_empty);
            return;
        }
        List<Double> weights = new ArrayList<>(_columns.stream().map(Column::weight).toList());
        weights.add(ACTIONS_WEIGHT);
        List<ColumnConstraints> widths = ColumnWidths.percentages(weights).stream().map(percent -> {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(percent);
            return constraints;
        }).toList();

        GridPane header = grid(widths, "lavs-table-header");
        for (int i = 0; i < _columns.size(); i++) {
            Label label = new Label(_columns.get(i).header());
            label.getStyleClass().add("lavs-table-header-cell");
            header.add(label, i, 0);
        }

        _rows.getChildren().setAll(_items.stream().map(item -> row(item, widths)).toList());
        _table.getChildren().setAll(header, _scroll);
    }

    private GridPane row(T item, List<ColumnConstraints> widths) {
        GridPane row = grid(widths, "lavs-table-row");
        for (int i = 0; i < _columns.size(); i++) {
            row.add(_columns.get(i).cell().apply(item), i, 0);
        }
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        _actions.forEach(action -> {
            Button link = new Button(action.label());
            link.getStyleClass().addAll(action.danger() ? "lavs-button-danger-link" : "lavs-button-link", "lavs-row-action");
            link.setOnAction(event -> action.handler().accept(item));
            actions.getChildren().add(link);
        });
        row.add(actions, _columns.size(), 0);
        return row;
    }

    private static GridPane grid(List<ColumnConstraints> widths, String styleClass) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add(styleClass);
        grid.getColumnConstraints().setAll(widths);
        grid.setHgap(8);
        return grid;
    }
}
