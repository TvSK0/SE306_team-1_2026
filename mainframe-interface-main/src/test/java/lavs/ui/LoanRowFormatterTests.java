// AI-assisted: tests for how a loan row is shown in the Loans table (Figma 03a).
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LoanRowFormatterTests {
    // LT-R1
    @Test
    void moneyShowsDollarsWithThousandsAndCents() {
        Assertions.assertEquals("$450,000.00", LoanRowFormatter.money("450000.0"));
        Assertions.assertEquals("$25,000.50", LoanRowFormatter.money(" 25000.5 "));
    }

    // LT-R1
    @Test
    void anUnreadableAmountIsShownAsSentAndNoneIsBlank() {
        Assertions.assertEquals("about 5k", LoanRowFormatter.money("about 5k"));
        Assertions.assertEquals("", LoanRowFormatter.money(null));
        Assertions.assertEquals("", LoanRowFormatter.money(" "));
    }

    // LT-R1
    @Test
    void theStartDateShowsAsDayMonthYear() {
        Assertions.assertEquals("05/01/2026", LoanRowFormatter.date("2026-01-05"));
    }

    // LT-R1
    @Test
    void anUnreadableDateIsShownAsSentAndNoneIsBlank() {
        Assertions.assertEquals("next week", LoanRowFormatter.date("next week"));
        Assertions.assertEquals("", LoanRowFormatter.date(null));
        Assertions.assertEquals("", LoanRowFormatter.date(""));
    }

    // LT-R5 (the roles follow the table)
    @Test
    void theRoleSaysCheckingUntilTheRolesAreKnown() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Checking…"),
                LoanRowFormatter.roleTag("Main borrower", false));
    }

    // LT-R1
    @Test
    void theThreeRolesAreInfoTags() {
        Assertions.assertEquals(TagVariant.INFO, LoanRowFormatter.roleTag("Main borrower", true).variant());
        Assertions.assertEquals(TagVariant.INFO, LoanRowFormatter.roleTag("Co-borrower", true).variant());
        Assertions.assertEquals(TagVariant.INFO, LoanRowFormatter.roleTag("Guarantor", true).variant());
    }

    // LT-R1
    @Test
    void anyOtherRoleIsNeutralAsSentAndNoRoleIsBlank() {
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Witness"),
                LoanRowFormatter.roleTag("Witness", true));
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, ""),
                LoanRowFormatter.roleTag(null, true));
    }

    // LT-R1
    @Test
    void activeIsSuccessNewIsWarningAndTheRestAreNeutral() {
        Assertions.assertEquals(TagVariant.SUCCESS, LoanRowFormatter.statusTag("active").variant());
        Assertions.assertEquals(TagVariant.WARNING, LoanRowFormatter.statusTag("NEW").variant());
        Assertions.assertEquals(new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, "Pending"),
                LoanRowFormatter.statusTag("Pending"));
        Assertions.assertEquals("", LoanRowFormatter.statusTag(null).text());
    }
}
