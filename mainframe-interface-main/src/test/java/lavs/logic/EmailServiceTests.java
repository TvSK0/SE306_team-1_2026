package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.model.Email;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. Saves now go through the upload queue (US11): a real SyncService over an in-memory
// queue sends them to the same FakeConnection, as the same requests.
class EmailServiceTests {
    private static final String CUSTOMER = "C-10234";
    private static final EmailInput PERSONAL = new EmailInput("Personal", "", "jane@example.com", true);

    private final FakeConnection connection = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(connection);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final InMemoryChangeQueue queue = new InMemoryChangeQueue();
    private final SyncService sync = new SyncService(queue, gateway, executor);
    private final EmailService service = new EmailService(gateway, sync);

    @AfterEach
    void tearDown() {
        sync.close();
    }

    // --- checking the form ---

    @Test
    void acceptsACompleteEmail() {
        assertTrue(service.validate(PERSONAL).isValid());
    }

    @Test
    void needsATypeAndAnAddress() {
        assertEquals(List.of(EmailInput.TYPE, EmailInput.ADDRESS),
                fieldsWithErrors(new EmailInput(null, "", " ", false)));
    }

    @Test
    void anOtherTypeNeedsADescription() {
        assertEquals(List.of(EmailInput.OTHER_TYPE), fieldsWithErrors(new EmailInput("Other", null, "a@b.co", false)));
    }

    @Test
    void checksTheAddressWithTheSharedEmailValidator() {
        List<FieldError> errors = service.validate(new EmailInput("Work", "", "jane at example", false)).getErrors();

        assertEquals(EmailInput.ADDRESS, errors.get(0).getFieldName());
    }

    // --- listing ---

    @Test
    void listsEveryEmailAndOnlyReadsAgainForAnOtherDescription() {
        givenListPage(1, 2, row(5, "Personal", "y"), row(6, "Other", "n"));
        connection.whenRequested("customerEmail.retrieve", Map.of("id", "6"),
                FakeConnection.success(Map.of("id", "6", "type", "Other", "otherType", "Shared")));

        ServiceResult<List<Email>> result = service.listEmails(CUSTOMER);

        assertTrue(result.isSuccessful());
        Email personal = result.getValue().get(0);
        assertAll(
                () -> assertEquals(5L, personal.getId()),
                () -> assertEquals("jane@example.com", personal.getAddress()),
                () -> assertTrue(personal.isPrimary()),
                () -> assertNull(personal.getOtherType()));
        assertEquals("Shared", result.getValue().get(1).getOtherType());
        assertEquals(1, sent("customerEmail.retrieve").size());
    }

    @Test
    void readsEveryPageAndStopsOnAnEmptyOne() {
        givenListPage(1, 5, row(5, "Personal", "y"));
        givenListPage(2, 5);

        assertEquals(1, service.listEmails(CUSTOMER).getValue().size());
        assertEquals(2, sent("customerEmail.list").size());
    }

    @Test
    void readsTheNextPageUntilItHasThemAll() {
        givenListPage(1, 2, row(5, "Personal", "y"));
        givenListPage(2, 2, row(6, "Work", "n"));

        assertEquals(2, service.listEmails(CUSTOMER).getValue().size());
    }

    @Test
    void reportsAListTheMainframeRefused() {
        connection.whenRequested("customerEmail.list", FakeConnection.rejected("no such customer"));

        assertEquals(ServiceResult.Outcome.REJECTED, service.listEmails(CUSTOMER).getOutcome());
    }

    @Test
    void reportsAnOtherDescriptionThatCouldNotBeRead() {
        givenListPage(1, 1, row(6, "Other", "n"));
        connection.whenRequested("customerEmail.retrieve", FakeConnection.networkDown());

        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, service.listEmails(CUSTOMER).getOutcome());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void needsACustomerNumber(String customerNumber) {
        assertFalse(service.listEmails(customerNumber).getValidationErrors().isValid());
        assertFalse(service.addEmail(customerNumber, PERSONAL).getValidationErrors().isValid());
        assertFalse(service.updateEmail(customerNumber, 5, PERSONAL).getValidationErrors().isValid());
        assertFalse(service.deleteEmail(customerNumber, 5).getValidationErrors().isValid());
        assertTrue(connection.getSentRequests().isEmpty());
        assertTrue(queue.all().isEmpty(), "nothing is queued when the checks fail");
    }

    // --- adding, updating and deleting ---

