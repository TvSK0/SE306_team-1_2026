package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.model.Address;
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
class AddressServiceTests {
    private static final String CUSTOMER = "C-10234";
    private static final AddressInput HOME = new AddressInput("Residential", "", "1 Queen St", "Unit 2", "CBD",
            "Auckland", "1010", "New Zealand", true, true);

    private final FakeConnection connection = new FakeConnection();
    private final MainframeGateway gateway = new MainframeGateway(connection);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final InMemoryChangeQueue queue = new InMemoryChangeQueue();
    private final SyncService sync = new SyncService(queue, gateway, executor);
    private final AddressService service = new AddressService(gateway, sync);

    @AfterEach
    void tearDown() {
        sync.close();
    }

    // --- checking the form ---

    @Test
    void acceptsACompleteNewZealandAddress() {
        assertTrue(service.validate(HOME).isValid());
    }

    @Test
    void needsATypeAFirstLineACityAndACountry() {
        AddressInput empty = new AddressInput(null, "", " ", "", "", "", "", "", false, false);

        assertEquals(List.of(AddressInput.TYPE, AddressInput.LINE1, AddressInput.CITY, AddressInput.COUNTRY),
                fieldsWithErrors(empty));
    }

    @Test
    void anOtherTypeNeedsADescription() {
        AddressInput bach = new AddressInput("Other", " ", "2 Beach Rd", "", "Piha", "Auckland", "0772",
                "New Zealand", false, false);

        assertEquals(List.of(AddressInput.OTHER_TYPE), fieldsWithErrors(bach));
    }

    @ParameterizedTest
    @ValueSource(strings = {"New Zealand", " new zealand "})
    void aNewZealandAddressNeedsAPostcode(String country) {
        AddressInput noPostcode = new AddressInput("Residential", "", "1 Queen St", "", "", "Auckland", "",
                country, false, false);

        assertEquals(List.of(AddressInput.POST_CODE), fieldsWithErrors(noPostcode));
    }

    @Test
    void aNewZealandPostcodeHasFourDigits() {
        AddressInput badPostcode = new AddressInput("Residential", "", "1 Queen St", "", "", "Auckland", "10100",
                "New Zealand", false, false);

        List<FieldError> errors = service.validate(badPostcode).getErrors();

        assertEquals(AddressInput.POST_CODE, errors.get(0).getFieldName());
        assertEquals("must be a 4-digit New Zealand postcode", errors.get(0).getMessage());
    }

    @Test
    void aMissingCountryIsAskedForButNotTakenAsNewZealand() {
        AddressInput noCountry = new AddressInput("Residential", "", "1 Queen St", "", "", "Auckland", "", null,
                false, false);

        assertEquals(List.of(AddressInput.COUNTRY), fieldsWithErrors(noCountry));
    }

    @Test
    void sendsNothingForOptionalFieldsThatWereNeverFilledIn() {
        connection.whenRequested("customer.addAddress", FakeConnection.success(addressReply(9, "Residential", null, "n", "n")));

        service.addAddress(CUSTOMER, new AddressInput("Residential", null, "10 Downing St", null, null, "London",
                null, "United Kingdom", false, false));

        MessageRequest sent = connection.getSentRequests().get(0);
        assertNull(sent.getValue("line2"));
        assertNull(sent.getValue("suburb"));
        assertNull(sent.getValue("postCode"));
    }

    @Test
    void anOverseasPostcodeIsOptionalAndNotChecked() {
        assertTrue(service.validate(new AddressInput("Residential", "", "10 Downing St", "", "", "London", "",
                "United Kingdom", false, false)).isValid());
        assertTrue(service.validate(new AddressInput("Residential", "", "10 Downing St", "", "", "London",
                "SW1A 2AA", "United Kingdom", false, false)).isValid());
    }

    // --- listing ---

    @Test
    void listsEveryAddressWithItsFullDetails() {
        givenListPage(1, 2, 7, 8);
        givenAddress(7, "Residential", null, "y", "y");
        givenAddress(8, "Other", "Bach", "n", "n");

        ServiceResult<List<Address>> result = service.listAddresses(CUSTOMER);

        assertTrue(result.isSuccessful());
        List<Address> addresses = result.getValue();
        assertEquals(2, addresses.size());
        Address home = addresses.get(0);
        assertAll(
                () -> assertEquals(7L, home.getId()),
                () -> assertEquals(CUSTOMER, home.getCustomerNumber()),
                () -> assertEquals("1 Queen St", home.getLine1()),
                () -> assertEquals("Unit 2", home.getLine2()),
                () -> assertEquals("CBD", home.getSuburb()),
                () -> assertEquals("1010", home.getPostCode()),
                () -> assertTrue(home.isPrimary()),
                () -> assertTrue(home.isMailing()));
        assertEquals("Bach", addresses.get(1).getOtherType());
        assertFalse(addresses.get(1).isMailing());
    }

