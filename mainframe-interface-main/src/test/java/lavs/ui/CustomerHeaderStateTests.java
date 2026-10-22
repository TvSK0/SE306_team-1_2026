// AI-assisted: tests for what the customer record header and top bar show (Figma 03a).
package lavs.ui;

import lavs.data.MainframeGateway;
import lavs.logic.CustomerService;
import lavs.logic.ServiceResult;
import lavs.model.Customer;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class CustomerHeaderStateTests {
    // CR-R3
    @Test
    void aSavedCustomerShowsLoadingUntilItArrives() {
        // Act
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Assert
        Assertions.assertTrue(header.isSaved());
        Assertions.assertEquals("Loading…", header.name());
        Assertions.assertTrue(header.statusTag().isEmpty());
        Assertions.assertEquals("Customer no. C-10234", header.detailsLine());
        Assertions.assertEquals("Customer", header.topBarTitle());
    }

    // CR-R3
    @Test
    void aLoadedCustomerShowsNameStatusNumberAndDateOfBirth() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Act
        Optional<BannerContent> banner = header.applyLoad(load(customerReply("1988-03-14")));

        // Assert
        Assertions.assertTrue(banner.isEmpty());
        Assertions.assertEquals("Jane Smith", header.name());
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.SUCCESS, "Active"),
                header.statusTag().orElseThrow());
        Assertions.assertEquals("Customer no. C-10234 · DOB 14/03/1988", header.detailsLine());
        Assertions.assertEquals("Customer: Jane Smith (C-10234)", header.topBarTitle());
    }

    // CR-R3
    @Test
    void anUnknownDateOfBirthAndNameFallBackCleanly() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-10234");
        Customer nameless = new Customer();
        nameless.setNumber("C-10234");

        // Act
        header.setCustomer(nameless);

        // Assert
        Assertions.assertEquals("Customer C-10234", header.name());
        Assertions.assertEquals("Customer no. C-10234", header.detailsLine());
    }

    // CR-R4
    @Test
    void aNewCustomerShowsNotSavedYet() {
        // Act
        CustomerHeaderState header = new CustomerHeaderState(null);

        // Assert
        Assertions.assertFalse(header.isSaved());
        Assertions.assertEquals("New customer", header.name());
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Not saved yet"),
                header.statusTag().orElseThrow());
        Assertions.assertEquals("Customer number is assigned by the mainframe", header.detailsLine());
        Assertions.assertEquals("New customer", header.topBarTitle());
    }

    // CR-R4 (once the Details tab saves a new customer, the header names them)
    @Test
    void aNewCustomerOnceSavedIsShownLikeAnyOther() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState(null);
        Customer saved = new Customer();
        saved.setNumber("C-20001");
        saved.setFirstName("Ana");
        saved.setFamilyName("Lee");
        saved.setDateOfBirth(LocalDate.of(2000, 1, 2));

        // Act
        header.setCustomer(saved);

        // Assert
        Assertions.assertTrue(header.isSaved());
        Assertions.assertEquals("Customer: Ana Lee (C-20001)", header.topBarTitle());
        Assertions.assertEquals("Customer no. C-20001 · DOB 02/01/2000", header.detailsLine());
    }

    // CR-R3
    @Test
    void aNetworkFailureShowsTheOfflineBannerAndTheNumberAsTheName() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Act
        Optional<BannerContent> banner = header.applyLoad(load(connection));

        // Assert
        Assertions.assertEquals(BannerContent.offline(), banner.orElseThrow());
        Assertions.assertEquals("Customer C-10234", header.name());
        Assertions.assertTrue(header.statusTag().isEmpty());
    }

    // CR-R3
    @Test
    void aRefusalShowsTheMainframesReason() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.retrieve", FakeConnection.rejected("No such customer"));
        CustomerHeaderState header = new CustomerHeaderState("C-9");

        // Act
        BannerContent banner = header.applyLoad(load(connection)).orElseThrow();

        // Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't load the customer",
                "Reason given: No such customer. Go back and open the record again."), banner);
    }

    // CR-R3
    @Test
    void aRefusalWithNoReasonSaysSo() {
        Assertions.assertEquals("The mainframe gave no reason. Go back and open the record again.",
                CustomerHeaderState.couldntLoad(" ").body());
    }

    private static FakeConnection customerReply(String dateOfBirth) {
        Map<String, String> values = new HashMap<>();
        values.put("number", "C-10234");
        values.put("firstName", "Jane");
        values.put("familyName", "Smith");
        values.put("status", "Active");
        values.put("dateOfBirth", dateOfBirth);
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.retrieve", FakeConnection.success(values));
        return connection;
    }

    private static ServiceResult<Customer> load(FakeConnection connection) {
        return new CustomerService(new MainframeGateway(connection), 1).loadCustomer("C-10234");
    }
}
