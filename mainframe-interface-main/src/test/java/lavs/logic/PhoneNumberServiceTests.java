package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.model.PhoneNumber;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. Saves now go through the upload queue (US11): a real SyncService over an in-memory
// queue sends them to the same FakeConnection, as the same requests.
class PhoneNumberServiceTests {
    private static final String CUSTOMER = "C-10234";
    private static final PhoneNumberInput MOBILE = new PhoneNumberInput("Mobile", "", "+64", "21 123 4567", true, true);

    private final FakeConnection connection = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(connection);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final InMemoryChangeQueue queue = new InMemoryChangeQueue();
    private final SyncService sync = new SyncService(queue, gateway, executor);
    private final PhoneNumberService service = new PhoneNumberService(gateway, sync);

    @AfterEach
    void tearDown() {
        sync.close();
    }

    // --- checking the form ---

    @Test
    void acceptsACompletePhoneNumber() {
        assertTrue(service.validate(MOBILE).isValid());
    }

    @Test
    void needsATypeAndANumber() {
        assertEquals(List.of(PhoneNumberInput.TYPE, PhoneNumberInput.NUMBER),
                fieldsWithErrors(new PhoneNumberInput(" ", "", "", "", false, false)));
    }

    @Test
    void anOtherTypeNeedsADescription() {
        assertEquals(List.of(PhoneNumberInput.OTHER_TYPE),
                fieldsWithErrors(new PhoneNumberInput("Other", "", "", "09 123 4567", false, false)));
    }