    @Test
    void readsEveryPageAndStopsOnAnEmptyOne() {
        givenListPage(1, 5, 7);
        givenListPage(2, 5);
        givenAddress(7, "Residential", null, "y", "y");

        ServiceResult<List<Address>> result = service.listAddresses(CUSTOMER);

        assertEquals(1, result.getValue().size());
        assertEquals(2, sent("customerAddress.list").size());
    }

    @Test
    void readsTheNextPageUntilItHasThemAll() {
        givenListPage(1, 2, 7);
        givenListPage(2, 2, 8);
        givenAddress(7, "Residential", null, "y", "y");
        givenAddress(8, "Postal", null, "n", "y");

        assertEquals(2, service.listAddresses(CUSTOMER).getValue().size());
    }

    @Test
    void reportsAListTheMainframeRefused() {
        connection.whenRequested("customerAddress.list", FakeConnection.rejected("no such customer"));

        ServiceResult<List<Address>> result = service.listAddresses(CUSTOMER);

        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertNull(result.getValue());
    }

    @Test
    void reportsAnAddressThatCouldNotBeRead() {
        givenListPage(1, 1, 7);
        connection.whenRequested("customerAddress.retrieve", FakeConnection.networkDown());

        ServiceResult<List<Address>> result = service.listAddresses(CUSTOMER);

        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, result.getOutcome());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void needsACustomerNumber(String customerNumber) {
        assertFalse(service.listAddresses(customerNumber).getValidationErrors().isValid());
        assertFalse(service.addAddress(customerNumber, HOME).getValidationErrors().isValid());
        assertFalse(service.updateAddress(customerNumber, 7, HOME).getValidationErrors().isValid());
        assertFalse(service.deleteAddress(customerNumber, 7).getValidationErrors().isValid());
        assertTrue(connection.getSentRequests().isEmpty());
        assertTrue(queue.all().isEmpty(), "nothing is queued when the checks fail");
    }

    // --- adding, updating and deleting ---

    @Test
    void addsAnAddressAndReturnsWhatWasSaved() {
        connection.whenRequested("customer.addAddress", FakeConnection.success(addressReply(9, "Residential", null, "y", "y")));

        ServiceResult<Address> result = service.addAddress(CUSTOMER, new AddressInput("Residential", "ignored",
                " 1 Queen St ", " ", "", " Auckland ", " 1010 ", " New Zealand ", true, false));

        assertTrue(result.isSuccessful());
        // what was saved, as sent; the new id comes from listing again
        assertNull(result.getValue().getId());
        assertEquals("1 Queen St", result.getValue().getLine1());
        assertEquals("Auckland", result.getValue().getCity());
        assertTrue(result.getValue().isPrimary());
        assertFalse(result.getValue().isMailing());
        MessageRequest sent = connection.getSentRequests().get(0);
        assertAll(
                () -> assertEquals(CUSTOMER, sent.getValue("customer")),
                () -> assertEquals("1 Queen St", sent.getValue("line1")),
                () -> assertNull(sent.getValue("line2"), "blank optional fields are sent as nothing"),
                () -> assertNull(sent.getValue("suburb")),
                () -> assertEquals("Auckland", sent.getValue("city")),
                () -> assertEquals("1010", sent.getValue("postCode")),
                () -> assertEquals("y", sent.getValue("isPrimary")),
                () -> assertEquals("n", sent.getValue("isMailing")),
                () -> assertNull(sent.getValue("otherType"), "only an Other type sends a description"));
    }

    @Test
    void sendsTheDescriptionOfAnOtherType() {
        connection.whenRequested("customer.addAddress", FakeConnection.success(addressReply(9, "Other", "Bach", "n", "n")));

        service.addAddress(CUSTOMER, new AddressInput("Other", " Bach ", "2 Beach Rd", "", "", "Auckland", "0772",
                "New Zealand", false, false));

        assertEquals("Bach", connection.getSentRequests().get(0).getValue("otherType"));
    }

    @Test
    void doesNotContactTheMainframeWhenTheFormHasErrors() {
        ServiceResult<Address> result = service.addAddress(CUSTOMER,
                new AddressInput("Residential", "", "", "", "", "Auckland", "1010", "New Zealand", false, false));

        assertEquals(List.of(AddressInput.LINE1), result.getValidationErrors().getErrors().stream()
                .map(FieldError::getFieldName).toList());
        assertTrue(connection.getSentRequests().isEmpty());
    }

