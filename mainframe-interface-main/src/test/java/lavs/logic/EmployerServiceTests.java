package lavs.logic;

// AI-assisted: tests rewritten for the queue path (US11). EmployerService now writes through
// SyncService, so these drive a real SyncService over the in-memory ChangeQueue and the fake
// gateway: validation still stops a bad call before it is queued, a good call is SENT (SAVED),
// an offline call is WAITING, a refused call is REJECTED, and a local save failure is reported.
import lavs.data.ChangeQueue;
import lavs.data.SendResult;
import lavs.model.PendingChange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployerServiceTests {
    private static final String CUSTOMER_NUMBER = "CUST-42";

    private InMemoryChangeQueue queue;
    private FakeUploadGateway gateway;
    private ScheduledExecutorService executor;
    private SyncService sync;
    private EmployerService service;

    @BeforeEach
    void setUp() {
        queue = new InMemoryChangeQueue();
        gateway = new FakeUploadGateway();
        executor = Executors.newSingleThreadScheduledExecutor();
        sync = new SyncService(queue, gateway, executor);
        service = new EmployerService(sync);
    }

    @AfterEach
    void tearDown() {
        sync.close();
    }

    // --- addEmployer ---

    @Test
    void addEmployerQueuesTheChangeAndSucceedsWhenOnline() {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "80000.00", "Primary");

        assertTrue(result.isSuccessful());
        assertEquals(EmployerResult.Outcome.SAVED, result.getOutcome());
        assertTrue(result.getValidationErrors().isValid());
        // the exact message and fields the mainframe is sent, matching AddEmployer.send
        assertEquals(List.of("customer.addEmployer"), gateway.sent);
        assertEquals(Map.of(
                "customer", CUSTOMER_NUMBER,
                "name", "Acme Corp",
                "position", "Engineer",
                "salary", "80000.0",
                "type", "Primary"), gateway.sentFields.get(0));
    }

    @Test
    void addEmployerAcceptsABlankSalaryAndSendsZero() {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "", "Secondary");

        assertTrue(result.isSuccessful());
        assertEquals("0.0", gateway.sentFields.get(0).get("salary"));
    }

    @Test
    void addEmployerAcceptsANullSalary() {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", null, "Primary");

        assertTrue(result.isSuccessful());
        assertEquals("0.0", gateway.sentFields.get(0).get("salary"));
    }

    @Test
    void addEmployerWaitsToUploadWhenOffline() {
        gateway.offline = true;

        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Primary");

        assertFalse(result.isSuccessful());
        assertTrue(result.isWaitingToUpload());
        assertEquals(EmployerResult.Outcome.WAITING, result.getOutcome());
        // the change is kept in the queue to send later, and nothing reached the mainframe
        assertEquals(1, queue.all().size());
        assertEquals(PendingChange.Status.PENDING, queue.all().get(0).getStatus());
        assertTrue(gateway.sent.isEmpty());
    }

    @Test
    void addEmployerReportsAMainframeRejectionWithoutThrowing() {
        gateway.answerSend(SendResult.rejected("customer not found"));

        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals(EmployerResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("customer not found", result.getMessage());
    }

    @Test
    void addEmployerReportsALocalSaveFailure() {
        EmployerService failing = new EmployerService(new SyncService(throwingQueue(), gateway, executor));

        EmployerResult result = failing.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals(EmployerResult.Outcome.REJECTED, result.getOutcome());
        assertTrue(result.getMessage().contains("disk full"));
    }

    @Test
    void addEmployerFailsValidationForMissingRequiredFieldsWithoutQueuing() {
        EmployerResult result = service.addEmployer(null, "", "Engineer", "50000", null);

        assertFalse(result.isSuccessful());
        assertFalse(result.getValidationErrors().isValid());
        // customerNumber, name, type all missing = 3 errors
        assertEquals(3, result.getValidationErrors().getErrors().size());
        assertTrue(queue.all().isEmpty());
        assertTrue(gateway.sent.isEmpty());
    }

    @Test
    void addEmployerRejectsAnUnknownType() {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Contractor");

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void addEmployerTreatsABlankTypeAsMissingRatherThanUnknown() {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "   ");

        // only the "is required" error - the unknown-type check is skipped for blank
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void addEmployerRejectsAnInvalidSalaryFormat() {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "not-a-number", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("salary", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void addEmployerRejectsASalaryWithMoreThanTwoDecimalPlaces() {
        EmployerResult result = service.addEmployer(
                CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000.999", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals("salary", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(queue.all().isEmpty());
    }

    // --- updateEmployer ---

    @Test
    void updateEmployerQueuesTheChangeAndSucceeds() {
        EmployerResult result = service.updateEmployer(
                1L, CUSTOMER_NUMBER, "Acme Corp", "Senior Engineer", "90000.00", "Primary");

        assertTrue(result.isSuccessful());
        assertEquals(List.of("customer.updateEmployer"), gateway.sent);
        assertEquals(Map.of(
                "customer", CUSTOMER_NUMBER,
                "id", "1",
                "name", "Acme Corp",
                "position", "Senior Engineer",
                "salary", "90000.0",
                "type", "Primary"), gateway.sentFields.get(0));
    }

    @Test
    void updateEmployerReportsAMainframeRejectionWithoutThrowing() {
        gateway.answerSend(SendResult.rejected("no such employer"));

        EmployerResult result = service.updateEmployer(
                99L, CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals(EmployerResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("no such employer", result.getMessage());
    }

    @Test
    void updateEmployerFailsValidationForMissingRequiredFields() {
        EmployerResult result = service.updateEmployer(1L, null, "", "Engineer", "50000", null);

        assertFalse(result.isSuccessful());
        assertEquals(3, result.getValidationErrors().getErrors().size());
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void updateEmployerRejectsAnUnknownType() {
        EmployerResult result = service.updateEmployer(
                1L, CUSTOMER_NUMBER, "Acme Corp", "Engineer", "50000", "Freelance");

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("type", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void updateEmployerRejectsAnInvalidSalary() {
        EmployerResult result = service.updateEmployer(
                1L, CUSTOMER_NUMBER, "Acme Corp", "Engineer", "-500", "Primary");

        assertFalse(result.isSuccessful());
        assertEquals("salary", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(queue.all().isEmpty());
    }

    // --- deleteEmployer ---

    @Test
    void deleteEmployerQueuesTheDeleteAndSucceeds() {
        EmployerResult result = service.deleteEmployer(1L, CUSTOMER_NUMBER);

        assertTrue(result.isSuccessful());
        assertEquals(List.of("customer.deleteEmployer"), gateway.sent);
        assertEquals(Map.of("customer", CUSTOMER_NUMBER, "id", "1"), gateway.sentFields.get(0));
    }

    @Test
    void deleteEmployerWaitsToUploadWhenOffline() {
        gateway.offline = true;

        EmployerResult result = service.deleteEmployer(1L, CUSTOMER_NUMBER);

        assertTrue(result.isWaitingToUpload());
        assertEquals(1, queue.all().size());
    }

    @Test
    void deleteEmployerFailsValidationWithoutQueuing() {
        EmployerResult result = service.deleteEmployer(1L, " ");

        assertFalse(result.isSuccessful());
        assertFalse(result.getValidationErrors().isValid());
        assertTrue(queue.all().isEmpty());
        assertTrue(gateway.sent.isEmpty());
    }

    @Test
    void deleteEmployerReportsAMainframeRejectionWithoutThrowing() {
        gateway.answerSend(SendResult.rejected("employer not found"));

        EmployerResult result = service.deleteEmployer(99L, CUSTOMER_NUMBER);

        assertFalse(result.isSuccessful());
        assertEquals(EmployerResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("employer not found", result.getMessage());
    }

    @Test
    void deleteEmployerFailsValidationForNullCustomerNumber() {
        EmployerResult result = service.deleteEmployer(1L, null);

        assertFalse(result.isSuccessful());
        assertEquals(1, result.getValidationErrors().getErrors().size());
        assertEquals("customerNumber", result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(queue.all().isEmpty());
    }

    // --- helpers ---

    // a queue whose first write fails, to drive the local-save-failure path
    private static ChangeQueue throwingQueue() {
        return new ChangeQueue() {
            @Override
            public PendingChange add(PendingChange change) {
                throw new RuntimeException("disk full");
            }

            @Override
            public Optional<PendingChange> next() {
                return Optional.empty();
            }

            @Override
            public List<PendingChange> all() {
                return List.of();
            }

            @Override
            public void update(PendingChange change) {
            }

            @Override
            public void markRejected(long id, String reason) {
            }

            @Override
            public void delete(long id) {
            }
        };
    }
}
