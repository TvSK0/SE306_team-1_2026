// AI-assisted.
package lavs.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import lavs.logic.SyncService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

// The floating panel that pops up when the user clicks the status in the top bar
// This lists each queued change with it's description and upload status
// Is wired in LavsApp  topBar.setOnStatusClicked(() -> panel.toggle(topBar))
// It's styled by the lavs-pending-* classes in lavs.css
// AI-assisted: a possible-duplicate new customer offers "Use existing" / "It's a new customer",
// and a rejected change offers "Dismiss" (US11)
public class PendingUploadsPanel {
    private final PendingUploadsState _state;
    private final Popup _popup = new Popup();
    private final VBox _rows = new VBox(0);

    public PendingUploadsPanel(SyncService sync) {
        _state = new PendingUploadsState(sync);
        _popup.setAutoHide(true);

        Label header = new Label("Pending uploads");
        header.getStyleClass().add("lavs-pending-header");

        _rows.getStyleClass().add("lavs-pending-list");

        VBox card = new VBox(0, header, _rows);
        card.getStyleClass().add("lavs-pending-panel");

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setMaxHeight(400);
        scroll.setPrefWidth(320);
        scroll.getStyleClass().add("lavs-pending-scroll");

        _popup.getContent().add(scroll);
    }

    // Toggles the panel open or closed, positioned below the right edge of the given node.
    public void toggle(Node anchor) {
        if (_popup.isShowing()) {
            _popup.hide();
        } else {
            refresh();
            var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
            if (bounds != null) {
                _popup.show(anchor, bounds.getMaxX() - 320, bounds.getMaxY() + 4);
            }
        }
    }

    // refreshes the queue n rebuilds the row list. is called each time the panel is opened.
    public void refresh() {
        _rows.getChildren().clear();
        List<PendingUploadsState.Row> rows = _state.getRows();
        if (rows.isEmpty()) {
            Label empty = new Label("All changes uploaded");
            empty.getStyleClass().add("lavs-pending-empty");
            empty.setPadding(new Insets(12));
            _rows.getChildren().add(empty);
            return;
        }
        for (PendingUploadsState.Row row : rows) {
            _rows.getChildren().add(buildRow(row));
        }
    }

    private Node buildRow(PendingUploadsState.Row row) {
        Label description = new Label(row.getDescription());
        description.getStyleClass().add("lavs-pending-description");

        String detailText = row.getCustomerNumber() == null || row.getCustomerNumber().isBlank()
                ? row.getStatusText()
                : row.getCustomerNumber() + " · " + row.getStatusText();
        Label detail = new Label(detailText);
        detail.getStyleClass().add("lavs-pending-detail");

        VBox rowBox = new VBox(2, description, detail);
        // AI-assisted: the MMM needs to know why a change is paused or was rejected (US11)
        if (row.getReason() != null) {
            Label reason = new Label(row.getReason());
            reason.getStyleClass().add("lavs-pending-detail");
            reason.setWrapText(true);
            rowBox.getChildren().add(reason);
        }
        HBox actions = actionsFor(row);
        if (!actions.getChildren().isEmpty()) {
            rowBox.getChildren().add(actions);
        }
        rowBox.getStyleClass().add("lavs-pending-row");
        rowBox.setPadding(new Insets(8, 12, 8, 12));
        return rowBox;
    }

    // AI-assisted: the buttons for what the MMM can do about this change, if anything (US11)
    private HBox actionsFor(PendingUploadsState.Row row) {
        HBox actions = new HBox(6);
        if (row.canResolveDuplicate()) {
            for (String existing : row.getDuplicateMatches()) {
                actions.getChildren().add(actionButton("Use existing " + existing,
                        () -> _state.useExistingCustomer(row, existing)));
            }
            actions.getChildren().add(actionButton("It's a new customer", () -> _state.confirmNewCustomer(row)));
        }
        if (row.canDismiss()) {
            actions.getChildren().add(actionButton("Dismiss", () -> _state.dismiss(row)));
        }
        return actions;
    }

    // AI-assisted: runs the action on the sync thread, then shows the queue again. The future
    // completes on the sync thread, so it hops back to the JavaFX thread before touching the panel.
    private Button actionButton(String text, Supplier<CompletableFuture<Void>> action) {
        Button button = new Button(text);
        button.getStyleClass().add("lavs-button-secondary");
        button.setOnAction(event -> {
            button.setDisable(true);
            action.get().whenComplete((done, failure) -> Platform.runLater(this::refresh));
        });
        return button;
    }
}
