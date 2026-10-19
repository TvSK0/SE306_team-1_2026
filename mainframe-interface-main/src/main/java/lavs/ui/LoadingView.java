// AI-assisted: copies a LoadingState onto an action button and the nodes shown while a read runs.
package lavs.ui;

import javafx.scene.Node;
import javafx.scene.control.Button;

import java.util.Arrays;
import java.util.List;

// Thin control: the decisions live in LoadingState (unit tested). A screen calls start()
// when it begins a call and finish() from the result or error callback.
public class LoadingView {
    private final LoadingState _state;
    private final Button _actionButton;
    private final List<Node> _shownWhileReading;

    // shownWhileReading is the Cancel button and the skeleton rows; pass none for a save
    public LoadingView(LoadingState state, Button actionButton, Node... shownWhileReading) {
        _state = state;
        _actionButton = actionButton;
        _shownWhileReading = Arrays.asList(shownWhileReading);
        show();
    }

    public void start() {
        _state.start();
        show();
    }

    public void finish() {
        _state.finish();
        show();
    }

    private void show() {
        _actionButton.setText(_state.getButtonText());
        _actionButton.setDisable(_state.isButtonDisabled());
        // not managed when hidden, so the layout does not leave a gap
        _shownWhileReading.forEach(node -> {
            node.setVisible(_state.isReadInProgress());
            node.setManaged(_state.isReadInProgress());
        });
    }
}