    @Test
    void addsAnEmailAndReturnsWhatWasSaved() {
        connection.whenRequested("customer.addEmail", FakeConnection.success(emailReply(7)));

        ServiceResult<Email> result = service.addEmail(CUSTOMER, new EmailInput("Personal", "ignored", " jane@example.com ", true));

        assertTrue(result.isSuccessful());
        // what was saved, as sent; the new id comes from listing again
        assertNull(result.getValue().getId());
        assertEquals("jane@example.com", result.getValue().getAddress());
        assertTrue(result.getValue().isPrimary());
        MessageRequest sent = connection.getSentRequests().get(0);
        assertAll(
                () -> assertEquals(CUSTOMER, sent.getValue("customer")),
                () -> assertEquals("jane@example.com", sent.getValue("address")),
                () -> assertNull(sent.getValue("otherType")),
                () -> assertEquals("y", sent.getValue("isPrimary")));
    }

    @Test
    void doesNotContactTheMainframeWhenTheFormHasErrors() {
        service.addEmail(CUSTOMER, new EmailInput("Personal", "", "not-an-email", false));

        assertTrue(connection.getSentRequests().isEmpty());
    }

    @Test
    void updatesAnEmailById() {
        connection.whenRequested("customer.updateEmail", FakeConnection.success(emailReply(5)));

        ServiceResult<Email> result = service.updateEmail(CUSTOMER, 5, new EmailInput("Other", " Shared ", "family@example.com", false));

        assertTrue(result.isSuccessful());
        MessageRequest sent = connection.getSentRequests().get(0);
        assertEquals("5", sent.getValue("id"));
        assertEquals("Shared", sent.getValue("otherType"));
    }

    @Test
    void reportsAnUpdateTheMainframeRefused() {
        connection.whenRequested("customer.updateEmail", FakeConnection.rejected("no such email"));

        ServiceResult<Email> result = service.updateEmail(CUSTOMER, 5, PERSONAL);

        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("no such email", result.getMessage());
        assertNull(result.getValue());
    }

    @Test
    void keepsAnEmailAddedOfflineToUploadLater() {
        connection.whenRequested("customer.addEmail", FakeConnection.networkDown());

        ServiceResult<Email> result = service.addEmail(CUSTOMER, PERSONAL);

        assertTrue(result.isSaved());
        assertTrue(result.isWaitingToUpload());
        assertEquals("jane@example.com", result.getValue().getAddress());
        assertEquals("customer.addEmail", queue.all().get(0).getMessageType());
    }

    @Test
    void keepsAnUpdateMadeOfflineToUploadLater() {
        connection.whenRequested("customer.updateEmail", FakeConnection.networkDown());

        ServiceResult<Email> result = service.updateEmail(CUSTOMER, 5, PERSONAL);

        assertTrue(result.isWaitingToUpload());
        assertEquals(5L, result.getValue().getId());
    }

    @Test
    void keepsADeleteMadeOfflineToUploadLater() {
        connection.whenRequested("customer.deleteEmail", FakeConnection.networkDown());

        assertTrue(service.deleteEmail(CUSTOMER, 5).isWaitingToUpload());
        assertEquals("customer.deleteEmail", queue.all().get(0).getMessageType());
    }

    @Test
    void deletesAnEmailById() {
        connection.whenRequested("customer.deleteEmail", FakeConnection.success(Map.of("id", "5", "customer", CUSTOMER)));

        assertTrue(service.deleteEmail(CUSTOMER, 5).isSuccessful());
        assertEquals("5", connection.getSentRequests().get(0).getValue("id"));
    }

    @Test
    void fillsTheFormFromASavedEmail() {
        Email saved = new Email(CUSTOMER, "Work", null, false);

        EmailInput input = EmailInput.fromEmail(saved);

        assertAll(
                () -> assertEquals("Work", input.getType()),
                () -> assertEquals("", input.getOtherType()),
                () -> assertEquals("", input.getAddress()),
                () -> assertFalse(input.isPrimary()));
    }

    // --- helpers ---

    private List<String> fieldsWithErrors(EmailInput input) {
        return service.validate(input).getErrors().stream().map(FieldError::getFieldName).toList();
    }

    private List<MessageRequest> sent(String requestType) {
        return connection.getSentRequests().stream().filter(r -> requestType.equals(r.getRequestType())).toList();
    }

    // one row of a customerEmail.list reply: id, type, isPrimary
    private static String[] row(long id, String type, String primary) {
        return new String[] {Long.toString(id), type, primary};
    }

    private void givenListPage(long page, long total, String[]... rows) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(rows.length));
        for (int i = 1; i <= rows.length; i++) {
            values.put(i + ":id", rows[i - 1][0]);
            values.put(i + ":type", rows[i - 1][1]);
            values.put(i + ":isPrimary", rows[i - 1][2]);
            values.put(i + ":address", "jane@example.com");
        }
        connection.whenRequested("customerEmail.list", Map.of("page", Long.toString(page)), FakeConnection.success(values));
    }

    private static Map<String, String> emailReply(long id) {
        Map<String, String> values = new HashMap<>();
        values.put("id", Long.toString(id));
        values.put("type", "Personal");
        values.put("address", "jane@example.com");
        values.put("isPrimary", "y");
        return values;
    }
}
