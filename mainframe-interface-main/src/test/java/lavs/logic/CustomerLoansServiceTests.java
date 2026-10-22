// AI-assisted: tests for CustomerLoansService, one per Loans tab requirement.
package lavs.logic;

import lavs.model.Loan;
import lavs.validation.FieldError;
import mainframe.client.FakeConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustomerLoansServiceTests {
    private static final String LOAN_LIST = "loan.list";
    private static final String LOAN_RETRIEVE = "loan.retrieve";
    private static final String COBORROWERS = "loanCoborrower.list";

    // LT-R1, LT-R5
    @Test
    void listsTheLoansWithTheMainBorrowersRoleWhenTheCustomerOwnsTheLoan() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, Map.of("number", "C-1"), FakeConnection.success(loans(
                loan("L-1", "450000.0", "2026-01-05", "Active"), loan("L-2", "25000.5", "2026-03-01", "New"))));
        connection.whenRequested(LOAN_RETRIEVE, FakeConnection.success(retrieved("C-1")));
        CustomerLoansService service = new CustomerLoansService(connection);

        // Act
        LookupResult<List<LoanRow>> result = service.listLoans(" C-1 ");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        List<LoanRow> rows = result.getValue();
        Assertions.assertEquals(2, rows.size());
        Assertions.assertEquals(new LoanRow("L-1", "Main borrower", "Active", new BigDecimal("450000.0"),
                LocalDate.of(2026, 1, 5)), rows.get(0));
        Assertions.assertEquals("New", rows.get(1).status());
        Assertions.assertEquals(new BigDecimal("25000.5"), rows.get(1).principal());
    }

    // LT-R5 (the role is looked up for loans the customer does not own, so the list is right
    // whichever loans loan.list returns)
    @Test
    void findsTheCustomerAmongTheCoBorrowersAndGuarantorsOfOtherPeoplesLoans() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.success(loans(
                loan("L-1", "1.0", "2026-01-05", "Active"), loan("L-2", "2.0", "2026-01-06", "Active"),
                loan("L-3", "3.0", "2026-01-07", "Active"), loan("L-4", "4.0", "2026-01-08", "Active"))));
        for (String number : List.of("L-1", "L-2", "L-3", "L-4")) {
            connection.whenRequested(LOAN_RETRIEVE, Map.of("number", number), FakeConnection.success(retrieved("C-9")));
        }
        connection.whenRequested(COBORROWERS, Map.of("number", "L-1"),
                FakeConnection.success(borrowers(borrower("C-1", "Coborrower"))));
        connection.whenRequested(COBORROWERS, Map.of("number", "L-2"),
                FakeConnection.success(borrowers(borrower("C-5", "Guarantor"), borrower("C-1", "Guarantor"))));
        connection.whenRequested(COBORROWERS, Map.of("number", "L-3"),
                FakeConnection.success(borrowers(borrower("C-1", "Other"))));
        connection.whenRequested(COBORROWERS, Map.of("number", "L-4"),
                FakeConnection.success(borrowers(borrower("C-7", "Coborrower"))));

        // Act
        List<LoanRow> rows = new CustomerLoansService(connection).listLoans("C-1").getValue();

        // Assert
        Assertions.assertEquals("Co-borrower", rows.get(0).role());
        Assertions.assertEquals("Guarantor", rows.get(1).role());
        Assertions.assertEquals("Other", rows.get(2).role());
        Assertions.assertNull(rows.get(3).role());
    }

    // LT-R1
    @Test
    void anUnreadablePrincipalOrDateIsLeftOutRatherThanFailingTheList() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.success(loans(
                loan("L-1", "abc", "03/02/2001", "Active"), loan("L-2", null, null, "Active"),
                loan("L-3", " ", " ", "Active"))));
        connection.whenRequested(LOAN_RETRIEVE, FakeConnection.success(retrieved("C-1")));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        result.getValue().forEach(row -> {
            Assertions.assertNull(row.principal());
            Assertions.assertNull(row.startDate());
        });
    }

    // LT-R1
    @Test
    void aCustomerWithNoLoansHasAnEmptyList() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.success(loans()));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertTrue(result.getValue().isEmpty());
        Assertions.assertEquals(1, connection.getSentRequests().size());
    }

    // LT-R1 (an empty page ends the list, so a wrong total cannot cause an endless loop)
    @Test
    void anEmptyPageEndsTheListEvenIfTheTotalSaysThereAreMore() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        Map<String, String> first = loans(loan("L-1", "1.0", "2026-01-05", "Active"));
        first.put("total", "5");
        connection.whenRequested(LOAN_LIST, Map.of("page", "1"), FakeConnection.success(first));
        Map<String, String> second = loans();
        second.put("total", "5");
        connection.whenRequested(LOAN_LIST, Map.of("page", "2"), FakeConnection.success(second));
        connection.whenRequested(LOAN_RETRIEVE, FakeConnection.success(retrieved("C-1")));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertEquals(1, result.getValue().size());
    }

    // LT-R1
    @Test
    void aBlankCustomerNumberSendsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans(" ");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertEquals(List.of(CustomerLoansService.CUSTOMER_NUMBER),
                result.getValidationErrors().getErrors().stream().map(FieldError::getFieldName).toList());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // LT-R1
    @Test
    void anUnreachableMainframeIsANetworkFailure() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.setIsConnected(false);

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.NETWORK_FAILURE, result.getOutcome());
        Assertions.assertEquals(FakeConnection.networkDown().getStatus().getErrorMessage(), result.getMessage());
        Assertions.assertNull(result.getValue());
    }

    // LT-R1
    @Test
    void aRefusedListIsRejectedWithTheMainframesReason() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.rejected("No such customer"));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("No such customer", result.getMessage());
    }

    // LT-R1
    @Test
    void aRefusedLoanLookupFailsTheWholeList() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.success(loans(loan("L-1", "1.0", "2026-01-05", "Active"))));
        connection.whenRequested(LOAN_RETRIEVE, FakeConnection.rejected("No such loan"));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("No such loan", result.getMessage());
    }

    // LT-R1
    @Test
    void aRefusedBorrowerListFailsTheWholeList() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.success(loans(loan("L-1", "1.0", "2026-01-05", "Active"))));
        connection.whenRequested(LOAN_RETRIEVE, FakeConnection.success(retrieved("C-9")));
        connection.whenRequested(COBORROWERS, FakeConnection.rejected("No such loan"));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
    }

    // LT-R1 (the starter message throws when an item has no id; that must not escape)
    @Test
    void aBorrowerReplyTheStarterMessageCannotReadIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.success(loans(loan("L-1", "1.0", "2026-01-05", "Active"))));
        connection.whenRequested(LOAN_RETRIEVE, FakeConnection.success(retrieved("C-9")));
        connection.whenRequested(COBORROWERS, FakeConnection.success(Map.of("total", "1", "items", "1")));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("The mainframe sent a reply that could not be read.", result.getMessage());
    }

    // LT-R1
    @Test
    void aLoanListReplyTheStarterMessageCannotReadIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_LIST, FakeConnection.success(Map.of("total", "1", "items", "x")));

        // Act
        LookupResult<List<LoanRow>> result = new CustomerLoansService(connection).listLoans("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
    }

    // LT-R2
    @Test
    void opensTheSavedLoan() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        Map<String, String> loan = new HashMap<>(retrieved("C-1"));
        loan.put("loanNumber", "L-1");
        loan.put("status", "Active");
        loan.put("principal", "450000.0");
        loan.put("rateType", "Fixed");
        loan.put("rateAmount", "6.29");
        loan.put("rateFrequency", "Annual");
        loan.put("paymentAmount", "2500.0");
        loan.put("paymentFrequency", "Monthly");
        loan.put("startDate", "2026-01-05");
        loan.put("renewalDate", "2031-01-05");
        connection.whenRequested(LOAN_RETRIEVE, Map.of("number", "L-1"), FakeConnection.success(loan));

        // Act
        LookupResult<Loan> result = new CustomerLoansService(connection).openLoan("L-1");

        // Assert
        Assertions.assertTrue(result.isSuccessful());
        Assertions.assertEquals("L-1", result.getValue().getLoanNumber());
        Assertions.assertEquals("C-1", result.getValue().getCustomerNumber());
        Assertions.assertEquals(new BigDecimal("450000.0"), result.getValue().getPrincipal());
    }

    // LT-R2
    @Test
    void aBlankLoanNumberSendsNothing() {
        // Arrange
        FakeConnection connection = new FakeConnection();

        // Act
        LookupResult<Loan> result = new CustomerLoansService(connection).openLoan(" ");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID, result.getOutcome());
        Assertions.assertTrue(connection.getSentRequests().isEmpty());
    }

    // LT-R2
    @Test
    void aRefusedLoanIsRejectedAndAnUnreachableMainframeIsANetworkFailure() {
        // Arrange
        FakeConnection refused = new FakeConnection();
        refused.whenRequested(LOAN_RETRIEVE, FakeConnection.rejected("No such loan"));
        FakeConnection down = new FakeConnection();
        down.setIsConnected(false);

        // Act and Assert
        LookupResult<Loan> rejected = new CustomerLoansService(refused).openLoan("L-1");
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, rejected.getOutcome());
        Assertions.assertEquals("No such loan", rejected.getMessage());
        Assertions.assertEquals(LookupResult.Outcome.NETWORK_FAILURE,
                new CustomerLoansService(down).openLoan("L-1").getOutcome());
    }

    // LT-R2 (a reply the loan reader cannot make a loan from must not escape)
    @Test
    void aLoanReplyThatCannotBeReadIsRejected() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested(LOAN_RETRIEVE, FakeConnection.success(Map.of("customerNumber", "C-1")));

        // Act
        LookupResult<Loan> result = new CustomerLoansService(connection).openLoan("L-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
    }

    // LT-R3
    @Test
    void readsWhetherTheCustomerHasAMailingAddressAPhoneAndAnEmail() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerAddress.list", FakeConnection.success(addresses("n", "y")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(total(1)));
        connection.whenRequested("customerEmail.list", FakeConnection.success(total(0)));

        // Act
        LookupResult<StartLoanFacts> result = new CustomerLoansService(connection).startLoanFacts(" C-1 ");

        // Assert
        Assertions.assertEquals(new StartLoanFacts(true, true, false), result.getValue());
        Assertions.assertEquals("C-1", connection.getSentRequests().get(0).getValue("number"));
    }

    // LT-R3
    @Test
    void aCustomerWithOnlyOtherAddressesHasNoMailingAddress() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerAddress.list", FakeConnection.success(addresses("n", "")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(total(0)));
        connection.whenRequested("customerEmail.list", FakeConnection.success(total(2)));

        // Act
        LookupResult<StartLoanFacts> result = new CustomerLoansService(connection).startLoanFacts("C-1");

        // Assert
        Assertions.assertEquals(new StartLoanFacts(false, false, true), result.getValue());
    }

    // LT-R3 (a mailing address on a later page is still found)
    @Test
    void looksForTheMailingAddressOnLaterPages() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        Map<String, String> first = addresses("n");
        first.put("total", "2");
        connection.whenRequested("customerAddress.list", Map.of("page", "1"), FakeConnection.success(first));
        Map<String, String> second = addresses("y");
        second.put("total", "2");
        connection.whenRequested("customerAddress.list", Map.of("page", "2"), FakeConnection.success(second));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(total(0)));
        connection.whenRequested("customerEmail.list", FakeConnection.success(total(0)));

        // Act
        LookupResult<StartLoanFacts> result = new CustomerLoansService(connection).startLoanFacts("C-1");

        // Assert
        Assertions.assertTrue(result.getValue().hasMailingAddress());
    }

    // LT-R3 (an empty page ends the search for a mailing address)
    @Test
    void anEmptyAddressPageEndsTheSearch() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        Map<String, String> first = addresses("n");
        first.put("total", "9");
        connection.whenRequested("customerAddress.list", Map.of("page", "1"), FakeConnection.success(first));
        Map<String, String> second = addresses();
        second.put("total", "9");
        connection.whenRequested("customerAddress.list", Map.of("page", "2"), FakeConnection.success(second));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.success(total(0)));
        connection.whenRequested("customerEmail.list", FakeConnection.success(total(0)));

        // Act
        LookupResult<StartLoanFacts> result = new CustomerLoansService(connection).startLoanFacts("C-1");

        // Assert
        Assertions.assertFalse(result.getValue().hasMailingAddress());
    }

    // LT-R3
    @Test
    void theFactsFailWhenAnyListFails() {
        // Arrange
        FakeConnection connection = new FakeConnection();
        connection.whenRequested("customerAddress.list", FakeConnection.success(addresses("y")));
        connection.whenRequested("customerPhoneNumber.list", FakeConnection.rejected("No such customer"));

        // Act
        LookupResult<StartLoanFacts> result = new CustomerLoansService(connection).startLoanFacts("C-1");

        // Assert
        Assertions.assertEquals(LookupResult.Outcome.REJECTED, result.getOutcome());
        Assertions.assertEquals("No such customer", result.getMessage());
    }

    // LT-R3
    @Test
    void theFactsNeedANumberAndAConnection() {
        // Arrange
        FakeConnection offline = new FakeConnection();
        offline.setIsConnected(false);

        // Act and Assert
        Assertions.assertEquals(LookupResult.Outcome.INVALID,
                new CustomerLoansService(new FakeConnection()).startLoanFacts(null).getOutcome());
        Assertions.assertEquals(LookupResult.Outcome.NETWORK_FAILURE,
                new CustomerLoansService(offline).startLoanFacts("C-1").getOutcome());
    }

    private static Map<String, String> loans(Map<String, String>... items) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Integer.toString(items.length));
        values.put("items", Integer.toString(items.length));
        for (int i = 0; i < items.length; i++) {
            String prefix = (i + 1) + ":";
            for (Map.Entry<String, String> entry : items[i].entrySet()) {
                values.put(prefix + entry.getKey(), entry.getValue());
            }
        }
        return values;
    }

    private static Map<String, String> loan(String number, String principal, String startDate, String status) {
        Map<String, String> item = new HashMap<>();
        item.put("loanNumber", number);
        item.put("status", status);
        if (principal != null) {
            item.put("principal", principal);
        }
        if (startDate != null) {
            item.put("startDate", startDate);
        }
        return item;
    }

    // a loan.retrieve reply; the starter message throws if the three amounts are missing, as a
    // real loan always has them
    private static Map<String, String> retrieved(String customerNumber) {
        Map<String, String> values = new HashMap<>();
        values.put("customerNumber", customerNumber);
        values.put("principal", "1.0");
        values.put("rateAmount", "1.0");
        values.put("paymentAmount", "1.0");
        return values;
    }

    private static Map<String, String> borrowers(Map<String, String>... items) {
        return loans(items);
    }

    private static Map<String, String> borrower(String customerNumber, String type) {
        Map<String, String> item = new HashMap<>();
        item.put("id", "1");
        item.put("type", type);
        item.put("coborrowerNumber", customerNumber);
        return item;
    }

    // customerAddress.list items: each flag is the isMailing value of one address
    private static Map<String, String> addresses(String... isMailing) {
        Map<String, String> values = new HashMap<>();
        values.put("total", Integer.toString(isMailing.length));
        values.put("items", Integer.toString(isMailing.length));
        for (int i = 0; i < isMailing.length; i++) {
            values.put((i + 1) + ":id", Integer.toString(i + 1));
            values.put((i + 1) + ":isMailing", isMailing[i]);
        }
        return values;
    }

    private static Map<String, String> total(long total) {
        return Map.of("total", Long.toString(total), "items", "0");
    }
}
