// AI-assisted: tests for "+ New loan", including US6 acceptance criterion 1 word for word.
package lavs.ui;

import lavs.logic.CustomerLoansService;
import lavs.logic.LoanService;
import lavs.logic.LookupResult;
import lavs.logic.StartLoanFacts;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class NewLoanCheckTests {
    private static final LoanService LOANS = new LoanService(new FakeConnection());

    // LT-R4
    @Test
    void aCustomerWithAMailingAddressAndAPhoneMayStartALoan() {
        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith", new StartLoanFacts(true, true, false));

        // Assert
        Assertions.assertTrue(decision.canStart());
        Assertions.assertTrue(decision.banner().isEmpty());
        Assertions.assertTrue(decision.linkSection().isEmpty());
    }

    // LT-R4
    @Test
    void aMailingAddressAndAnEmailAloneIsEnough() {
        Assertions.assertTrue(NewLoanCheck.decide(LOANS, "Jane Smith", new StartLoanFacts(true, false, true)).canStart());
    }

    // LT-R3: GIVEN the customer has no mailing address, WHEN the MMM tries to start a loan,
    // THEN the application stops them and says what is missing.
    @Test
    void aCustomerWithNoMailingAddressIsStoppedAndToldSo() {
        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith", new StartLoanFacts(false, true, true));

        // Assert
        Assertions.assertFalse(decision.canStart());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Can't start a new loan",
                "Jane Smith has no mailing address. Add one, or mark an existing address as mailing, then try again."),
                decision.banner().orElseThrow());
        Assertions.assertEquals(Section.ADDRESSES, decision.linkSection().orElseThrow());
        Assertions.assertEquals("Go to Addresses →", decision.linkText());
    }

    // LT-R3: GIVEN the customer has no phone number or email, WHEN the MMM tries to start a
    // loan, THEN the application stops them and says what is missing.
    @Test
    void aCustomerWithNoPhoneOrEmailIsStoppedAndToldSo() {
        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith", new StartLoanFacts(true, false, false));

        // Assert
        Assertions.assertFalse(decision.canStart());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Can't start a new loan",
                "Jane Smith has no phone number or email. Add one, then try again."), decision.banner().orElseThrow());
        Assertions.assertEquals(Section.PHONE_NUMBERS, decision.linkSection().orElseThrow());
        Assertions.assertEquals("Go to Phone numbers →", decision.linkText());
    }

    // LT-R3 (both missing: the mailing address first, and the link goes to Addresses)
    @Test
    void whenBothAreMissingTheMailingAddressIsListedFirst() {
        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith", new StartLoanFacts(false, false, false));

        // Assert
        Assertions.assertEquals("Jane Smith has no mailing address. Add one, or mark an existing address as mailing. "
                        + "Jane Smith has no phone number or email. Add one, then try again.",
                decision.banner().orElseThrow().body());
        Assertions.assertEquals(Section.ADDRESSES, decision.linkSection().orElseThrow());
    }

    // LT-R3
    @Test
    void aBlankNameReadsAsThisCustomer() {
        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, " ", new StartLoanFacts(true, false, false));

        // Assert
        Assertions.assertTrue(decision.banner().orElseThrow().body().startsWith("This customer has no phone"));
    }

    // LT-R3 (the facts as the service reads them)
    @Test
    void decidesFromTheFactsTheServiceRead() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerAddress.list", FakeConnection.success(Map.of("total", "0", "items", "0")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(Map.of("total", "0", "items", "0")));
        connection.whenRequested("customerEmail.list", FakeConnection.success(Map.of("total", "1", "items", "0")));
        LookupResult<StartLoanFacts> facts = new CustomerLoansService(connection).startLoanFacts("C-1");

        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith", facts);

        // Assert
        Assertions.assertFalse(decision.canStart());
        Assertions.assertEquals(Section.ADDRESSES, decision.linkSection().orElseThrow());
    }

    // LT-R4
    @Test
    void goesAheadWhenTheServiceReadEverythingNeeded() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerAddress.list", FakeConnection.success(
                Map.of("total", "1", "items", "1", "1:id", "1", "1:isMailing", "y")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(Map.of("total", "1", "items", "0")));
        connection.whenRequested("customerEmail.list", FakeConnection.success(Map.of("total", "0", "items", "0")));

        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith",
                new CustomerLoansService(connection).startLoanFacts("C-1"));

        // Assert
        Assertions.assertTrue(decision.canStart());
    }

    // LT-R3
    @Test
    void aNetworkFailureBlocksTheLoanWithTheOfflineBanner() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith",
                new CustomerLoansService(connection).startLoanFacts("C-1"));

        // Assert
        Assertions.assertFalse(decision.canStart());
        Assertions.assertEquals(BannerContent.offline(), decision.banner().orElseThrow());
        Assertions.assertTrue(decision.linkSection().isEmpty());
    }

    // LT-R3
    @Test
    void aRefusalBlocksTheLoanWithTheMainframesReason() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerAddress.list", FakeConnection.rejected("No such customer"));

        // Act
        NewLoanCheck.Decision decision = NewLoanCheck.decide(LOANS, "Jane Smith",
                new CustomerLoansService(connection).startLoanFacts("C-1"));

        // Assert
        Assertions.assertFalse(decision.canStart());
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't start a new loan",
                "Reason given: No such customer. Try again."), decision.banner().orElseThrow());
    }

    // LT-R3
    @Test
    void aRefusalWithNoReasonSaysSoAndNeverAddsASecondFullStop() {
        Assertions.assertEquals("The mainframe gave no reason. Try again.", NewLoanCheck.couldntStart(" ").body());
        Assertions.assertEquals("Reason given: Too short. Try again.", NewLoanCheck.couldntStart("Too short.").body());
    }
}
