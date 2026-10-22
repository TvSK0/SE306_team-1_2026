// AI-assisted: a label, any input control and an inline error message, as one form field.
package lavs.ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

// Thin control. Which fields get an error, and with what text, is decided by FieldErrorPresenter
// (unit tested). Styled by lavs-field-label, lavs-field-error and lavs-error in lavs.css:
// an error turns the label red and gives the input the danger border and background.
public class FormField extends VBox implements FieldView {
    private static final String INPUT_CLASS = "lavs-input";
    private static final String ERROR_CLASS = "lavs-error";

    private final Label _label = new Label();
    private final Label _error = new Label();
    private final Node _input;

    // Gives the input the lavs-input look (size, border, error state).
    public FormField(String labelText, Node input) {
        this(labelText, input, true);
    }

    // Pass styleInput = false for something that is not an input, e.g. a read-only label.
    public FormField(String labelText, Node input, boolean styleInput) {
        super(4);
        _input = input;
        if (styleInput) {
            // remove first so a node that already has the class does not get it twice
            input.getStyleClass().remove(INPUT_CLASS);
            input.getStyleClass().add(INPUT_CLASS);
        }
        _label.setText(labelText);
        _label.getStyleClass().add("lavs-field-label");
        _error.getStyleClass().add("lavs-field-error");
        _error.setWrapText(true);
        getChildren().addAll(_label, input, _error);
        clearError();
    }

    public Node getInput() {
        return _input;
    }

    @Override
    public void setError(String message) {
        clearError();
        _error.setText(message);
        _error.setVisible(true);
        _error.setManaged(true);
        _label.getStyleClass().add(ERROR_CLASS);
        _input.getStyleClass().add(ERROR_CLASS);
    }

    @Override
    public void clearError() {
        _error.setText("");
        // not managed when empty, so the field does not keep a gap for a message that is not there
        _error.setVisible(false);
        _error.setManaged(false);
        _label.getStyleClass().remove(ERROR_CLASS);
        _input.getStyleClass().remove(ERROR_CLASS);
    }
}
