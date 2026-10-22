// AI-assisted: tests for what the customer record header shows, one per customer record requirement.
package lavs.ui;

import lavs.logic.CustomerRecordResult;
import lavs.logic.CustomerRecordService;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertEquals(Optional.of("C-10234"), header.numberToLoad());
        Assertions.assertEquals("Loading…", header.name());
        Assertions.assertTrue(header.statusTag().isEmpty());
        Assertions.assertEquals("Customer no. C-10234", header.detailsLine());
        Assertions.assertEquals("Customer", header.topBarTitle());
        Assertions.assertTrue(header.numberToCount().isEmpty());
        Assertions.assertNull(header.customer());
    }

    // CR-R3
    @Test
    void aLoadedCustomerShowsNameStatusNumberAndDateOfBirth() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Act
        Optional<BannerContent> banner = header.applyLoad(loaded("1988-03-14", "Active"));

        // Assert
        Assertions.assertTrue(banner.isEmpty());
        Assertions.assertEquals("Jane Smith", header.name());
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.SUCCESS, "Active"),
                header.statusTag().orElseThrow());
        Assertions.assertEquals("Customer no. C-10234 · DOB 14/03/1988", header.detailsLine());
        Assertions.assertEquals("Customer: Jane Smith (C-10234)", header.topBarTitle());
        Assertions.assertEquals(Optional.of("C-10234"), header.numberToCount());
        Assertions.assertEquals("C-10234", header.customer().getNumber());
    }

    // CR-R3
    @Test
    void anInactiveCustomerHasANeutralTag() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Act
        header.applyLoad(loaded("1988-03-14", "Inactive"));

        // Assert
        Assertions.assertEquals(TagVariant.NEUTRAL, header.statusTag().orElseThrow().variant());
    }

    // CR-R3
    @Test
    void anUnknownDateOfBirthLeavesOutThatPartOfTheLine() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Act
        header.applyLoad(loaded(null, "Active"));

        // Assert
        Assertions.assertEquals("Customer no. C-10234", header.detailsLine());
    }

    // CR-R3
    @Test
    void aNameTheMainframeDidNotSendFallsBackToTheNumber() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Act
        header.applyLoad(result(Map.of("number", "C-10234", "status", "Active"), true));

        // Assert
        Assertions.assertEquals("Customer C-10234", header.name());
    }

    // CR-R4
    @Test
    void aNewCustomerShowsNotSavedYet() {
        // Act
        CustomerHeaderState header = new CustomerHeaderState(null);

        // Assert
        Assertions.assertFalse(header.isSaved());
        Assertions.assertTrue(header.numberToLoad().isEmpty());
        Assertions.assertTrue(header.numberToCount().isEmpty());
        Assertions.assertEquals("New customer", header.name());
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Not saved yet"),
                header.statusTag().orElseThrow());
        Assertions.assertEquals("Customer number is assigned by the mainframe", header.detailsLine());
        Assertions.assertEquals("New customer", header.topBarTitle());
    }

    // CR-R3
    @Test
    void aNetworkFailureShowsTheOfflineBannerAndKeepsTheNumberAsTheName() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-10234");

        // Act
        Optional<BannerContent> banner = header.applyLoad(failed(true));

        // Assert
        Assertions.assertEquals(BannerContent.offline(), banner.orElseThrow());
        Assertions.assertEquals("Customer C-10234", header.name());
        Assertions.assertTrue(header.statusTag().isEmpty());
        Assertions.assertTrue(header.numberToCount().isEmpty());
    }

    // CR-R3
    @Test
    void aRefusalShowsTheMainframesReason() {
        // Arrange
        CustomerHeaderState header = new CustomerHeaderState("C-9");

        // Act
        BannerContent banner = header.applyLoad(failed(false)).orElseThrow();

        // Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't load the customer",
                "Reason given: No such customer. Go back and open the record again."), banner);
    }

    // CR-R3
    @Test
    void aRefusalWithNoReasonSaysSo() {
        Assertions.assertEquals("The mainframe gave no reason. Go back and open the record again.",
                CustomerHeaderState.rejected(" ").body());
    }

    // CR-R3
    @Test
    void doesNotAddASecondFullStopToTheReason() {
        Assertions.assertEquals("Reason given: Too short. Go back and open the record again.",
                CustomerHeaderState.rejected("Too short.").body());
    }

    // the CustomerRecordResult factories are not public, so each result comes from the real service
    private static CustomerRecordResult loaded(String dateOfBirth, String status) {
        Map<String, String> values = new HashMap<>();
        values.put("number", "C-10234");
        values.put("firstName", "Jane");
        values.put("familyName", "Smith");
        values.put("status", status);
        if (dateOfBirth != null) {
            values.put("dateOfBirth", dateOfBirth);
        }
        return result(values, true);
    }

    private static CustomerRecordResult result(Map<String, String> values, boolean success) {
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customer.retrieve", FakeConnection.success(values));
        return new CustomerRecordService(connection).load("C-10234");
    }

    private static CustomerRecordResult failed(boolean networkDown) {
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(!networkDown);
        connection.whenRequested("customer.retrieve", FakeConnection.rejected("No such customer"));
        return new CustomerRecordService(connection).load("C-10234");
    }
}
