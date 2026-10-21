// AI-assisted: the window layout: top bar above the screen the Navigator is showing.
package lavs.ui;

import javafx.scene.layout.BorderPane;

// Thin control. What the top bar shows for each screen is decided in TopBarState (unit tested).
public class MainWindow extends BorderPane {
    private final TopBar _topBar;

    public MainWindow(Navigator navigator, TopBar topBar) {
        _topBar = topBar;
        setTop(topBar);
        setCenter(navigator.getContent());
        navigator.setOnScreenChanged(this::onScreenChanged);
    }

    // screens and the login flow use this to set the user name and the title
    public TopBar getTopBar() {
        return _topBar;
    }

    // Runs each time the Navigator shows a screen. A screen that wants a more specific title
    // (the customer record) sets it on the top bar after this.
    private void onScreenChanged(Screen screen) {
        _topBar.setTitle(TopBarState.defaultTitleFor(screen));
        boolean shown = TopBarState.isShownOn(screen);
        _topBar.setVisible(shown);
        // not managed when hidden, so the login card is centred without an empty strip above it
        _topBar.setManaged(shown);
    }
}
