// AI-assisted: tests for how a loan row is shown in the Loans table.
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LoanRowFormatterTests {
    // LT-R1
    @Test
    void moneyShowsDollarsWithThousandsAndCents() {
        Assertions.assertEquals("$450,000.00", LoanRowFormatter.money(new BigDecimal("450000")));
        Assertions.assertEquals("$25,000.50", LoanRowFormatter.money(new BigDecimal("25000.5")));
    }

    // LT-R1
    @Test
    void anUnknownPrincipalIsBlank() {
        Assertions.assertEquals("", LoanRowFormatter.money(null));
    }

    // LT-R1
    @Test
    void theStartDateShowsAsDayMonthYear() {
        Assertions.assertEquals("05/01/2026", LoanRowFormatter.date(LocalDate.of(2026, 1, 5)));
    }

    // LT-R1
    @Test
    void anUnknownStartDateIsBlank() {
        Assertions.assertEquals("", LoanRowFormatter.date(null));
    }

    // LT-R1
    @Test
    void theThreeRolesAreInfoTags() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.INFO, "Main borrower"),
                LoanRowFormatter.roleTag("Main borrower"));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.INFO, "Co-borrower"),
                LoanRowFormatter.roleTag("Co-borrower"));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.INFO, "Guarantor"),
                LoanRowFormatter.roleTag("Guarantor"));
    }

    // LT-R1
    @Test
    void anyOtherRoleIsNeutralAsSentAndNoRoleIsBlank() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Witness"),
                LoanRowFormatter.roleTag("Witness"));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, ""),
                LoanRowFormatter.roleTag(null));
    }

    // LT-R1
    @Test
    void activeIsSuccessAndNewIsWarning() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.SUCCESS, "Active"),
                LoanRowFormatter.statusTag("ACTIVE"));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.WARNING, "New"),
                LoanRowFormatter.statusTag("new"));
    }

    // LT-R1
    @Test
    void anyOtherStatusIsNeutralAsTheMainframeSentIt() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Pending"),
                LoanRowFormatter.statusTag("Pending"));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, ""),
                LoanRowFormatter.statusTag(null));
    }
}
