package lavs.logic;

import lavs.data.MainframeGateway;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

// AI-assisted: verify the precondition against actual message responses.
class CustomerEligibilityServiceTests {
    private final FakeConnection connection = new FakeConnection();
    private final CustomerEligibilityService service = new CustomerEligibilityService(new MainframeGateway(connection));
    private void addresses(String mailing) {
        connection.whenRequested("customerAddress.list", FakeConnection.success(Map.of(
                "items", "1", "total", "1", "1:id", "1", "1:isMailing", mailing)));
    }
    private void contacts(String kind, boolean present) {
        connection.whenRequested(kind + ".list", FakeConnection.success(present
                ? Map.of("items", "1", "1:id", "2") : Map.of("items", "0")));
    }
    @Test void invalidCustomerNeverSends() {
        assertFalse(service.check(" ").getValidationErrors().isValid());
        assertTrue(connection.getSentRequests().isEmpty());
    }
    @Test void mailingAndPhoneAreEligible() {
        addresses("Y"); contacts("customerPhoneNumber", true);
        assertTrue(service.check("C1").isSuccessful());
        assertEquals(2, connection.getSentRequests().size());
    }
    @Test void emailCanReplacePhone() {
        addresses("y"); contacts("customerPhoneNumber", false); contacts("customerEmail", true);
        assertTrue(service.check("C1").isSuccessful());
    }
    @Test void missingMailingAndContactsReportsBoth() {
        addresses("n"); contacts("customerPhoneNumber", false); contacts("customerEmail", false);
        assertEquals(2, service.check("C1").getValidationErrors().getErrors().size());
    }
    @Test void noAddressesStopsAtEmptyPage() {
        connection.whenRequested("customerAddress.list", FakeConnection.success(Map.of("items", "0", "total", "500")));
        contacts("customerPhoneNumber", true);
        assertEquals(1, service.check("C1").getValidationErrors().getErrors().size());
        assertEquals(2, connection.getSentRequests().size());
    }
    @Test void unavailableAddressesAreNotTreatedAsAbsent() {
        connection.setIsConnected(false);
        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, service.check("C1").getOutcome());
    }
    @Test void phoneRejectionPropagates() {
        addresses("y"); connection.whenRequested("customerPhoneNumber.list", FakeConnection.rejected("Denied"));
        assertEquals("Denied", service.check("C1").getMessage());
    }
    @Test void emailRejectionPropagates() {
        addresses("y"); contacts("customerPhoneNumber", false);
        connection.whenRequested("customerEmail.list", FakeConnection.rejected("Denied"));
        assertEquals("Denied", service.check("C1").getMessage());
    }
}
