// AI-assisted: tests for the Loans section's wording and small rules.
package lavs.ui;

import lavs.logic.CustomerLoansService;
import lavs.logic.LookupResult;
import lavs.model.Customer;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LoansSectionTextTests {
    // LT-R1
    @Test
    void theSubtitleNamesTheCustomer() {
        Assertions.assertEquals("Loans where Jane Smith is the main borrower, co-borrower or guarantor.",
                LoansSectionText.subtitle("Jane Smith"));
    }

    // LT-R1
    @Test
    void theCustomerIsCalledByNameOrByNumberIfTheNameIsNotKnown() {
        // Arrange
        Customer jane = new Customer();
        jane.setFirstName("Jane");
        jane.setFamilyName("Smith");

        // Act and Assert
        Assertions.assertEquals("Jane Smith", LoansSectionText.customerLabel(jane, "C-1"));
        Assertions.assertEquals("C-1", LoansSectionText.customerLabel(new Customer(), "C-1"));
        Assertions.assertEquals("C-1", LoansSectionText.customerLabel(null, "C-1"));
    }

    // LT-R1
    @Test
    void theEmptyTableSaysNoLoansYet() {
        Assertions.assertEquals("No loans yet.", LoansSectionText.EMPTY);
    }

    // LT-R2 (the loan screen's Back link shows the name of the customer it is given)
    @Test
    void theLoanScreenIsGivenTheLoadedCustomerOtherwiseOneWithTheNumber() {
        // Arrange
        Customer loaded = new Customer();
        loaded.setNumber("C-1");
        loaded.setFirstName("Jane");

        // Act and Assert
        Assertions.assertSame(loaded, LoansSectionText.customerFor(loaded, "C-1"));
        Assertions.assertEquals("C-1", LoansSectionText.customerFor(null, "C-1").getNumber());
    }

    // LT-R1
    @Test
    void aNetworkFailureOnTheListShowsTheOfflineBanner() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);
        LookupResult<?> result = new CustomerLoansService(connection).listLoans("C-1");

        // Act and Assert
        Assertions.assertEquals(BannerContent.offline(), LoansSectionText.loadFailure(result).orElseThrow());
        Assertions.assertEquals(BannerContent.offline(), LoansSectionText.openFailure(result).orElseThrow());
    }

    // LT-R1
    @Test
    void aRefusalOnTheListShowsTheMainframesReason() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.list", FakeConnection.rejected("No such customer"));
        LookupResult<?> result = new CustomerLoansService(connection).listLoans("C-1");

        // Act and Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't load the loans",
                "Reason given: No such customer. Try again."), LoansSectionText.loadFailure(result).orElseThrow());
    }

    // LT-R2
    @Test
    void aRefusalOnOpeningALoanShowsTheMainframesReason() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.retrieve", FakeConnection.rejected("No such loan"));
        LookupResult<?> result = new CustomerLoansService(connection).openLoan("L-1");

        // Act and Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't open the loan",
                "Reason given: No such loan. Try again."), LoansSectionText.openFailure(result).orElseThrow());
    }

    // LT-R1
    @Test
    void aRefusalWithNoReasonSaysSoAndNeverAddsASecondFullStop() {
        // Arrange
        FakeConnection noReason = new FakeConnection();
        noReason.whenRequested("loan.list", FakeConnection.rejected(" "));
        FakeConnection stop = new FakeConnection();
        stop.whenRequested("loan.list", FakeConnection.rejected("Too short."));

        // Act and Assert
        Assertions.assertEquals("The mainframe gave no reason. Try again.",
                LoansSectionText.loadFailure(new CustomerLoansService(noReason).listLoans("C-1")).orElseThrow().body());
        Assertions.assertEquals("Reason given: Too short. Try again.",
                LoansSectionText.loadFailure(new CustomerLoansService(stop).listLoans("C-1")).orElseThrow().body());
    }

    // LT-R1 (a blank number never reaches the screen, but the banner still says something)
    @Test
    void anInvalidInputShowsABanner() {
        // Arrange
        LookupResult<?> result = new CustomerLoansService(new FakeConnection()).listLoans(" ");

        // Act and Assert
        Assertions.assertTrue(LoansSectionText.loadFailure(result).isPresent());
    }

    // LT-R1
    @Test
    void aSuccessfulReadHasNoFailureBanner() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("loan.list", FakeConnection.success(java.util.Map.of("total", "0", "items", "0")));
        LookupResult<?> result = new CustomerLoansService(connection).listLoans("C-1");

        // Act and Assert
        Assertions.assertTrue(LoansSectionText.loadFailure(result).isEmpty());
    }

    // LT-R1
    @Test
    void anUnexpectedFailureShowsItsMessageUnderTheGivenTitle() {
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't open the loan",
                "Something went wrong: boom."), LoansSectionText.unexpected("Couldn't open the loan",
                new IllegalStateException("boom")));
        Assertions.assertEquals("Something went wrong.",
                LoansSectionText.unexpected("Couldn't open the loan", new IllegalStateException()).body());
    }

    // LT-R1
    @Test
    void theListOfRolesIsTheThreeInfoRoles() {
        Assertions.assertEquals(List.of("Main borrower", "Co-borrower", "Guarantor"),
                List.of(CustomerLoansService.MAIN_BORROWER, CustomerLoansService.CO_BORROWER,
                        CustomerLoansService.GUARANTOR));
    }
}
