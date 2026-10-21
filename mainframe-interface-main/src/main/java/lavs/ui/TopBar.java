// AI-assisted: the dark bar across the top: LAVS, screen title, status pill, user and Log out.
package lavs.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

// Thin control: the text and styles come from TopBarState (unit tested). Styled by the
// lavs-topbar and lavs-status-* classes in lavs.css (Figma: padding 12x24, gap 20).
public class TopBar extends HBox {
    private final TopBarState _state;
    private final Label _title = new Label();
    private final HBox _statusPill = new HBox(8);
    private final Label _statusLabel = new Label();
    private final Label _statusDetail = new Label();
    private final Label _user = new Label();
    private final Button _logOut = new Button("Log out");

    // uiThread runs a Runnable on the JavaFX thread (Platform::runLater in the app), because
    // the StatusFeed can report a change from the sync thread
    public TopBar(StatusFeed feed, Consumer<Runnable> uiThread) {
        super(20);
        _state = new TopBarState(feed);

        Label brand = new Label("LAVS");
        brand.getStyleClass().add("lavs-topbar-brand");
        _title.getStyleClass().add("lavs-topbar-title");
        // the title takes the free space, pushing the rest to the right
        Region grow = new Region();
        HBox.setHgrow(grow, Priority.ALWAYS);

        Region dot = new Region();
        dot.getStyleClass().add("lavs-status-dot");
        _statusLabel.getStyleClass().add("lavs-status-label");
        _statusDetail.getStyleClass().add("lavs-status-detail");
        _statusPill.setAlignment(Pos.CENTER_LEFT);
        _statusPill.getChildren().addAll(dot, _statusLabel, _statusDetail);

        _user.getStyleClass().add("lavs-topbar-user");
        _logOut.getStyleClass().add("lavs-topbar-logout");

        getStyleClass().add("lavs-topbar");
        setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(brand, _title, grow, _statusPill, _user, _logOut);

        refresh();
        feed.onChange(() -> uiThread.accept(this::refresh));
    }

    public void setOnLogOut(Runnable action) {
        _logOut.setOnAction(event -> action.run());
    }

    // Aum hangs the pending-uploads panel (US11) on this
    public void setOnStatusClicked(Runnable action) {
        _statusPill.setOnMouseClicked(event -> action.run());
    }

    public void setTitle(String title) {
        _state.setTitle(title);
        refresh();
    }

    public void setUserName(String userName) {
        _state.setUserName(userName);
        refresh();
    }

    // Copies the state onto the labels. Call on the UI thread.
    public void refresh() {
        _title.setText(_state.getTitle());
        _statusLabel.setText(_state.getStatusLabel());
        _statusDetail.setText(_state.getStatusDetail());
        _statusPill.getStyleClass().setAll("lavs-status-pill", _state.getStatusStyle());
        _user.setText(_state.getUserText());
    }
}
