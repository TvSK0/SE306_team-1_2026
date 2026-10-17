package mainframe.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// A test Connection that replies based on what is asked. Unlike StubConnection,
// it can return different responses for different requests, simulate rejections
// and network failures, and records every request it receives.
public class FakeConnection implements Connection {
    private final List<Rule> _rules = new ArrayList<>();
    private final List<MessageRequest> _sentRequests = new ArrayList<>();
    private boolean _isConnected = true;
    private Status _loginStatus = new Status(1);

    // replies with the response for any request of this type
    public void whenRequested(String requestType, MessageResponse response) {
        whenRequested(requestType, Map.of(), response);
    }

    // replies with the response when the request type matches and the request
    // contains every given value, e.g. Map.of("category", "7", "page", "1")
    public void whenRequested(String requestType, Map<String, String> values, MessageResponse response) {
        _rules.add(new Rule(requestType, values, response));
    }

    public List<MessageRequest> getSentRequests() {
        return List.copyOf(_sentRequests);
    }

    // AI-assisted: toggle this mid-test to simulate the connection dropping out.
    // While offline, send() returns networkDown() instead of consulting the
    // scripted rules below - call setIsConnected(true) again to "reconnect".
    public void setIsConnected(boolean isConnected) {
        _isConnected = isConnected;
    }

    public void setLoginStatus(Status status) {
        _loginStatus = status;
    }

    @Override
    public MessageResponse send(MessageRequest request) {
        _sentRequests.add(request);
        // AI-assisted: the one behaviour this card adds - previously
        // setIsConnected(false) only changed what isConnected() reported;
        // send() ignored it and kept returning scripted responses.
        if (!_isConnected) {
            return networkDown();
        }
        for (Rule rule : _rules) {
            if (rule.matches(request)) return rule.response;
        }
        throw new IllegalStateException("No response scripted for " + describe(request));
    }

    @Override
    public Status Login(String username, String password) throws IOException {
        return _loginStatus;
    }

    @Override
    public void Logout() {
        // Don't do anything
    }

    @Override
    public boolean isConnected() {
        return _isConnected;
    }

    @Override
    public void close() throws IOException {
        // Don't do anything
    }

    // a successful list response; items get ids and orders firstId, firstId + 1, ...
    // so a later page continues the numbering of the page before it
    public static MessageResponse list(long total, long firstId, String... names) {
        HashMap<String, String> values = new HashMap<>();
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(names.length));
        for (int i = 1; i <= names.length; i++) {
            String number = Long.toString(firstId + i - 1);
            values.put(i + ":id", number);
            values.put(i + ":name", names[i - 1]);
            values.put(i + ":order", number);
        }
        return success(values);
    }

    // a successful single-page list response with ids starting at 1
    public static MessageResponse list(String... names) {
        return list(names.length, 1, names);
    }

    public static MessageResponse success(Map<String, String> values) {
        return new MessageResponse(new Status(1), new HashMap<>(values));
    }

    // the response HttpConnection returns when the server rejects a message (HTTP 400)
    public static MessageResponse rejected(String message) {
        return new MessageResponse(
                new Status(StatusMessage.SERVER_FAILURE.getCode(), message, 1),
                new HashMap<>());
    }

    // the response HttpConnection returns when the server cannot be reached
    public static MessageResponse networkDown() {
        return StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1);
    }

    private static String describe(MessageRequest request) {
        StringBuilder text = new StringBuilder(request.getRequestType());
        for (String key : request.getValueKeys()) {
            text.append(' ').append(key).append('=').append(request.getValue(key));
        }
        return text.toString();
    }

    private record Rule(String requestType, Map<String, String> values, MessageResponse response) {
        boolean matches(MessageRequest request) {
            if (!requestType.equals(request.getRequestType())) return false;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                if (!entry.getValue().equals(request.getValue(entry.getKey()))) return false;
            }
            return true;
        }
    }
}
