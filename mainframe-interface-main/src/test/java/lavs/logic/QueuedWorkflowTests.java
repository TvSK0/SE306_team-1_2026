package lavs.logic;

import lavs.data.*;
import mainframe.client.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Map;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

// AI-assisted: application services, real SQLite and the shared gateway together.
class QueuedWorkflowTests {
    @TempDir Path directory;
    private final FakeConnection connection = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(connection);
    private final CustomerInput input = new CustomerInput("Ms", "Aroha", "Smith", "1990-01-02", "NZ");
    private final LoanDetailsInput loan = new LoanDetailsInput("1000", "Fixed", "5", "Yearly", "100", "Monthly", "2026-11-01", "");

    @Test void offlineCustomerAndEditsReopenAfterRestartAndStayWithTheirUser() {
        connection.setIsConnected(false);
        String number;
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            var service = new CustomerService(gateway, 1);
            var result = service.createCustomer(input);
            assertTrue(result.isQueued()); assertFalse(result.isSuccessful());
            number = result.getValue().getNumber();
            assertTrue(TemporaryNumbers.isTemporary(number));
            assertTrue(service.updateCustomer(number, new CustomerInput("Ms", "Ana", "Smith", "1990-01-02", "NZ")).isQueued());
            assertEquals(2, sync.getPendingCount());
            assertEquals("Ana", service.loadCustomer(number).getValue().getFirstName());
            assertTrue(SaveStatus.isQueued(result.getMainframeStatus()));
            assertEquals("Saved locally. Waiting to upload.", SaveStatus.message(result.getMainframeStatus()));
        }
        try (var sync = SyncSessions.open(directory, "bob", gateway)) {
            assertTrue(sync.getChanges().isEmpty());
            assertFalse(new CustomerService(gateway, 1).loadCustomer(number).isSuccessful());
        }
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            assertEquals(2, sync.getChanges().size());
            assertEquals("Ana", new CustomerService(gateway, 1).loadCustomer(number).getValue().getFirstName());
        }
    }
    @Test void offlineLoanSavesAndReopensWithoutPretendingItUploaded() throws Exception {
        connection.setIsConnected(false);
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            var service = new LoanService(gateway);
            assertTrue(service.canSaveLocally());
            var result = service.createLoan("C1", loan);
            assertTrue(result.isQueued()); assertFalse(result.isSuccessful());
            var number = result.getLoan().getLoanNumber();
            assertEquals("Waiting to upload", service.loadLoan(number).getLoan().getStatus());
            assertTrue(service.updateLoan("C1", number, loan).isQueued());
            assertEquals(2, sync.getChanges().size());
        }
    }
    @Test void onlineWriteReturnsActualMainframeResponseAndRejection() {
        connection.whenRequested("customer.addNote", FakeConnection.success(Map.of("id", "4")));
        connection.whenRequested("customer.deleteNote", FakeConnection.rejected("Locked"));
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            var service = new RecordNotesService(gateway);
            assertTrue(service.save(RecordOwner.CUSTOMER, "C1", null, "General", "hello").isSuccessful());
            assertEquals("Locked", service.delete(RecordOwner.CUSTOMER, "C1", 4).getMessage());
            assertEquals(1, sync.getChanges().size());
            assertEquals(0, sync.getPendingCount());
        }
    }
    @Test void duplicateRequiresOneOfTheOfferedCustomers() {
        connection.whenRequested("customer.search", FakeConnection.success(Map.of(
                "items", "1", "total", "1", "1:number", "C1", "1:firstName", "Aroha",
                "1:familyName", "Smith", "1:dateOfBirth", "1990-01-02")));
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            var result = new CustomerService(gateway, 1).createCustomer(input);
            assertEquals(ServiceResult.Outcome.PAUSED, result.getOutcome());
            assertTrue(SaveStatus.message(result.getMainframeStatus()).contains("duplicate"));
            var change = sync.getChanges().get(0);
            assertEquals(List.of("C1"), DuplicateCandidates.forChange(change));
            assertThrows(java.util.concurrent.CompletionException.class, () -> sync.resolveDuplicate(change.getId(), "C2").join());
            sync.resolveDuplicate(change.getId(), "C1").join();
            assertEquals("C1", sync.resolveNumber(result.getValue().getNumber()));
            assertTrue(sync.getChanges().isEmpty());
            assertThrows(java.util.concurrent.CompletionException.class, () -> sync.resolveDuplicate(change.getId(), null).join());
        }
    }
    @Test void invalidSessionAndUnwritableDirectoryFailBeforeOpening() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> SyncSessions.open(directory, null, gateway));
        assertThrows(IllegalArgumentException.class, () -> SyncSessions.open(directory, " ", gateway));
        var file = directory.resolve("file"); java.nio.file.Files.writeString(file, "x");
        assertThrows(LocalStoreException.class, () -> SyncSessions.open(file, "alice", gateway));
    }
    @Test void dispatcherRejectsQueries() {
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            assertThrows(IllegalArgumentException.class, () -> new QueuedWrites(sync).send(new MessageRequest(MessageRequest.QUERY, "customer.search")));
        }
    }
    @Test void verifiedLoanRejectsInvalidEligibilityAndRetainsItLocally() {
        connection.whenRequested("customerAddress.list", FakeConnection.success(Map.of("items", "0")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(Map.of("items", "0")));
        connection.whenRequested("customerEmail.list", FakeConnection.success(Map.of("items", "0")));
        var uploads = new VerifiedUploads(gateway);
        var change = new lavs.model.PendingChange("loan.create", "C1", "TMP-L", Map.of("customerNumber", "C1"));
        assertTrue(uploads.isConnected());
        assertEquals(SendResult.Outcome.REJECTED, uploads.send(change).getOutcome());
        assertFalse(connection.getSentRequests().stream().anyMatch(r -> r.getRequestType().equals("loan.create")));
    }
    @Test void verifiedLoanPropagatesEligibilityRejection() {
        connection.whenRequested("customerAddress.list", FakeConnection.rejected("Denied"));
        var change = new lavs.model.PendingChange("loan.create", "C1", "TMP-L", Map.of("customerNumber", "C1"));
        assertEquals("Denied", new VerifiedUploads(gateway).send(change).getMessage());
    }
    @Test void verifiedLoanUploadsAfterCheckingCurrentRecords() {
        connection.whenRequested("customerAddress.list", FakeConnection.success(Map.of("items", "1", "total", "1", "1:id", "1", "1:isMailing", "y")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(Map.of("items", "1", "1:id", "2")));
        connection.whenRequested("loan.create", FakeConnection.success(Map.of("loanNumber", "L1")));
        var change = new lavs.model.PendingChange("loan.create", "C1", "TMP-L", Map.of("customerNumber", "C1"));
        assertEquals("L1", new VerifiedUploads(gateway).send(change).getAssignedNumber());
    }
    @Test void queuedSubmissionRechecksTheCurrentLoanStatus() {
        connection.whenRequested("loan.retrieve", FakeConnection.success(Map.of("loanNumber", "L1", "customerNumber", "C1",
                "status", "Pending", "principal", "1000", "rateAmount", "5", "paymentAmount", "100")));
        var change = new lavs.model.PendingChange("loan.updateStatus", "C1", null, Map.of("loanNumber", "L1", "status", "Pending"));
        var result = new VerifiedUploads(gateway).send(change);
        assertEquals(SendResult.Outcome.REJECTED, result.getOutcome());
        assertTrue(result.getMessage().contains("only a New loan"));
    }
    @Test void queuedSubmissionWaitsForNetworkAndReportsRejections() {
        var change = new lavs.model.PendingChange("loan.updateStatus", "C1", null, Map.of("loanNumber", "L1", "status", "Pending"));
        connection.setIsConnected(false);
        assertEquals(SendResult.Outcome.NETWORK_FAILURE, new VerifiedUploads(gateway).send(change).getOutcome());
        connection.setIsConnected(true);
        connection.whenRequested("loan.retrieve", FakeConnection.rejected("Denied"));
        assertEquals("Denied", new VerifiedUploads(gateway).send(change).getMessage());
    }
    @Test void queuedSubmissionUploadsOnlyAfterRecheckingAllRequirements() {
        connection.whenRequested("loan.retrieve", FakeConnection.success(Map.of("loanNumber", "L1", "customerNumber", "C1",
                "status", "New", "principal", "1000", "rateAmount", "5", "paymentAmount", "100")));
        connection.whenRequested("customerAddress.list", FakeConnection.success(Map.of("items", "1", "total", "1", "1:id", "1", "1:isMailing", "y")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(Map.of("items", "1", "1:id", "2")));
        connection.whenRequested("customerEmployer.list", FakeConnection.success(Map.of("items", "1", "1:id", "3")));
        connection.whenRequested("loanCoborrower.list", FakeConnection.success(Map.of("items", "0", "total", "0")));
        connection.whenRequested("loan.updateStatus", FakeConnection.success(Map.of("status", "Pending")));
        var change = new lavs.model.PendingChange("loan.updateStatus", "C1", null, Map.of("loanNumber", "L1", "status", "Pending"));
        assertEquals(SendResult.Outcome.SUCCESS, new VerifiedUploads(gateway).send(change).getOutcome());
        var sent = connection.getSentRequests();
        assertEquals("loan.updateStatus", sent.get(sent.size() - 1).getRequestType());
    }
    @Test void borrowerChangeUsesDurableCompoundOperation() throws Exception {
        connection.setIsConnected(false);
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            var existing = new lavs.model.LoanCoborrower("L1", "C1", LoanBorrowerService.COBORROWER); existing.setId(4L);
            var result = new LoanBorrowerService(gateway).updateCoborrower(existing, "C1", LoanBorrowerService.GUARANTOR);
            assertTrue(SaveStatus.isQueued(result.getMainframeStatus()));
            assertEquals("loan.replaceCoborrower", sync.getChanges().get(0).getMessageType());
            assertEquals(1, sync.getChanges().size());
        }
    }
    @Test void documentEditUsesTwoRealMainframeMessages() {
        connection.whenRequested("customer.addDocument", FakeConnection.success(Map.of("id", "10")));
        connection.whenRequested("customer.deleteDocument", FakeConnection.success(Map.of()));
        try (var sync = SyncSessions.open(directory, "alice", gateway)) {
            assertTrue(new RecordDocumentsService(gateway).save(RecordOwner.CUSTOMER, "C1", 4L, "Identity", "id.pdf", "pdf", "123").isSuccessful());
            assertEquals(List.of("customer.addDocument", "customer.deleteDocument"),
                    connection.getSentRequests().stream().map(MessageRequest::getRequestType).toList());
            assertTrue(sync.getChanges().isEmpty());
        }
    }
}
