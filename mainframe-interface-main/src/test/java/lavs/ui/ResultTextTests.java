package lavs.ui;

import lavs.data.LocalDatabase;
import lavs.data.MainframeGateway;
import lavs.data.SqlitePendingChangeStore;
import lavs.logic.AddressInput;
import lavs.logic.AddressService;
import lavs.logic.CustomerInput;
import lavs.logic.CustomerService;
import lavs.logic.ServiceResult;
import lavs.logic.SyncService;
import lavs.model.Address;
import lavs.model.Customer;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. The results come from a real service over a FakeConnection, so these also
// check that the wording matches how results are built.
class ResultTextTests {
    private static final CustomerInput JANE = new CustomerInput("", "Jane", "Smith", "2001-02-03", "");

    private final FakeConnection connection = new FakeConnection();
    private CustomerService service;
    // AI-assisted: saves go through the upload queue (US11), kept in a SQLite file in a temp folder;
    // a new customer is searched for first (the duplicate check)
    private SyncService _sync;

    @BeforeEach
    void setUp() {
        MainframeGateway gateway = new MainframeGateway(connection);
        _sync = SyncService.create(new SqlitePendingChangeStore(new LocalDatabase(folder.resolve("customers.db"))),
                gateway);
        service = new CustomerService(gateway, _sync, 1);
    }

    @AfterEach
    void tearDown() {
        _sync.close();
    }

    private void givenNoExistingCustomers() {
        connection.whenRequested("customer.search", FakeConnection.success(Map.of("page", "1", "total", "0", "items", "0")));
    }

    @Test
    void saysWhatWorked() {
        givenNoExistingCustomers();
        connection.whenRequested("customer.create", FakeConnection.success(Map.of("number", "C-1")));

        assertEquals("Customer created.", ResultText.forChange(service.createCustomer(JANE), "Customer created."));
    }

    // AI-assisted: a new customer who may already be on the mainframe waits for the MMM (US11)
    @Test
    void saysANewCustomerWhoMayAlreadyExistIsWaitingForTheMmm() {
        Map<String, String> found = new java.util.HashMap<>(Map.of("page", "1", "total", "1", "items", "1"));
        found.put("1:number", "C-777");
        found.put("1:firstName", "Jane");
        found.put("1:familyName", "Smith");
        found.put("1:dateOfBirth", "2001-02-03");
        connection.whenRequested("customer.search", FakeConnection.success(found));

        assertEquals("Customer created. " + ResultText.HELD_FOR_REVIEW,
                ResultText.forChange(service.createCustomer(JANE), "Customer created."));
    }

    @Test
    void asksToFixTheFieldsWhenTheChecksStoppedIt() {
        ServiceResult<Customer> result = service.createCustomer(new CustomerInput("", "", "Smith", "2001-02-03", ""));

        assertEquals(ResultText.FIX_FIELDS, ResultText.forChange(result, "Customer created."));
    }

    @TempDir
    Path folder;

    // AI-assisted: a save kept on the upload queue (US11) reads as done, plus that it will upload
    @Test
    void saysASaveIsKeptOnThisLaptopWhenItCannotUploadYet() {
        connection.whenRequested("customer.addAddress", FakeConnection.networkDown());
        MainframeGateway gateway = new MainframeGateway(connection);
        try (SyncService sync = SyncService.create(
                new SqlitePendingChangeStore(new LocalDatabase(folder.resolve("lavs.db"))), gateway)) {
            ServiceResult<Address> result = new AddressService(gateway, sync).addAddress("C-1",
                    new AddressInput("Residential", "", "1 Queen St", "", "", "Auckland", "1010", "New Zealand",
                            false, true));

            assertEquals("Address added. " + ResultText.WAITING_TO_UPLOAD,
                    ResultText.forChange(result, "Address added."));
        }
    }

    // AI-assisted: since US11 a save that can't reach the mainframe is kept; a read still can't be done
    @Test
    void saysWhenTheMainframeCannotBeReached() {
        connection.setIsConnected(false);

        assertEquals("Customer created. " + ResultText.WAITING_TO_UPLOAD,
                ResultText.forChange(service.createCustomer(JANE), "Customer created."));
        assertEquals(ResultText.NO_CONNECTION, ResultText.forLoad(service.loadCustomer("C-1"), "The customer"));
    }

    @Test
    void passesOnWhyTheMainframeRefused() {
        givenNoExistingCustomers();
        connection.whenRequested("customer.create", FakeConnection.rejected("customer already exists"));
        connection.whenRequested("customer.retrieve", FakeConnection.rejected("no such customer"));

        assertEquals("The mainframe rejected the change: customer already exists",
                ResultText.forChange(service.createCustomer(JANE), "Customer created."));
        assertEquals("The customer could not be loaded: no such customer",
                ResultText.forLoad(service.loadCustomer("C-404"), "The customer"));
    }

    @Test
    void namesTheMissingFieldWhenALoadWasStoppedByTheChecks() {
        assertEquals("The customer could not be loaded: customerNumber is required",
                ResultText.forLoad(service.loadCustomer(" "), "The customer"));
    }

    @Test
    void treatsAnUncheckedIOExceptionAsTheMainframeBeingUnreachable() {
        assertEquals(ResultText.NO_CONNECTION, ResultText.forError(new UncheckedIOException(new IOException("timed out"))));
        assertEquals("Something went wrong: boom", ResultText.forError(new IllegalStateException("boom")));
    }

    @Test
    void startsAFieldMessageWithACapital() {
        assertEquals("Is required", ResultText.capitalise("is required"));
        assertEquals("", ResultText.capitalise(""));
    }
}
