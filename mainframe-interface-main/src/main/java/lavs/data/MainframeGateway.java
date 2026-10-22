package lavs.data;

import lavs.model.PendingChange;
import mainframe.client.Connection;
import mainframe.client.Message;
import mainframe.client.MessageRequest;
import mainframe.client.MessageResponse;
import mainframe.client.messages.customer.Search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// AI-assisted: the one place the app talks to the mainframe. It wraps the Connection and
// turns every answer into a GatewayResult (success, network failure or rejected) instead
// of throwing, so each service has to handle all three. Services get it through their
// constructor, so their tests can pass one built on a FakeConnection.
//
// Login goes through the Connection, which keeps the session token to itself.
//
// It is also the UploadGateway Aum's SyncService sends queued changes through: a queued
// change is the message type and its fields, so it is sent as that request as it stands.
public class MainframeGateway implements UploadGateway {
    static final long SEARCH_PAGE_SIZE = 50;

    // the reply field holding the number the mainframe gives a record it creates
    private static final Map<String, String> CREATED_NUMBER_FIELD = Map.of(
            "customer.create", "number",
            "loan.create", "loanNumber");

    private final Connection _connection;
    private volatile WriteDispatcher writes;
    private volatile java.util.function.UnaryOperator<String> resolveNumber = java.util.function.UnaryOperator.identity();
    private volatile java.util.function.Function<MessageRequest, MessageResponse> localReads = request -> null;
    public void setNumberResolver(java.util.function.UnaryOperator<String> resolver) { resolveNumber = resolver; }
    public void setLocalReads(java.util.function.Function<MessageRequest, MessageResponse> reads) { localReads = reads; }

    public MainframeGateway(Connection connection) {
        _connection = connection;
    }

    // Sends a message now and says how it went. The message keeps what the mainframe sent
    // back, so read it from the message after a SUCCESS.
    // AI-assisted: the same gateway handles queries and durable application writes.
    // Queue uploads use send(PendingChange), which deliberately bypasses this dispatcher.
    public void setWriteDispatcher(WriteDispatcher dispatcher) { writes = dispatcher; }
    public boolean queuesWrites() { return writes != null; }

    public GatewayResult send(Message message) {
        final MessageResponse[] reply = new MessageResponse[1];
        Connection exchange = new Connection() {
            public MessageResponse send(MessageRequest request) {
                for (String key : java.util.List.of("number", "customer", "customerNumber", "loanNumber", "coborrowerNumber")) {
                    String value = request.getValue(key);
                    if (value != null) request.setValue(key, resolveNumber.apply(value));
                }
                if (MessageRequest.QUERY.equals(request.getMessageType())) {
                    reply[0] = localReads.apply(request);
                    if (reply[0] != null) return reply[0];
                }
                WriteDispatcher dispatcher = writes;
                reply[0] = MessageRequest.UPDATE.equals(request.getMessageType()) && dispatcher != null
                        ? dispatcher.send(request) : sendRequest(request);
                return reply[0];
            }
            public boolean isConnected() { return MainframeGateway.this.isConnected(); }
            public mainframe.client.Status Login(String user, String password) throws IOException {
                return _connection.Login(user, password);
            }
            public void Logout() { _connection.Logout(); }
            public void close() { /* The app owns the shared connection. */ }
        };
        try {
            return GatewayResult.from(message.send(exchange)).withResponse(reply[0]);
        } catch (IOException e) {
            return GatewayResult.networkFailure(e.getMessage());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Malformed replies from the supplied message parsers are read failures.
            return GatewayResult.from(new mainframe.client.Status(
                    mainframe.client.StatusMessage.UNABLE_TO_PARSE.getCode(), "Could not read the mainframe reply", 0));
        }
    }

    private MessageResponse sendRequest(MessageRequest request) {
        synchronized (_connection) { return _connection.send(request); }
    }

    // Signs in; the Connection keeps the token for every later message.
    public GatewayResult login(String username, String password) {
        try {
            synchronized (_connection) { return GatewayResult.from(_connection.Login(username, password)); }
        } catch (IOException e) {
            return GatewayResult.networkFailure(e.getMessage());
        }
    }

    // Forgets the token, so nothing more is sent as this user.
    public void logout() {
        synchronized (_connection) { _connection.Logout(); }
    }

    @Override
    public boolean isConnected() {
        return _connection.isConnected();
    }

    // Sends one queued change as its mainframe message. For a change that creates a customer
    // or a loan, a SUCCESS carries the number the mainframe gave it, so the queue can swap it
    // for the temporary one.
    @Override
    public SendResult send(PendingChange change) {
        MessageRequest request = new MessageRequest(MessageRequest.UPDATE, change.getMessageType());
        change.getFields().forEach(request::setValue);
        MessageResponse response = sendRequest(request);
        GatewayResult result = GatewayResult.from(response.getStatus());
        switch (result.getOutcome()) {
            case SUCCESS:
                String numberField = CREATED_NUMBER_FIELD.get(change.getMessageType());
                return SendResult.success(numberField == null ? null : response.getValue(numberField), response);
            case NETWORK_FAILURE:
                return SendResult.networkFailure(result.getMessage());
            default:
                return SendResult.rejected(result.getMessage());
        }
    }

    // Looks for customers already on the mainframe with the same first name, family name and
    // date of birth as a queued customer.create. Searches by family name (the broadest match)
    // and compares the rest here, page by page. A search the mainframe refuses counts as no
    // match, so a customer is never stuck in the queue because of it.
    @Override
    public DuplicateCheckResult findExistingCustomers(PendingChange createCustomer) {
        Map<String, String> fields = createCustomer.getFields();
        String familyName = fields.get("familyName");
        if (isBlank(familyName)) {
            return DuplicateCheckResult.none();
        }
        List<String> matches = new ArrayList<>();
        long seen = 0;
        long page = 1;
        while (true) {
            // a new message per page, because message objects keep the items from earlier sends
            Search search = new Search(page, familyName.trim(), SEARCH_PAGE_SIZE);
            GatewayResult result = send(search);
            if (result.getOutcome() == GatewayResult.Outcome.NETWORK_FAILURE) {
                return DuplicateCheckResult.networkFailure(result.getMessage());
            }
            if (result.getOutcome() == GatewayResult.Outcome.REJECTED) {
                break;
            }
            Search.Item[] items = search.getItems();
            for (Search.Item item : items) {
                if (sameCustomer(fields, item)) {
                    matches.add(item.getNumber());
                }
            }
            seen += items.length;
            if (items.length == 0 || seen >= search.getTotal()) {
                break;
            }
            page++;
        }
        return matches.isEmpty() ? DuplicateCheckResult.none() : DuplicateCheckResult.found(matches);
    }

    private static boolean sameCustomer(Map<String, String> fields, Search.Item item) {
        return sameText(fields.get("firstName"), item.getFirstName())
                && sameText(fields.get("familyName"), item.getFamilyName())
                && sameText(fields.get("dateOfBirth"), item.getDateOfBirth());
    }

    // ignoring case and spaces at either end; a missing value never matches
    private static boolean sameText(String ours, String theirs) {
        return ours != null && theirs != null && ours.trim().equalsIgnoreCase(theirs.trim());
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
