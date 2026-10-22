package lavs.data;

import mainframe.client.Connection;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.io.IOException;

// Keeps the mainframe's transaction numbers valid for the app's one Connection.
//
// The server keeps one counter per user and rejects a request whose number is not above the
// last one it saw ("Invalid transaction number"). Two things break that: two of our threads
// sending at once on the same Connection (their numbers can arrive out of order), and anyone
// else signed in as the same user (the class shares admin), which moves the counter past ours.
//
// So this sends one request at a time, and on that rejection signs in again (which brings the
// counter up to date) and sends the request once more. The server refused the request without
// applying it, so sending it again is safe.
public class SequencedConnection implements Connection {
    static final String STALE_NUMBER = "Invalid transaction number";

    private final Connection _connection;
    // the last successful sign-in, kept to sign in again; null when signed out
    private String _username;
    private String _password;

    public SequencedConnection(Connection connection) {
        _connection = connection;
    }

    @Override
    public synchronized MessageResponse send(MessageRequest request) {
        MessageResponse response = _connection.send(request);
        if (!isStaleNumber(response.getStatus()) || !signInAgain()) {
            return response;
        }
        return _connection.send(request);
    }

    @Override
    public synchronized Status Login(String username, String password) throws IOException {
        Status status = _connection.Login(username, password);
        if (status.getWasSuccessful()) {
            _username = username;
            _password = password;
        }
        return status;
    }

    @Override
    public synchronized void Logout() {
        _username = null;
        _password = null;
        _connection.Logout();
    }

    // a ping with no transaction number, so it does not need to wait its turn
    @Override
    public boolean isConnected() {
        return _connection.isConnected();
    }

    @Override
    public void close() throws IOException {
        _connection.close();
    }

    private boolean signInAgain() {
        if (_username == null) {
            return false;
        }
        try {
            return _connection.Login(_username, _password).getWasSuccessful();
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean isStaleNumber(Status status) {
        return StatusMessage.SERVER_FAILURE.getCode().equals(status.getErrorCode())
                && status.getErrorMessage() != null
                && status.getErrorMessage().contains(STALE_NUMBER);
    }
}
