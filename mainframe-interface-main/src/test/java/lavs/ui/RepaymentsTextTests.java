// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import mainframe.client.Status;
import mainframe.client.StatusMessage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RepaymentsTextTests {

    @Test
    void showsMoneyWithCommasAndCents() {
        // Act / Assert
        Assertions.assertAll(
                () -> Assertions.assertEquals("$1,002,953.61", RepaymentsText.money(new BigDecimal("1002953.61"))),
                () -> Assertions.assertEquals("$2,786.00", RepaymentsText.money(new BigDecimal("2786"))),
                () -> Assertions.assertEquals("$0.50", RepaymentsText.money(new BigDecimal("0.5"))));
    }

    @Test
    void roundsMoneyToTheNearestCent() {
        // Act
        String text = RepaymentsText.money(new BigDecimal("427.255"));

        // Assert
        Assertions.assertEquals("$427.26", text);
    }

    @Test
    void showsDatesDayFirst() {
        // Act
        String text = RepaymentsText.date(LocalDate.of(2056, 11, 1));

        // Assert
        Assertions.assertEquals("01/11/2056", text);
    }

    @Test
    void saysWhenTheMainframeCannotBeReached() {
        // Arrange
        Status status = StatusMessage.NETWORK_FAILURE_UNAVAILABLE.generateMessageResponse(1).getStatus();

        // Act
        String text = RepaymentsText.forFailure(status);

        // Assert
        Assertions.assertEquals("Couldn't reach the mainframe. Check your connection and click Refresh.", text);
    }

    @Test
    void passesOnTheMainframesReason() {
        // Arrange
        Status status = new Status(StatusMessage.SERVER_FAILURE.getCode(), "Unable to run query: query error", 1);

        // Act
        String text = RepaymentsText.forFailure(status);

        // Assert
        Assertions.assertEquals(
                "The mainframe couldn't produce the repayments: Unable to run query: query error", text);
    }
}
