package lavs.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Popup;
import lavs.logic.DuplicateCandidates;
import lavs.logic.SyncService;
import lavs.logic.PendingUploadsState;
import lavs.model.PendingChange;
import java.util.List;
import java.util.concurrent.CompletableFuture;

// AI-assisted: reads SQLite in the background and exposes the duplicate/rejection decisions.
public final class PendingUploadsPanel {
    private final SyncService sync;
    private final BackgroundTasks tasks;
    private final Navigator navigator;
    private final Popup popup = new Popup();
    private final VBox rows = new VBox(12);
    private final Label message = new Label();
    private BackgroundTasks.Handle loading;
    private boolean changing;

    public PendingUploadsPanel(SyncService sync, BackgroundTasks tasks, Navigator navigator) {
        this.sync = sync;
        this.tasks = tasks;
        this.navigator = navigator;
        popup.setAutoHide(true);
        message.setWrapText(true);
        VBox card = new VBox(12, new Label("Pending uploads"), message, rows);
        card.setPadding(new Insets(16));
        card.getStyleClass().add("lavs-card");
        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportWidth(440);
        scroll.setPrefViewportHeight(380);
        popup.getContent().add(scroll);
        sync.addListener(() -> Platform.runLater(() -> { if (popup.isShowing() && !changing) refresh(); }));
    }

    public void hide() { popup.hide(); if (loading != null) loading.cancel(); }

    public void toggle(Node anchor) {
        if (popup.isShowing()) { hide(); return; }
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds == null) return;
        popup.show(anchor, bounds.getMaxX() - 440, bounds.getMaxY() + 4);
        refresh();
    }

    public void refresh() {
        if (loading != null) loading.cancel();
        message.setText("Loading pending changes...");
        loading = tasks.run(sync::getChanges, changes -> {
            rows.getChildren().clear();
            for (PendingChange change : changes) rows.getChildren().add(row(change));
            message.setText(changes.isEmpty() ? "All changes uploaded" : "Changes are kept on this laptop until accepted.");
        }, error -> message.setText(ResultText.forError(error)));
    }

    private Node row(PendingChange change) {
        Label title = new Label(PendingUploadsState.descriptionFor(change));
        title.getStyleClass().add("lavs-section-title");
        Label state = new Label(PendingUploadsState.statusTextFor(change));
        Label detail = new Label(change.getStatusMessage());
        detail.setWrapText(true);
        VBox box = new VBox(6, title, state, detail);
        box.setPadding(new Insets(8));
        TextArea saved = new TextArea(change.getFields().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + java.util.Objects.toString(entry.getValue(), ""))
                .collect(java.util.stream.Collectors.joining("\n")));
        saved.setEditable(false);
        saved.setWrapText(true);
        saved.setPrefRowCount(5);
        TitledPane values = new TitledPane("Saved details", saved);
        values.setExpanded(false);
        box.getChildren().add(values);
        if (change.getCustomerNumber() != null) {
            Button customer = new Button("Open customer " + change.getCustomerNumber());
            customer.setOnAction(event -> { hide(); navigator.goTo(Screen.CUSTOMER_RECORD, change.getCustomerNumber()); });
            box.getChildren().add(customer);
        }
        List<String> candidates = DuplicateCandidates.forChange(change);
        if (!candidates.isEmpty()) {
            ComboBox<String> choice = new ComboBox<>();
            choice.getItems().setAll(candidates);
            choice.setPromptText("Choose the existing customer");
            Button use = new Button("Use existing customer");
            use.disableProperty().bind(choice.valueProperty().isNull());
            use.setOnAction(event -> action(() -> sync.resolveDuplicate(change.getId(), choice.getValue())));
            Button create = new Button("This is a new customer");
            create.setOnAction(event -> action(() -> sync.resolveDuplicate(change.getId(), null)));
            box.getChildren().addAll(choice, new HBox(8, use, create));
        }
        if (change.getStatus() == PendingChange.Status.REJECTED) {
            Button dismiss = new Button("Dismiss rejection");
            dismiss.setOnAction(event -> ConfirmDialog.ask(popup.getOwnerWindow(),
                    new ConfirmContent("Dismiss this rejection?", "The rejected change will be removed from this laptop. "
                            + "Copy or correct its details before dismissing it.", "Dismiss"),
                    () -> action(() -> sync.dismissRejected(change.getId()))));
            box.getChildren().add(dismiss);
        }
        return box;
    }

    private void action(java.util.function.Supplier<CompletableFuture<Void>> action) {
        changing = true;
        rows.setDisable(true);
        message.setText("Updating pending changes...");
        tasks.run(() -> { action.get().join(); return true; }, done -> {
            changing = false;
            rows.setDisable(false);
            refresh();
        }, error -> {
            changing = false;
            rows.setDisable(false);
            message.setText(ResultText.forError(error));
        });
    }
}
