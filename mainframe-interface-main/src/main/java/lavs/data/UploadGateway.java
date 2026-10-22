package lavs.data;

import lavs.model.PendingChange;

// AI-assisted.
// MainframeGateway is the real one; tests use FakeUploadGateway.

// What the sync threads needs from the mainframe. Tests pass thru a fake
public interface UploadGateway {

    // Sends one queued change as its mainframe message
    SendResult send(PendingChange change);

    // Searches the mainframe for customers that match a queued customer.create change
    DuplicateCheckResult findExistingCustomers(PendingChange createCustomer);

    // The connection status to the mainframe
    boolean isConnected();
}
