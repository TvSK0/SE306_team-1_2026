package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.model.Customer;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import mainframe.client.MessageRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted
class CustomerServiceTests {
    private static final long ORGANISATION = 1;
    // "today" for every test: 22 October 2026
    private static final Clock TODAY = Clock.fixed(Instant.parse("2026-10-22T10:00:00Z"), ZoneOffset.UTC);
    private static final CustomerInput JANE = new CustomerInput("Ms", "Jane", "Smith", "2001-02-03", "New Zealand");

    private final FakeConnection connection = new FakeConnection();
    private final CustomerService service = new CustomerService(new MainframeGateway(connection), ORGANISATION, TODAY);

    // --- checking the form ---

    @Test
    void acceptsACompleteForm() {
        assertTrue(service.validate(JANE).isValid());
    }

    @Test
    void titleAndNationalityAreOptional() {
        assertTrue(service.validate(new CustomerInput("", "Jane", "Smith", "2001-02-03", null)).isValid());
    }

    @Test
    void needsAFirstNameAFamilyNameAndADateOfBirth() {
        List<String> fields = fieldsWithErrors(new CustomerInput("Ms", " ", null, "", "New Zealand"));

        assertEquals(List.of(CustomerInput.FIRST_NAME, CustomerInput.FAMILY_NAME, CustomerInput.DATE_OF_BIRTH), fields);
    }

    @ParameterizedTest
    @ValueSource(strings = {"03/02/2001", "2001-02-30", "yesterday"})
    void needsARealDateInYearMonthDayForm(String dateOfBirth) {
        List<FieldError> errors = service.validate(new CustomerInput("", "Jane", "Smith", dateOfBirth, "")).getErrors();

        assertEquals(1, errors.size());
        assertEquals(CustomerInput.DATE_OF_BIRTH, errors.get(0).getFieldName());
    }

    @Test
    void aDateOfBirthCannotBeInTheFuture() {
        List<FieldError> errors = service.validate(new CustomerInput("", "Jane", "Smith", "2026-10-23", "")).getErrors();

        assertEquals("cannot be in the future", errors.get(0).getMessage());
    }

    @Test
    void aDateOfBirthCanBeToday() {
        assertTrue(service.validate(new CustomerInput("", "Jane", "Smith", " 2026-10-22 ", "")).isValid());
    }

    // --- creating ---

    @Test
    void createsTheCustomerAndReturnsTheirNewNumber() {
        connection.whenRequested("customer.create", FakeConnection.success(customerReply("C-10234")));

        ServiceResult<Customer> result = service.createCustomer(new CustomerInput(" Ms ", " Jane ", " Smith ", "2001-02-03", " New Zealand "));

        assertTrue(result.isSuccessful());
        Customer saved = result.getValue();
        assertAll(
                () -> assertEquals("C-10234", saved.getNumber()),
                () -> assertEquals("Jane Smith", saved.getFullName()),
                () -> assertEquals(LocalDate.of(2001, 2, 3), saved.getDateOfBirth()),
                () -> assertEquals("Active", saved.getStatus()));
        MessageRequest sent = connection.getSentRequests().get(0);
        assertAll(
                () -> assertEquals("Jane", sent.getValue("firstName"), "trimmed"),
                () -> assertEquals("Smith", sent.getValue("familyName")),
                () -> assertEquals("Ms", sent.getValue("title")),
                () -> assertEquals("New Zealand", sent.getValue("nationality")),
                () -> assertEquals("2001-02-03", sent.getValue("dateOfBirth")),
                () -> assertEquals(Long.toString(ORGANISATION), sent.getValue("organisation")));
    }

    @Test
    void sendsNothingForBlankOptionalFields() {
        connection.whenRequested("customer.create", FakeConnection.success(customerReply("C-1")));

        service.createCustomer(new CustomerInput("  ", "Jane", "Smith", "2001-02-03", ""));
        service.createCustomer(new CustomerInput(null, "Jane", "Smith", "2001-02-03", null));

        for (MessageRequest sent : connection.getSentRequests()) {
            assertNull(sent.getValue("title"));
            assertNull(sent.getValue("nationality"));
        }
    }

