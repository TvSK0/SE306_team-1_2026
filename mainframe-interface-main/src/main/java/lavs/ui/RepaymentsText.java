// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import mainframe.client.Status;
import mainframe.client.StatusMessage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

// How the Repayments tab writes money, dates and load errors (mockup 6).
// Kept free of JavaFX so it can be unit tested.
public final class RepaymentsText {
    private static final DateTimeFormatter DAY_FIRST = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private RepaymentsText() {}

    // "$1,002,953.61": always two decimal places, rounded half up
    public static String money(BigDecimal amount) {
        DecimalFormat format = new DecimalFormat("$#,##0.00");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount);
    }

    // "01/11/2056", the New Zealand order
    public static String date(LocalDate date) {
        return date.format(DAY_FIRST);
    }

    public static String forFailure(Status status) {
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            return "Couldn't reach the mainframe. Check your connection and click Refresh.";
        }
        return "The mainframe couldn't produce the repayments: " + status.getErrorMessage();
    }
}
