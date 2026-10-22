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

    public MainframeGateway(Connection connection) {
        _connection = connection;
    }

    // Sends a message now and says how it went. The message keeps what the mainframe sent
    // back, so read it from the message after a SUCCESS.
    public GatewayResult send(Message message) {
        try {
            return GatewayResult.from(message.send(_connection));
        } catch (IOException e) {
            return GatewayResult.networkFailure(e.getMessage());
        }
    }

    // Signs in; the Connection keeps the token for every later message.
    public GatewayResult login(String username, String password) {
        try {
            return GatewayResult.from(_connection.Login(username, password));
        } catch (IOException e) {
            return GatewayResult.networkFailure(e.getMessage());
        }
    }

    // Forgets the token, so nothing more is sent as this user.
    public void logout() {
        _connection.Logout();
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
        MessageResponse response = _connection.send(request);
        GatewayResult result = GatewayResult.from(response.getStatus());
        switch (result.getOutcome()) {
            case SUCCESS:
                String numberField = CREATED_NUMBER_FIELD.get(change.getMessageType());
                return SendResult.success(numberField == null ? null : response.getValue(numberField));
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