    @Test
    void checksTheNumberWithTheSharedPhoneValidator() {
        List<FieldError> errors = service.validate(new PhoneNumberInput("Home", "", "", "call me", false, false)).getErrors();

        assertEquals(PhoneNumberInput.NUMBER, errors.get(0).getFieldName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+64", "64", "+1", " +44 "})
    void acceptsACountryCode(String countryCode) {
        assertTrue(service.validate(new PhoneNumberInput("Home", "", countryCode, "09 123 4567", false, false)).isValid());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+6400", "NZ", "+ 64"})
    void rejectsSomethingThatIsNotACountryCode(String countryCode) {
        List<FieldError> errors = service.validate(new PhoneNumberInput("Home", "", countryCode, "09 123 4567", false, false))
                .getErrors();

        assertEquals(PhoneNumberInput.COUNTRY_CODE, errors.get(0).getFieldName());
        assertEquals("must be like +64", errors.get(0).getMessage());
    }

    // --- listing ---

    @Test
    void listsEveryPhoneNumberAndOnlyReadsAgainForAnOtherDescription() {
        givenListPage(1, 2, row(3, "Mobile", "y", "y"), row(4, "Other", "n", "n"));
        connection.whenRequested("customerPhoneNumber.retrieve", Map.of("id", "4"),
                FakeConnection.success(Map.of("id", "4", "type", "Other", "otherType", "Pager")));

        ServiceResult<List<PhoneNumber>> result = service.listPhoneNumbers(CUSTOMER);

        assertTrue(result.isSuccessful());
        PhoneNumber mobile = result.getValue().get(0);
        assertAll(
                () -> assertEquals(3L, mobile.getId()),
                () -> assertEquals("+64", mobile.getCountryCode()),
                () -> assertEquals("21 123 4567", mobile.getNumber()),
                () -> assertTrue(mobile.isPrimary()),
                () -> assertTrue(mobile.canTxt()),
                () -> assertNull(mobile.getOtherType()));
        assertEquals("Pager", result.getValue().get(1).getOtherType());
        assertFalse(result.getValue().get(1).canTxt());
        assertEquals(1, sent("customerPhoneNumber.retrieve").size());
    }

    @Test
    void readsEveryPageAndStopsOnAnEmptyOne() {
        givenListPage(1, 5, row(3, "Mobile", "y", "y"));
        givenListPage(2, 5);

        assertEquals(1, service.listPhoneNumbers(CUSTOMER).getValue().size());
        assertEquals(2, sent("customerPhoneNumber.list").size());
    }

    @Test
    void readsTheNextPageUntilItHasThemAll() {
        givenListPage(1, 2, row(3, "Mobile", "y", "y"));
        givenListPage(2, 2, row(4, "Home", "n", "n"));

        assertEquals(2, service.listPhoneNumbers(CUSTOMER).getValue().size());
    }

    @Test
    void reportsAListTheMainframeRefused() {
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.rejected("no such customer"));

        assertEquals(ServiceResult.Outcome.REJECTED, service.listPhoneNumbers(CUSTOMER).getOutcome());
    }

    @Test
    void reportsAnOtherDescriptionThatCouldNotBeRead() {
        givenListPage(1, 1, row(4, "Other", "n", "n"));
        connection.whenRequested("customerPhoneNumber.retrieve", FakeConnection.networkDown());

        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, service.listPhoneNumbers(CUSTOMER).getOutcome());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void needsACustomerNumber(String customerNumber) {
        assertFalse(service.listPhoneNumbers(customerNumber).getValidationErrors().isValid());
        assertFalse(service.addPhoneNumber(customerNumber, MOBILE).getValidationErrors().isValid());
        assertFalse(service.updatePhoneNumber(customerNumber, 3, MOBILE).getValidationErrors().isValid());
        assertFalse(service.deletePhoneNumber(customerNumber, 3).getValidationErrors().isValid());
        assertTrue(connection.getSentRequests().isEmpty());
        assertTrue(queue.all().isEmpty(), "nothing is queued when the checks fail");
    }

    // --- adding, updating and deleting ---

    @Test
    void addsAPhoneNumberAndReturnsWhatWasSaved() {
        connection.whenRequested("customer.addPhoneNumber", FakeConnection.success(phoneReply(5)));

        ServiceResult<PhoneNumber> result = service.addPhoneNumber(CUSTOMER,
                new PhoneNumberInput("Mobile", "ignored", " ", " 21 123 4567 ", false, true));

        assertTrue(result.isSuccessful());
        // what was saved, as sent; the new id comes from listing again
        assertNull(result.getValue().getId());
        assertEquals("21 123 4567", result.getValue().getNumber());
        assertTrue(result.getValue().canTxt());
        MessageRequest sent = connection.getSentRequests().get(0);
        assertAll(
                () -> assertEquals(CUSTOMER, sent.getValue("customer")),
                () -> assertEquals("21 123 4567", sent.getValue("number")),
                () -> assertNull(sent.getValue("countryCode"), "a blank country code is sent as nothing"),
                () -> assertNull(sent.getValue("otherType")),
                () -> assertEquals("n", sent.getValue("isPrimary")),
                () -> assertEquals("y", sent.getValue("canTxt")));
    }

    @Test
    void doesNotContactTheMainframeWhenTheFormHasErrors() {
        service.addPhoneNumber(CUSTOMER, new PhoneNumberInput("Mobile", "", "", "", false, false));

        assertTrue(connection.getSentRequests().isEmpty());
    }

    @Test
    void updatesAPhoneNumberById() {
        connection.whenRequested("customer.updatePhoneNumber", FakeConnection.success(phoneReply(3)));

        ServiceResult<PhoneNumber> result = service.updatePhoneNumber(CUSTOMER, 3,
                new PhoneNumberInput("Other", " Pager ", "+64", "21 123 4567", true, false));

        assertTrue(result.isSuccessful());
        MessageRequest sent = connection.getSentRequests().get(0);
        assertEquals("3", sent.getValue("id"));
        assertEquals("Pager", sent.getValue("otherType"));
        assertEquals("+64", sent.getValue("countryCode"));
    }

    @Test
    void reportsAnUpdateTheMainframeRefused() {
        connection.whenRequested("customer.updatePhoneNumber", FakeConnection.rejected("no such phone number"));

        ServiceResult<PhoneNumber> result = service.updatePhoneNumber(CUSTOMER, 3, MOBILE);

        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("no such phone number", result.getMessage());
        assertNull(result.getValue());
    }

    @Test
    void keepsAPhoneNumberAddedOfflineToUploadLater() {
        connection.whenRequested("customer.addPhoneNumber", FakeConnection.networkDown());

        ServiceResult<PhoneNumber> result = service.addPhoneNumber(CUSTOMER, MOBILE);

        assertTrue(result.isSaved());
        assertTrue(result.isWaitingToUpload());
        assertEquals("21 123 4567", result.getValue().getNumber());
        assertEquals("customer.addPhoneNumber", queue.all().get(0).getMessageType());
    }

    @Test
    void keepsAnUpdateMadeOfflineToUploadLater() {
        connection.whenRequested("customer.updatePhoneNumber", FakeConnection.networkDown());

        ServiceResult<PhoneNumber> result = service.updatePhoneNumber(CUSTOMER, 3, MOBILE);

        assertTrue(result.isWaitingToUpload());
        assertEquals(3L, result.getValue().getId());
    }

    @Test
    void keepsADeleteMadeOfflineToUploadLater() {
        connection.whenRequested("customer.deletePhoneNumber", FakeConnection.networkDown());

        assertTrue(service.deletePhoneNumber(CUSTOMER, 3).isWaitingToUpload());
        assertEquals("customer.deletePhoneNumber", queue.all().get(0).getMessageType());
    }

    @Test
    void deletesAPhoneNumberById() {
        connection.whenRequested("customer.deletePhoneNumber", FakeConnection.success(Map.of("id", "3", "customer", CUSTOMER)));

        assertTrue(service.deletePhoneNumber(CUSTOMER, 3).isSuccessful());
        assertEquals("3", connection.getSentRequests().get(0).getValue("id"));
    }

    @Test
    void fillsTheFormFromASavedPhoneNumber() {
        PhoneNumber saved = new PhoneNumber(CUSTOMER, "Mobile", null, "021 123 4567", true, false);

        PhoneNumberInput input = PhoneNumberInput.fromPhoneNumber(saved);

        assertAll(
                () -> assertEquals("Mobile", input.getType()),
                () -> assertEquals("", input.getOtherType()),
                () -> assertEquals("", input.getCountryCode()),
                () -> assertEquals("021 123 4567", input.getNumber()),
                () -> assertTrue(input.isPrimary()),
                () -> assertFalse(input.canTxt()));
    }

    // --- helpers ---

    private List<String> fieldsWithErrors(PhoneNumberInput input) {
        return service.validate(input).getErrors().stream().map(FieldError::getFieldName).toList();
    }

    private List<MessageRequest> sent(String requestType) {
        return connection.getSentRequests().stream().filter(r -> requestType.equals(r.getRequestType())).toList();
    }

    // one row of a customerPhoneNumber.list reply: id, type, isPrimary, canTxt
    private static String[] row(long id, String type, String primary, String canTxt) {
        return new String[] {Long.toString(id), type, primary, canTxt};
    }

    private void givenListPage(long page, long total, String[]... rows) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(rows.length));
        for (int i = 1; i <= rows.length; i++) {
            values.put(i + ":id", rows[i - 1][0]);
            values.put(i + ":type", rows[i - 1][1]);
            values.put(i + ":isPrimary", rows[i - 1][2]);
            values.put(i + ":canTxt", rows[i - 1][3]);
            values.put(i + ":countryCode", "+64");
            values.put(i + ":number", "21 123 4567");
        }
        connection.whenRequested("customerPhoneNumber.list", Map.of("page", Long.toString(page)),
                FakeConnection.success(values));
    }

    private static Map<String, String> phoneReply(long id) {
        Map<String, String> values = new HashMap<>();
        values.put("id", Long.toString(id));
        values.put("type", "Mobile");
        values.put("countryCode", "+64");
        values.put("number", "21 123 4567");
        values.put("isPrimary", "n");
        values.put("canTxt", "y");
        return values;
    }
}
