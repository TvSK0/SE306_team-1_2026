// AI-assisted.
package lavs.ui;

import lavs.logic.SyncService;
import lavs.model.PendingChange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingUploadsStateTests {
    @Mock
    SyncService sync;

    PendingUploadsState state() {
        return new PendingUploadsState(sync);
    }

    private PendingChange pending(String messageType) {
        return change(messageType, PendingChange.Status.PENDING);
    }

    private PendingChange change(String messageType, PendingChange.Status status) {
        PendingChange c = new PendingChange(messageType, "C-1001", null, Map.of());
        c.setStatus(status);
        return c;
    }

    // --- isEmpty ---

    @Test
    void isEmptyWhenQueueIsEmpty() {
        when(sync.getChanges()).thenReturn(List.of());
        assertTrue(state().isEmpty());
    }

    @Test
    void isNotEmptyWhenQueueHasChanges() {
        when(sync.getChanges()).thenReturn(List.of(pending("customer.addEmployer")));
        assertFalse(state().isEmpty());
    }

    // --- hasActionNeeded ---

    @Test
    void noActionNeededWhenAllChangesPending() {
        when(sync.getChanges()).thenReturn(List.of(pending("customer.addEmployer")));
        assertFalse(state().hasActionNeeded());
    }

    @Test
    void actionNeededWhenAChangeIsPaused() {
        when(sync.getChanges()).thenReturn(List.of(
                change("customer.addEmployer", PendingChange.Status.PAUSED)));
        assertTrue(state().hasActionNeeded());
    }

    @Test
    void actionNeededWhenAChangeIsRejected() {
        when(sync.getChanges()).thenReturn(List.of(
                change("customer.addEmployer", PendingChange.Status.REJECTED)));
        assertTrue(state().hasActionNeeded());
    }

    @Test
    void actionNeededWhenMixedStatuses() {
        when(sync.getChanges()).thenReturn(List.of(
                pending("customer.addEmployer"),
                change("loan.create", PendingChange.Status.PAUSED)));
        assertTrue(state().hasActionNeeded());
    }

    // --- getRows ---

    @Test
    void rowsAreEmptyWhenQueueIsEmpty() {
        when(sync.getChanges()).thenReturn(List.of());
        assertTrue(state().getRows().isEmpty());
    }

    @Test
    void rowsMatchChangesInOrder() {
        when(sync.getChanges()).thenReturn(List.of(
                pending("customer.addEmployer"),
                change("loan.create", PendingChange.Status.PAUSED)));

        List<PendingUploadsState.Row> rows = state().getRows();

        assertEquals(2, rows.size());
        assertEquals("Add employer", rows.get(0).getDescription());
        assertEquals("C-1001", rows.get(0).getCustomerNumber());
        assertEquals("Waiting to upload", rows.get(0).getStatusText());
        assertEquals("Create loan", rows.get(1).getDescription());
        assertEquals("Paused — action needed", rows.get(1).getStatusText());
    }

    @Test
    void rowCustomerNumberCopiedFromChange() {
        PendingChange c = new PendingChange("customer.addEmail", "C-9999", null, Map.of());
        when(sync.getChanges()).thenReturn(List.of(c));

        assertEquals("C-9999", state().getRows().get(0).getCustomerNumber());
    }

    // --- descriptionFor ---

    @Test void customerCreateDescription()           { assertEquals("Create customer",               PendingUploadsState.descriptionFor(pending("customer.create"))); }
    @Test void customerUpdateDescription()           { assertEquals("Update customer details",        PendingUploadsState.descriptionFor(pending("customer.update"))); }
    @Test void addAddressDescription()               { assertEquals("Add address",                   PendingUploadsState.descriptionFor(pending("customer.addAddress"))); }
    @Test void updateAddressDescription()            { assertEquals("Update address",                PendingUploadsState.descriptionFor(pending("customer.updateAddress"))); }
    @Test void deleteAddressDescription()            { assertEquals("Delete address",                PendingUploadsState.descriptionFor(pending("customer.deleteAddress"))); }
    @Test void addPhoneDescription()                 { assertEquals("Add phone number",              PendingUploadsState.descriptionFor(pending("customer.addPhoneNumber"))); }
    @Test void updatePhoneDescription()              { assertEquals("Update phone number",           PendingUploadsState.descriptionFor(pending("customer.updatePhoneNumber"))); }
    @Test void deletePhoneDescription()              { assertEquals("Delete phone number",           PendingUploadsState.descriptionFor(pending("customer.deletePhoneNumber"))); }
    @Test void addEmailDescription()                 { assertEquals("Add email",                     PendingUploadsState.descriptionFor(pending("customer.addEmail"))); }
    @Test void updateEmailDescription()              { assertEquals("Update email",                  PendingUploadsState.descriptionFor(pending("customer.updateEmail"))); }
    @Test void deleteEmailDescription()              { assertEquals("Delete email",                  PendingUploadsState.descriptionFor(pending("customer.deleteEmail"))); }
    @Test void addEmployerDescription()              { assertEquals("Add employer",                  PendingUploadsState.descriptionFor(pending("customer.addEmployer"))); }
    @Test void updateEmployerDescription()           { assertEquals("Update employer",               PendingUploadsState.descriptionFor(pending("customer.updateEmployer"))); }
    @Test void deleteEmployerDescription()           { assertEquals("Delete employer",               PendingUploadsState.descriptionFor(pending("customer.deleteEmployer"))); }
    @Test void addNoteDescription()                  { assertEquals("Add customer note",             PendingUploadsState.descriptionFor(pending("customer.addNote"))); }
    @Test void addRelationshipDescription()          { assertEquals("Add relationship",              PendingUploadsState.descriptionFor(pending("customer.addRelationship"))); }
    @Test void loanCreateDescription()               { assertEquals("Create loan",                   PendingUploadsState.descriptionFor(pending("loan.create"))); }
    @Test void loanUpdateDescription()               { assertEquals("Update loan",                   PendingUploadsState.descriptionFor(pending("loan.update"))); }
    @Test void addCoborrowerDescription()            { assertEquals("Add co-borrower",               PendingUploadsState.descriptionFor(pending("loan.addCoborrower"))); }
    @Test void deleteCoborrowerDescription()         { assertEquals("Remove co-borrower",            PendingUploadsState.descriptionFor(pending("loan.deleteCoborrower"))); }
    @Test void loanUpdateStatusDescription()         { assertEquals("Submit loan for verification",  PendingUploadsState.descriptionFor(pending("loan.updateStatus"))); }
    @Test void unknownMessageTypeFallsBackToItsName(){ assertEquals("mainframe.custom",              PendingUploadsState.descriptionFor(pending("mainframe.custom"))); }

    // --- statusTextFor ---

    @Test void pendingStatusText()  { assertEquals("Waiting to upload",         PendingUploadsState.statusTextFor(change("x", PendingChange.Status.PENDING))); }
    @Test void pausedStatusText()   { assertEquals("Paused — action needed", PendingUploadsState.statusTextFor(change("x", PendingChange.Status.PAUSED))); }
    @Test void rejectedStatusText() { assertEquals("Rejected by mainframe",     PendingUploadsState.statusTextFor(change("x", PendingChange.Status.REJECTED))); }

    // --- what the MMM can do about a row (AI-assisted, US11) ---

    private PendingChange pausedNewCustomer(List<String> matches) {
        PendingChange create = new PendingChange("customer.create", "TMP-1", "TMP-1", Map.of());
        create.setId(5L);
        create.setStatus(PendingChange.Status.PAUSED);
        create.setStatusMessage("Possible existing customer: C-7");
        create.setDuplicateMatches(matches);
        return create;
    }

    @Test
    void onlyThePausedNewCustomerItselfOffersTheDuplicateChoice() {
        PendingChange address = new PendingChange("customer.addAddress", "TMP-1", null, Map.of());
        address.setStatus(PendingChange.Status.PAUSED);
        when(sync.getChanges()).thenReturn(List.of(pausedNewCustomer(List.of("C-7")), address,
                pausedNewCustomer(List.of())));

        List<PendingUploadsState.Row> rows = state().getRows();

        assertTrue(rows.get(0).canResolveDuplicate());
        assertEquals(List.of("C-7"), rows.get(0).getDuplicateMatches());
        assertEquals("Possible existing customer: C-7", rows.get(0).getReason());
        assertFalse(rows.get(1).canResolveDuplicate(), "the pause covers it, but the create row resolves it");
        assertFalse(rows.get(2).canResolveDuplicate(), "nothing to offer without a match");
        assertFalse(rows.get(0).canDismiss());
    }

    @Test
    void onlyARejectedChangeCanBeDismissed() {
        when(sync.getChanges()).thenReturn(List.of(change("x", PendingChange.Status.REJECTED), pending("x")));

        List<PendingUploadsState.Row> rows = state().getRows();

        assertTrue(rows.get(0).canDismiss());
        assertFalse(rows.get(1).canDismiss());
        assertFalse(rows.get(1).canResolveDuplicate());
    }

    @Test
    void showsANewCustomerRatherThanItsTemporaryNumber() {
        when(sync.getChanges()).thenReturn(List.of(pausedNewCustomer(List.of("C-7"))));

        assertEquals("New customer", state().getRows().get(0).getCustomerNumber());
    }

    @Test
    void eachActionGoesToTheUploadQueue() {
        when(sync.getChanges()).thenReturn(List.of(pausedNewCustomer(List.of("C-7"))));
        PendingUploadsState state = state();
        PendingUploadsState.Row row = state.getRows().get(0);

        state.useExistingCustomer(row, "C-7");
        state.confirmNewCustomer(row);
        state.dismiss(row);

        org.mockito.Mockito.verify(sync).useExistingCustomer("TMP-1", "C-7");
        org.mockito.Mockito.verify(sync).confirmNewCustomer("TMP-1");
        org.mockito.Mockito.verify(sync).dismissRejected(5L);
    }
}
