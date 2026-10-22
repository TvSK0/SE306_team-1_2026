// AI-assisted: the login screen (Figma 01): a centred card with username, password and Log in.
package lavs.ui;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import lavs.logic.LoginFlow;
import lavs.logic.LoginForm;

import java.util.Map;
import java.util.function.Consumer;

// Thin control: what to show for each outcome is decided in LoginScreenState (unit tested)
// and the work is in LoginFlow. Every call goes through BackgroundTasks so the window never
// freezes. The fields are never cleared, so a failed login keeps what was typed.
public class LoginScreen {
    private static final String BUTTON_TEXT = "Log in";

    private final LoginFlow _flow;
    private final BackgroundTasks _tasks;
    private final Consumer<String> _onSignedIn;

    private final TextField _username = new TextField();
    private final PasswordField _password = new PasswordField();
    private final FormField _usernameField = new FormField("Username", _username);
    private final FormField _passwordField = new FormField("Password", _password);
    private final Banner _banner = new Banner();
    private final Button _logIn = new Button(BUTTON_TEXT);
    private final LoadingView _loading = new LoadingView(LoadingState.forSave(BUTTON_TEXT, "Logging in…"), _logIn);
    private final FieldErrorPresenter _presenter = new FieldErrorPresenter(Map.of(
            LoginForm.USERNAME, _usernameField,
            LoginForm.PASSWORD, _passwordField));

    // onSignedIn gets the signed-in user's name; the caller sets the top bar and moves on
    public LoginScreen(LoginFlow flow, BackgroundTasks tasks, Consumer<String> onSignedIn) {
        _flow = flow;
        _tasks = tasks;
        _onSignedIn = onSignedIn;
    }

    public Node create() {
        Label brand = new Label("LAVS");
        brand.getStyleClass().add("lavs-brand");
        Label subtitle = new Label("Countrywide Banking Corporation\nLoan Application and Verification System");
        subtitle.getStyleClass().add("lavs-subtitle");

        _logIn.getStyleClass().add("lavs-button-primary");
        _logIn.setMaxWidth(Double.MAX_VALUE);
        _logIn.setMinHeight(35);
        _logIn.setPrefHeight(35);
        _logIn.setOnAction(event -> logIn());
        // Enter in either field presses Log in
        _username.setOnAction(event -> _logIn.fire());
        _password.setOnAction(event -> _logIn.fire());

        Label note = new Label(LoginScreenState.CONNECTION_NOTE);
        note.getStyleClass().add("lavs-note");
        note.setWrapText(true);

        VBox card = new VBox(18, brand, subtitle, _banner, _usernameField, _passwordField, _logIn, note);
        card.getStyleClass().addAll("lavs-card", "lavs-login-card");
        card.setMinWidth(400);
        card.setMaxWidth(400);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane page = new StackPane(card);
        page.getStyleClass().add("lavs-login-page");
        page.setAlignment(Pos.CENTER);
        return page;
    }

    private void logIn() {
        _presenter.clear();
        _banner.hide();
        String username = _username.getText();
        String password = _password.getText();
        _tasks.run(() -> _flow.run(username, password), _loading::start, attempt -> {
            _loading.finish();
            LoginScreenState.Display display = LoginScreenState.show(attempt);
            _presenter.present(display.fieldErrors());
            display.banner().ifPresent(_banner::show);
            display.signedInUser().ifPresent(_onSignedIn);
        }, failure -> {
            _loading.finish();
            LoginScreenState.showUnexpected(failure).banner().ifPresent(_banner::show);
        });
    }
}