    @Test
    void updatesAnAddressById() {
        connection.whenRequested("customer.updateAddress", FakeConnection.success(addressReply(7, "Postal", null, "n", "y")));

        ServiceResult<Address> result = service.updateAddress(CUSTOMER, 7, HOME);

        assertTrue(result.isSuccessful());
        assertEquals("7", connection.getSentRequests().get(0).getValue("id"));
        assertEquals(7L, result.getValue().getId());
        assertEquals("Residential", result.getValue().getType());
    }

    @Test
    void reportsAnUpdateTheMainframeRefused() {
        connection.whenRequested("customer.updateAddress", FakeConnection.rejected("no such address"));

        ServiceResult<Address> result = service.updateAddress(CUSTOMER, 7, HOME);

        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("no such address", result.getMessage());
        assertNull(result.getValue());
    }

    @Test
    void keepsAnAddressAddedOfflineToUploadLater() {
        connection.whenRequested("customer.addAddress", FakeConnection.networkDown());

        ServiceResult<Address> result = service.addAddress(CUSTOMER, HOME);

        assertFalse(result.isSuccessful());
        assertTrue(result.isSaved());
        assertTrue(result.isWaitingToUpload());
        assertEquals("1 Queen St", result.getValue().getLine1());
        assertEquals(1, queue.all().size(), "the address waits on the upload queue");
        assertEquals("customer.addAddress", queue.all().get(0).getMessageType());
    }

    @Test
    void keepsAnUpdateMadeOfflineToUploadLater() {
        connection.whenRequested("customer.updateAddress", FakeConnection.networkDown());

        ServiceResult<Address> result = service.updateAddress(CUSTOMER, 7, HOME);

        assertTrue(result.isWaitingToUpload());
        assertEquals(7L, result.getValue().getId());
        assertEquals("7", queue.all().get(0).getFields().get("id"));
    }

    @Test
    void keepsADeleteMadeOfflineToUploadLater() {
        connection.whenRequested("customer.deleteAddress", FakeConnection.networkDown());

        ServiceResult<Void> result = service.deleteAddress(CUSTOMER, 7);

        assertTrue(result.isWaitingToUpload());
        assertEquals("customer.deleteAddress", queue.all().get(0).getMessageType());
    }

    @Test
    void deletesAnAddressById() {
        connection.whenRequested("customer.deleteAddress", FakeConnection.success(Map.of("id", "7", "customer", CUSTOMER)));

        ServiceResult<Void> result = service.deleteAddress(CUSTOMER, 7);

        assertTrue(result.isSuccessful());
        MessageRequest sent = connection.getSentRequests().get(0);
        assertEquals("7", sent.getValue("id"));
        assertEquals(CUSTOMER, sent.getValue("customer"));
    }

    @Test
    void fillsTheFormFromASavedAddress() {
        Address saved = new Address(CUSTOMER, "Other", "2 Beach Rd", null, null, "Auckland", "0772",
                "New Zealand", false, true);
        saved.setOtherType("Bach");

        AddressInput input = AddressInput.fromAddress(saved);

        assertAll(
                () -> assertEquals("Other", input.getType()),
                () -> assertEquals("Bach", input.getOtherType()),
                () -> assertEquals("", input.getLine2()),
                () -> assertEquals("", input.getSuburb()),
                () -> assertTrue(input.isMailing()),
                () -> assertFalse(input.isPrimary()));
    }

    // --- helpers ---

    private List<String> fieldsWithErrors(AddressInput input) {
        return service.validate(input).getErrors().stream().map(FieldError::getFieldName).toList();
    }

    private List<MessageRequest> sent(String requestType) {
        return connection.getSentRequests().stream().filter(r -> requestType.equals(r.getRequestType())).toList();
    }

    private void givenListPage(long page, long total, long... ids) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Long.toString(total));
        values.put("items", Integer.toString(ids.length));
        for (int i = 1; i <= ids.length; i++) {
            values.put(i + ":id", Long.toString(ids[i - 1]));
            values.put(i + ":type", "Residential");
        }
        connection.whenRequested("customerAddress.list", Map.of("page", Long.toString(page)),
                FakeConnection.success(values));
    }

    private void givenAddress(long id, String type, String otherType, String primary, String mailing) {
        connection.whenRequested("customerAddress.retrieve", Map.of("id", Long.toString(id)),
                FakeConnection.success(addressReply(id, type, otherType, primary, mailing)));
    }

    private static Map<String, String> addressReply(long id, String type, String otherType, String primary, String mailing) {
        Map<String, String> values = new HashMap<>();
        values.put("id", Long.toString(id));
        values.put("type", type);
        if (otherType != null) {
            values.put("otherType", otherType);
        }
        values.put("line1", "1 Queen St");
        values.put("line2", "Unit 2");
        values.put("suburb", "CBD");
        values.put("city", "Auckland");
        values.put("postCode", "1010");
        values.put("country", "New Zealand");
        values.put("isPrimary", primary);
        values.put("isMailing", mailing);
        return values;
    }
}
