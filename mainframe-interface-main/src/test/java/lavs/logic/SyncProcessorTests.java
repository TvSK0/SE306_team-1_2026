package lavs.logic;

import lavs.data.DuplicateCheckResult;
import lavs.data.SendResult;
import lavs.model.PendingChange;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class SyncProcessorTests {

    private static final String TEMP = "TMP-1";

    private InMemoryChangeQueue queue;
    private FakeUploadGateway gateway;
    private SyncProcessor processor;

    @BeforeEach
    void setUp() {
        queue = new InMemoryChangeQueue();
        gateway = new FakeUploadGateway();
        processor = new SyncProcessor(queue, gateway);
    }

    private PendingChange addCreateCustomer() {
        return queue.add(new PendingChange("customer.create", TEMP, TEMP, Map.of("firstName", "Aroha")));
    }

    private PendingChange addAddress(String customer) {
        return queue.add(new PendingChange("customer.addAddress", customer, null, Map.of("customer", customer)));
    }

    private SyncStep next() {
        return processor.processNext().orElseThrow();
    }

    private PendingChange addLoanFor(String customer) {
        return queue.add(new PendingChange("loan.create", customer, "TMP-L", Map.of("customer", customer)));
    }

    private PendingChange addCoborrowerTo(String loan) {
        return queue.add(new PendingChange("loan.addCoborrower", "C2", null, Map.of("loanNumber", loan)));
    }

    @Test
    void nothingToDoWhenQueueIsEmpty() {
        assertTrue(processor.processNext().isEmpty());
    }

    @Test
    void sendsOldestFirstAndDeletesAccepted() {
        addAddress("C1");
        queue.add(new PendingChange("customer.addEmail", "C1", null, Map.of()));

        SyncStep first = next();
        SyncStep second = next();

        assertEquals(List.of("customer.addAddress", "customer.addEmail"), gateway.sent);
        assertEquals(SyncStep.Outcome.SENT, first.getOutcome());
        assertEquals(SyncStep.Outcome.SENT, second.getOutcome());
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void networkFailureLeavesChangeQueued() {
        addAddress("C1");
        gateway.answerSend(SendResult.networkFailure("timed out"));

        SyncStep step = next();

        assertEquals(SyncStep.Outcome.WAITING, step.getOutcome());
        assertEquals("timed out", step.getMessage());
        assertEquals(PendingChange.Status.PENDING, queue.all().get(0).getStatus());
    }

    @Test
    void rejectedChangeIsMarkedAndNotRetried() {
        addAddress("C1");
        gateway.answerSend(SendResult.rejected("Bad postcode"));

        SyncStep step = next();

        assertEquals(SyncStep.Outcome.REJECTED, step.getOutcome());
        assertEquals("Bad postcode", step.getMessage());
        assertEquals(PendingChange.Status.REJECTED, queue.all().get(0).getStatus());
        assertTrue(processor.processNext().isEmpty());
    }

    @Test
    void rejectedNewRecordRejectsChangesThatDependOnIt() {
        PendingChange create = queue.add(new PendingChange("loan.create", "C1", "TMP-L", Map.of()));
        PendingChange dependent = queue.add(new PendingChange("loan.addCoborrower", "C1", null,
                Map.of("loanNumber", "TMP-L")));
        PendingChange unrelated = addAddress("C2");
        gateway.answerSend(SendResult.rejected("Bad rate"));

        next();

        assertAll(
                () -> assertEquals(PendingChange.Status.REJECTED, create.getStatus()),
                () -> assertEquals(PendingChange.Status.REJECTED, dependent.getStatus()),
                () -> assertEquals("Not sent because change " + create.getId() + " was rejected",
                        dependent.getStatusMessage()),
                () -> assertEquals(PendingChange.Status.PENDING, unrelated.getStatus()));
    }

    @Test
    void temporaryNumberIsSwappedInLaterChangesOnceUploaded() {
        addCreateCustomer();
        PendingChange address = addAddress(TEMP);
        gateway.answerSend(SendResult.success("C77"));

        SyncStep step = next();

        assertEquals(SyncStep.Outcome.SENT, step.getOutcome());
        assertEquals("C77", step.getMessage());
        assertEquals("C77", address.getCustomerNumber());
        assertEquals("C77", address.getFields().get("customer"));
        assertEquals("C77", processor.resolveNumber(TEMP));
    }

    @Test
    void noSwapWhenMainframeAssignsNoNumber() {
        queue.add(new PendingChange("loan.create", "C1", "TMP-L", Map.of()));
        PendingChange dependent = queue.add(new PendingChange("loan.addCoborrower", "C1", null,
                Map.of("loanNumber", "TMP-L")));

        next();

        assertEquals("TMP-L", dependent.getFields().get("loanNumber"));
        assertEquals("TMP-L", processor.resolveNumber("TMP-L"));
    }

    @Test
    void newCustomerIsSearchedForBeforeUpload() {
        addCreateCustomer();
        gateway.answerSend(SendResult.success("C1"));

        SyncStep step = next();

        assertEquals(1, gateway.duplicateSearches);
        assertEquals(SyncStep.Outcome.SENT, step.getOutcome());
    }

    @Test
    void possibleDuplicatePausesThatCustomersChangesOnly() {
        PendingChange create = addCreateCustomer();
        PendingChange address = addAddress(TEMP);
        PendingChange other = addAddress("C2");
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5", "C6")));

        SyncStep step = next();

        assertAll(
                () -> assertEquals(SyncStep.Outcome.PAUSED, step.getOutcome()),
                () -> assertEquals("Possible existing customer: C5, C6", step.getMessage()),
                () -> assertEquals(PendingChange.Status.PAUSED, create.getStatus()),
                () -> assertEquals(PendingChange.Status.PAUSED, address.getStatus()),
                () -> assertEquals(PendingChange.Status.PENDING, other.getStatus()),
                // AI-assisted: the matches are kept on the create only, not parsed from the message (US11)
                () -> assertEquals(List.of("C5", "C6"), create.getDuplicateMatches()),
                () -> assertEquals(List.of("C5", "C6"), DuplicateCandidates.forChange(create)),
                () -> assertTrue(address.getDuplicateMatches().isEmpty()),
                () -> assertTrue(gateway.sent.isEmpty()));
        assertEquals(other.getId(), next().getChange().getId());
    }

    @Test
    void duplicateSearchFailingLeavesCustomerQueued() {
        addCreateCustomer();
        gateway.answerDuplicate(DuplicateCheckResult.networkFailure("offline"));

        SyncStep step = next();

        assertEquals(SyncStep.Outcome.WAITING, step.getOutcome());
        assertEquals("offline", step.getMessage());
        assertTrue(gateway.sent.isEmpty());
    }

    @Test
    void confirmedOrRealCustomersAreNotSearched() {
        PendingChange confirmed = addCreateCustomer();
        confirmed.setSkipDuplicateCheck(true);
        queue.add(new PendingChange("customer.create", null, "C-REAL", Map.of()));
        queue.add(new PendingChange("customer.create", null, null, Map.of()));

        next();
        next();
        next();

        assertEquals(0, gateway.duplicateSearches);
        assertEquals(3, gateway.sent.size());
    }

    @Test
    void useExistingCustomerDropsCreateAndRepointsChanges() {
        addCreateCustomer();
        PendingChange address = addAddress(TEMP);
        PendingChange rejected = addAddress(TEMP);
        PendingChange other = addAddress("C2");
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        next();
        rejected.setStatus(PendingChange.Status.REJECTED);

        processor.useExistingCustomer(TEMP, "C5");

        assertAll(
                () -> assertEquals(3, queue.all().size()),
                () -> assertEquals("C5", address.getCustomerNumber()),
                () -> assertEquals(PendingChange.Status.PENDING, address.getStatus()),
                () -> assertNull(address.getStatusMessage()),
                () -> assertEquals(PendingChange.Status.REJECTED, rejected.getStatus()),
                () -> assertEquals("C2", other.getCustomerNumber()),
                () -> assertEquals("C5", processor.resolveNumber(TEMP)));
    }

    // AI-assisted: another new customer that mentions the number must not be mistaken for its create.
    @Test
    void anotherNewCustomerThatMentionsTheNumberIsRepointedNotDropped() {
        addCreateCustomer();
        PendingChange referred = queue.add(new PendingChange("customer.create", "TMP-2", "TMP-2",
                Map.of("referredBy", TEMP)));
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        next();

        processor.useExistingCustomer(TEMP, "C5");

        assertEquals(1, queue.all().size());
        assertEquals(referred.getId(), queue.all().get(0).getId());
        assertEquals("C5", referred.getFields().get("referredBy"));
        assertEquals(PendingChange.Status.PENDING, referred.getStatus());
    }

    @Test
    void confirmNewCustomerResumesAndSkipsTheSearch() {
        PendingChange create = addCreateCustomer();
        PendingChange address = addAddress(TEMP);
        PendingChange otherPaused = addAddress("C2");
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        next();
        otherPaused.setStatus(PendingChange.Status.PAUSED);
        PendingChange laterPending = addAddress(TEMP);

        processor.confirmNewCustomer(TEMP);

        assertAll(
                () -> assertTrue(create.isSkipDuplicateCheck()),
                // AI-assisted: once the MMM has decided, the matches are no longer offered (US11)
                () -> assertTrue(create.getDuplicateMatches().isEmpty()),
                () -> assertEquals(PendingChange.Status.PENDING, create.getStatus()),
                () -> assertEquals(PendingChange.Status.PENDING, address.getStatus()),
                () -> assertTrue(!address.isSkipDuplicateCheck()),
                () -> assertEquals(PendingChange.Status.PAUSED, otherPaused.getStatus()),
                () -> assertEquals(PendingChange.Status.PENDING, laterPending.getStatus()));
        gateway.answerSend(SendResult.success("C80"));
        assertEquals(SyncStep.Outcome.SENT, next().getOutcome());
        assertEquals(1, gateway.duplicateSearches);
    }

    @Test
    void prepareNewUsesNumbersAlreadyAssigned() {
        addCreateCustomer();
        gateway.answerSend(SendResult.success("C77"));
        next();
        PendingChange later = new PendingChange("customer.addEmail", TEMP, null, Map.of("customer", TEMP));
        PendingChange unrelated = new PendingChange("customer.addEmail", "C2", null, Map.of());

        processor.prepareNew(later);
        processor.prepareNew(unrelated);

        assertEquals("C77", later.getCustomerNumber());
        assertEquals("C2", unrelated.getCustomerNumber());
    }

    @Test
    void prepareNewCopiesPausedOrRejectedStateOfWhatItDependsOn() {
        addCreateCustomer();
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        next();
        PendingChange rejectedLoan = queue.add(new PendingChange("loan.create", "C1", "TMP-L", Map.of()));
        rejectedLoan.setStatus(PendingChange.Status.REJECTED);
        queue.add(new PendingChange("loan.create", "C1", "TMP-OK", Map.of()));

        PendingChange forPaused = new PendingChange("customer.addEmail", TEMP, null, Map.of());
        PendingChange forRejected = new PendingChange("loan.addCoborrower", "C1", null, Map.of("loanNumber", "TMP-L"));
        PendingChange forPending = new PendingChange("loan.addCoborrower", "C1", null, Map.of("loanNumber", "TMP-OK"));
        processor.prepareNew(forPaused);
        processor.prepareNew(forRejected);
        processor.prepareNew(forPending);

        assertAll(
                () -> assertEquals(PendingChange.Status.PAUSED, forPaused.getStatus()),
                () -> assertEquals("Possible existing customer: C5", forPaused.getStatusMessage()),
                () -> assertEquals(PendingChange.Status.REJECTED, forRejected.getStatus()),
                () -> assertEquals("Not sent because change " + rejectedLoan.getId() + " was rejected",
                        forRejected.getStatusMessage()),
                () -> assertEquals(PendingChange.Status.PENDING, forPending.getStatus()));
    }

    // AI-assisted: changes that use a paused or rejected record indirectly (through a new loan).
    @Test
    void changesToTheNewLoanOfAPausedCustomerArePausedAndResumedWithIt() {
        addCreateCustomer();
        PendingChange loan = addLoanFor(TEMP);
        PendingChange coborrower = addCoborrowerTo("TMP-L");
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        next();

        SyncStep held = next();

        assertAll(
                () -> assertEquals(SyncStep.Outcome.PAUSED, held.getOutcome()),
                () -> assertEquals(coborrower.getId(), held.getChange().getId()),
                () -> assertEquals("Possible existing customer: C5", held.getMessage()),
                () -> assertEquals(PendingChange.Status.PAUSED, coborrower.getStatus()),
                () -> assertEquals(PendingChange.Status.PAUSED, loan.getStatus()),
                () -> assertTrue(gateway.sent.isEmpty()));
        assertTrue(processor.processNext().isEmpty());

        processor.confirmNewCustomer(TEMP);
        gateway.answerSend(SendResult.success("C80"), SendResult.success("L9"));
        next();
        next();
        next();

        assertEquals(List.of("customer.create", "loan.create", "loan.addCoborrower"), gateway.sent);
        assertEquals("C80", gateway.sentFields.get(1).get("customer"));
        assertEquals("L9", gateway.sentFields.get(2).get("loanNumber"));
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void usingTheExistingCustomerAlsoResumesChangesToItsNewLoan() {
        addCreateCustomer();
        addLoanFor(TEMP);
        addCoborrowerTo("TMP-L");
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")));
        next();
        next();

        processor.useExistingCustomer(TEMP, "C5");
        gateway.answerSend(SendResult.success("L9"));
        next();
        next();

        assertEquals(List.of("loan.create", "loan.addCoborrower"), gateway.sent);
        assertEquals("C5", gateway.sentFields.get(0).get("customer"));
        assertEquals("L9", gateway.sentFields.get(1).get("loanNumber"));
        assertTrue(queue.all().isEmpty());
    }

    @Test
    void changesToTheNewLoanOfARejectedCustomerAreRejectedWithoutBeingSent() {
        addCreateCustomer();
        PendingChange loan = addLoanFor(TEMP);
        PendingChange coborrower = addCoborrowerTo("TMP-L");
        gateway.answerSend(SendResult.rejected("Missing date of birth"));
        next();

        SyncStep held = next();

        assertAll(
                () -> assertEquals(SyncStep.Outcome.REJECTED, held.getOutcome()),
                () -> assertEquals(coborrower.getId(), held.getChange().getId()),
                () -> assertEquals("Not sent because change " + loan.getId() + " was rejected", held.getMessage()),
                () -> assertEquals(PendingChange.Status.REJECTED, coborrower.getStatus()),
                () -> assertEquals(PendingChange.Status.REJECTED, loan.getStatus()),
                () -> assertTrue(gateway.sent.isEmpty()));
        assertTrue(processor.processNext().isEmpty());
    }

    @Test
    void resumedChangeThatStillUsesAnotherPausedCustomerIsHeldAgain() {
        addCreateCustomer();
        PendingChange other = queue.add(new PendingChange("customer.create", "TMP-2", "TMP-2", Map.of()));
        PendingChange link = queue.add(new PendingChange("customer.addRelationship", TEMP, null,
                Map.of("customer", TEMP, "relatedCustomer", "TMP-2")));
        gateway.answerDuplicate(DuplicateCheckResult.found(List.of("C5")), DuplicateCheckResult.found(List.of("C6")));
        next();
        next();
        assertEquals(PendingChange.Status.PAUSED, other.getStatus());

        processor.confirmNewCustomer(TEMP);
        gateway.answerSend(SendResult.success("C80"));
        next();
        SyncStep held = next();

        assertAll(
                () -> assertEquals(SyncStep.Outcome.PAUSED, held.getOutcome()),
                () -> assertEquals(link.getId(), held.getChange().getId()),
                () -> assertEquals("Possible existing customer: C6", held.getMessage()),
                () -> assertEquals("C80", link.getCustomerNumber()),
                () -> assertEquals(List.of("customer.create"), gateway.sent));

        processor.confirmNewCustomer("TMP-2");
        gateway.answerSend(SendResult.success("C81"));
        next();
        next();

        assertEquals(List.of("customer.create", "customer.create", "customer.addRelationship"), gateway.sent);
        assertEquals("C80", gateway.sentFields.get(2).get("customer"));
        assertEquals("C81", gateway.sentFields.get(2).get("relatedCustomer"));
    }
    @Test
    void acceptedCreateWithoutNumberIsRetainedForReview() {
        addCreateCustomer(); addAddress(TEMP);
        gateway.answerSend(SendResult.success());
        assertEquals(SyncStep.Outcome.PAUSED, next().getOutcome());
        assertEquals(2, queue.all().size());
        assertTrue(queue.all().stream().allMatch(change -> change.getStatus() == PendingChange.Status.PAUSED));
    }
}
