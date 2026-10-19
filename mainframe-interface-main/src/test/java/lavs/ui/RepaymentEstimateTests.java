// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RepaymentEstimateTests {

    @Test
    void showsAWholeAmountPerPaymentPeriod() {
        // Act
        String text = RepaymentEstimate.describe("2786", "Monthly");

        // Assert
        Assertions.assertEquals("$2,786 / month", text);
    }

    @Test
    void keepsCentsWhenThereAreAny() {
        // Act
        String text = RepaymentEstimate.describe("1393.50", "Fortnightly");

        // Assert
        Assertions.assertEquals("$1,393.50 / fortnight", text);
    }

    @Test
    void dropsZeroCents() {
        // Act
        String text = RepaymentEstimate.describe("2786.00", "Weekly");

        // Assert
        Assertions.assertEquals("$2,786 / week", text);
    }

    @Test
    void acceptsADollarSignAndCommas() {
        // Act
        String text = RepaymentEstimate.describe(" $2,786.00 ", "Monthly");

        // Assert
        Assertions.assertEquals("$2,786 / month", text);
    }

    @Test
    void namesPeriodsItDoesNotKnowAsTheyAre() {
        // Act
        String text = RepaymentEstimate.describe("2786", "Every second Tuesday");

        // Assert
        Assertions.assertEquals("$2,786 every second tuesday", text);
    }

    @Test
    void showsTheAmountAloneUntilAFrequencyIsPicked() {
        // Act
        String text = RepaymentEstimate.describe("2786", null);

        // Assert
        Assertions.assertEquals("$2,786", text);
    }

    @Test
    void asksForAnAmountWhenItIsBlankOrNotANumber() {
        // Act / Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals(RepaymentEstimate.NO_AMOUNT, RepaymentEstimate.describe("", "Monthly")),
                () -> Assertions.assertEquals(RepaymentEstimate.NO_AMOUNT, RepaymentEstimate.describe(null, "Monthly")),
                () -> Assertions.assertEquals(RepaymentEstimate.NO_AMOUNT, RepaymentEstimate.describe("abc", "Monthly")),
                () -> Assertions.assertEquals(RepaymentEstimate.NO_AMOUNT, RepaymentEstimate.describe("-5", "Monthly")));
    }
}