    @Test
    void doesNotContactTheMainframeWhenTheFormHasErrors() {
        ServiceResult<Customer> result = service.createCustomer(new CustomerInput("", "", "Smith", "2001-02-03", ""));

        assertFalse(result.isSuccessful());
        assertNull(result.getOutcome());
        assertNull(result.getValue());
        assertEquals(CustomerInput.FIRST_NAME, result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(connection.getSentRequests().isEmpty());
    }

    @Test
    void reportsACreateTheMainframeRefused() {
        connection.whenRequested("customer.create", FakeConnection.rejected("duplicate customer"));

        ServiceResult<Customer> result = service.createCustomer(JANE);

        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("duplicate customer", result.getMessage());
        assertNull(result.getValue());
    }

    @Test
    void reportsACreateThatCouldNotReachTheMainframe() {
        connection.setIsConnected(false);

        ServiceResult<Customer> result = service.createCustomer(JANE);

        assertEquals(ServiceResult.Outcome.NETWORK_FAILURE, result.getOutcome());
    }

    // --- editing ---

    @Test
    void savesChangesToAnExistingCustomer() {
        connection.whenRequested("customer.update", FakeConnection.success(customerReply("C-10234")));

        ServiceResult<Customer> result = service.updateCustomer(" C-10234 ", JANE);

        assertTrue(result.isSuccessful());
        assertEquals("C-10234", connection.getSentRequests().get(0).getValue("number"));
        assertEquals("Jane", result.getValue().getFirstName());
    }

    @Test
    void anEditNeedsTheCustomerNumberAndAValidForm() {
        ServiceResult<Customer> result = service.updateCustomer("", new CustomerInput("", "Jane", "", "2001-02-03", ""));

        assertEquals(List.of(CustomerInput.FAMILY_NAME, CustomerService.CUSTOMER_NUMBER),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        assertTrue(connection.getSentRequests().isEmpty());
    }

    // --- loading ---

    @Test
    void loadsACustomersDetails() {
        connection.whenRequested("customer.retrieve", Map.of("number", "C-10234"),
                FakeConnection.success(customerReply("C-10234")));

        ServiceResult<Customer> result = service.loadCustomer(" C-10234 ");

        assertTrue(result.isSuccessful());
        assertEquals("Ms", result.getValue().getTitle());
        assertEquals("New Zealand", result.getValue().getNationality());
    }

    @Test
    void leavesTheDateOfBirthEmptyWhenTheMainframeHasNoneOrOneItCannotRead() {
        Map<String, String> noDate = customerReply("C-1");
        noDate.remove("dateOfBirth");
        Map<String, String> badDate = customerReply("C-2");
        badDate.put("dateOfBirth", "03/02/2001");
        connection.whenRequested("customer.retrieve", Map.of("number", "C-1"), FakeConnection.success(noDate));
        connection.whenRequested("customer.retrieve", Map.of("number", "C-2"), FakeConnection.success(badDate));

        assertNull(service.loadCustomer("C-1").getValue().getDateOfBirth());
        assertNull(service.loadCustomer("C-2").getValue().getDateOfBirth());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void needsANumberToLoadACustomer(String number) {
        ServiceResult<Customer> result = service.loadCustomer(number);

        assertEquals(CustomerService.CUSTOMER_NUMBER, result.getValidationErrors().getErrors().get(0).getFieldName());
        assertTrue(connection.getSentRequests().isEmpty());
    }

    @Test
    void reportsAnUnknownCustomer() {
        connection.whenRequested("customer.retrieve", FakeConnection.rejected("no such customer"));

        ServiceResult<Customer> result = service.loadCustomer("C-404");

        assertEquals(ServiceResult.Outcome.REJECTED, result.getOutcome());
        assertEquals("no such customer", result.getMainframeStatus().getErrorMessage());
    }

    @Test
    void usesTheSystemClockByDefault() {
        CustomerService real = new CustomerService(new MainframeGateway(connection), ORGANISATION);
        String tomorrow = LocalDate.now().plusDays(1).toString();

        assertFalse(real.validate(new CustomerInput("", "Jane", "Smith", tomorrow, "")).isValid());
    }

    // --- helpers ---

    private List<String> fieldsWithErrors(CustomerInput input) {
        return service.validate(input).getErrors().stream().map(FieldError::getFieldName).toList();
    }

    private static Map<String, String> customerReply(String number) {
        Map<String, String> values = new HashMap<>();
        values.put("number", number);
        values.put("title", "Ms");
        values.put("firstName", "Jane");
        values.put("familyName", "Smith");
        values.put("dateOfBirth", "2001-02-03");
        values.put("nationality", "New Zealand");
        values.put("status", "Active");
        return values;
    }
}
