package lavs.ui;

import lavs.data.MainframeGateway;
import lavs.logic.CustomerInput;
import lavs.logic.CustomerService;
import lavs.logic.ServiceResult;
import lavs.model.Customer;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. The results come from a real service over a FakeConnection, so these also
// check that the wording matches how results are built.
class ResultTextTests {
    private static final CustomerInput JANE = new CustomerInput("", "Jane", "Smith", "2001-02-03", "");

    private final FakeConnection connection = new FakeConnection();
    private final CustomerService service = new CustomerService(new MainframeGateway(connection), 1);

    @Test
    void saysWhatWorked() {
        connection.whenRequested("customer.create", FakeConnection.success(Map.of("number", "C-1")));

        assertEquals("Customer created.", ResultText.forChange(service.createCustomer(JANE), "Customer created."));
    }

    @Test
    void asksToFixTheFieldsWhenTheChecksStoppedIt() {
        ServiceResult<Customer> result = service.createCustomer(new CustomerInput("", "", "Smith", "2001-02-03", ""));

        assertEquals(ResultText.FIX_FIELDS, ResultText.forChange(result, "Customer created."));
    }

    @Test
    void saysWhenTheMainframeCannotBeReached() {
        connection.setIsConnected(false);

        assertEquals(ResultText.NO_CONNECTION, ResultText.forChange(service.createCustomer(JANE), "Customer created."));
        assertEquals(ResultText.NO_CONNECTION, ResultText.forLoad(service.loadCustomer("C-1"), "The customer"));
    }

    @Test
    void passesOnWhyTheMainframeRefused() {
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
