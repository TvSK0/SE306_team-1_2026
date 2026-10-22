package lavs.logic;

import lavs.data.MainframeGateway;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CustomerBrowsingTests {
    private final FakeConnection connection = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(connection);
    @Test void searchValidatesAndTrimsInput() {
        var service = new CustomerSearchService(gateway);
        assertFalse(service.search("", 0).getValidationErrors().isValid());
        connection.whenRequested("customer.search", FakeConnection.success(Map.of("items", "0", "total", "0")));
        assertTrue(service.search(" Smith ", 1).isSuccessful());
        assertEquals("Smith", connection.getSentRequests().get(0).getValue("search"));
    }
    @Test void searchConvertsRowsAndHandlesUnknownDates() {
        var fields = new java.util.HashMap<String, String>();
        fields.put("items", "3"); fields.put("total", "50");
        fields.put("1:number", "C1"); fields.put("1:firstName", "Ana"); fields.put("1:dateOfBirth", "1990-01-02");
        fields.put("2:dateOfBirth", "invalid");
        connection.whenRequested("customer.search", FakeConnection.success(fields));
        var page = new CustomerSearchService(gateway).search("Ana", 1).getValue();
        assertEquals("C1", page.customers().get(0).getNumber());
        assertEquals(java.time.LocalDate.of(1990, 1, 2), page.customers().get(0).getDateOfBirth());
        assertNull(page.customers().get(1).getDateOfBirth()); assertNull(page.customers().get(2).getDateOfBirth());
        assertTrue(page.hasNext());
        assertFalse(new CustomerSearchService.Page(page.customers(), 2, 50).hasNext());
        assertFalse(new CustomerSearchService.Page(List.of(), 1, 100).hasNext());
    }
    @Test void searchFailureReturnsNoRows() {
        connection.setIsConnected(false);
        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, new CustomerSearchService(gateway).search("Ana", 1).getOutcome());
    }
    @Test void loansReadAllPagesAndStopOnEmptyPage() {
        var service = new CustomerLoansService(gateway);
        assertFalse(service.list(null).getValidationErrors().isValid());
        connection.whenRequested("loan.list", Map.of("page", "1"), FakeConnection.success(Map.of(
                "items", "1", "total", "100", "1:loanNumber", "L1", "1:principal", "1000", "1:status", "New")));
        connection.whenRequested("loan.list", Map.of("page", "2"), FakeConnection.success(Map.of("items", "0", "total", "100")));
        var result = service.list("C1");
        assertEquals("L1", result.getValue().get(0).number());
        assertEquals(2, connection.getSentRequests().size());
    }
    @Test void loansStopAtTotal() {
        connection.whenRequested("loan.list", FakeConnection.success(Map.of("items", "1", "total", "1", "1:loanNumber", "L1")));
        assertEquals(1, new CustomerLoansService(gateway).list("C1").getValue().size());
        assertEquals(1, connection.getSentRequests().size());
    }
    @Test void loansPropagateFailure() {
        connection.setIsConnected(false);
        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, new CustomerLoansService(gateway).list("C1").getOutcome());
    }
    @Test void employersRetrieveDetailsAndAllPages() {
        var service = new EmployerService(gateway);
        assertFalse(service.listEmployers("").getValidationErrors().isValid());
        connection.whenRequested("customerEmployer.list", Map.of("page", "1"), FakeConnection.success(Map.of("items", "1", "total", "2", "1:id", "3")));
        connection.whenRequested("customerEmployer.list", Map.of("page", "2"), FakeConnection.success(Map.of("items", "1", "total", "2", "1:id", "4")));
        connection.whenRequested("customerEmployer.retrieve", FakeConnection.success(Map.of("id", "3", "name", "Company", "salary", "120000", "type", "Primary", "position", "Engineer")));
        var result = service.listEmployers("C1");
        assertTrue(result.isSuccessful()); assertEquals(2, result.getValue().size());
        assertEquals("Engineer", result.getValue().get(0).getPosition());
    }
    @Test void emptyEmployersStopDespiteWrongTotal() {
        connection.whenRequested("customerEmployer.list", FakeConnection.success(Map.of("items", "0", "total", "2")));
        assertTrue(new EmployerService(gateway).listEmployers("C1").getValue().isEmpty());
    }
    @Test void employerDetailFailureReturnsNoPartialList() {
        connection.whenRequested("customerEmployer.list", FakeConnection.success(Map.of("items", "1", "total", "1", "1:id", "3")));
        connection.whenRequested("customerEmployer.retrieve", FakeConnection.rejected("Denied"));
        assertEquals("Denied", new EmployerService(gateway).listEmployers("C1").getMessage());
    }
    @Test void employerListFailurePropagates() {
        connection.setIsConnected(false);
        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, new EmployerService(gateway).listEmployers("C1").getOutcome());
    }
}
