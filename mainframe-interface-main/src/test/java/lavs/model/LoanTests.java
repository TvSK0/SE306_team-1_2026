package lavs.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// AI-assisted: tests generated with Claude (team-approved).
class LoanTests {

    @Test
    void constructorSetsFieldsAndLeavesMainframeFieldsEmpty() {
        Loan loan = new Loan("C1", new BigDecimal("500000"), "Fixed", new BigDecimal("6.5"), "Yearly",
                new BigDecimal("750.25"), "Weekly", LocalDate.of(2025, 1, 2), LocalDate.of(2030, 1, 2));

        assertAll(
                () -> assertEquals("C1", loan.getCustomerNumber()),
                () -> assertEquals(new BigDecimal("500000"), loan.getPrincipal()),
                () -> assertEquals("Fixed", loan.getRateType()),
                () -> assertEquals(new BigDecimal("6.5"), loan.getRateAmount()),
                () -> assertEquals("Yearly", loan.getRateFrequency()),
                () -> assertEquals(new BigDecimal("750.25"), loan.getPaymentAmount()),
                () -> assertEquals("Weekly", loan.getPaymentFrequency()),
                () -> assertEquals(LocalDate.of(2025, 1, 2), loan.getStartDate()),
                () -> assertEquals(LocalDate.of(2030, 1, 2), loan.getRenewalDate()),
                () -> assertNull(loan.getLoanNumber()),
                () -> assertNull(loan.getStatus()));
    }

    @Test
    void settersUpdateFields() {
        Loan loan = new Loan();
        loan.setLoanNumber("L1");
        loan.setCustomerNumber("C2");
        loan.setStatus("Pending");
        loan.setPrincipal(new BigDecimal("1000"));
        loan.setRateType("Floating");
        loan.setRateAmount(new BigDecimal("7"));
        loan.setRateFrequency("Monthly");
        loan.setPaymentAmount(new BigDecimal("100"));
        loan.setPaymentFrequency("Fortnightly");
        loan.setStartDate(LocalDate.of(2026, 3, 1));
        loan.setRenewalDate(LocalDate.of(2027, 3, 1));

        assertAll(
                () -> assertEquals("L1", loan.getLoanNumber()),
                () -> assertEquals("C2", loan.getCustomerNumber()),
                () -> assertEquals("Pending", loan.getStatus()),
                () -> assertEquals(new BigDecimal("1000"), loan.getPrincipal()),
                () -> assertEquals("Floating", loan.getRateType()),
                () -> assertEquals(new BigDecimal("7"), loan.getRateAmount()),
                () -> assertEquals("Monthly", loan.getRateFrequency()),
                () -> assertEquals(new BigDecimal("100"), loan.getPaymentAmount()),
                () -> assertEquals("Fortnightly", loan.getPaymentFrequency()),
                () -> assertEquals(LocalDate.of(2026, 3, 1), loan.getStartDate()),
                () -> assertEquals(LocalDate.of(2027, 3, 1), loan.getRenewalDate()));
    }

    @Test
    void isNewUntilLoanNumberAssigned() {
        Loan loan = new Loan();
        assertTrue(loan.isNew());

        loan.setLoanNumber("L1");
        assertFalse(loan.isNew());
    }
}
