// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

// The headline of the Loan Details screen's "Repayment estimate" panel, e.g. "$2,786 / month".
// It only echoes what the MMM typed; the real schedule comes from the mainframe (US7).
// Kept free of JavaFX so it can be unit tested.
public final class RepaymentEstimate {
    public static final String NO_AMOUNT = "Enter a payment amount";

    // payment frequency values (lower case) and the period they read as after "/"
    private static final Map<String, String> PERIODS = new HashMap<>();
    static {
        PERIODS.put("daily", "day");
        PERIODS.put("weekly", "week");
        PERIODS.put("fortnightly", "fortnight");
        PERIODS.put("monthly", "month");
        PERIODS.put("quarterly", "quarter");
        PERIODS.put("yearly", "year");
        PERIODS.put("annually", "year");
    }

    private RepaymentEstimate() {}

    public static String describe(String paymentAmount, String paymentFrequency) {
        BigDecimal amount = parseAmount(paymentAmount);
        if (amount == null) {
            return NO_AMOUNT;
        }
        String text = formatDollars(amount);
        if (paymentFrequency == null || paymentFrequency.isBlank()) {
            return text;
        }
        String frequency = paymentFrequency.trim().toLowerCase();
        String period = PERIODS.get(frequency);
        if (period == null) {
            return text + " " + frequency;
        }
        return text + " / " + period;
    }

    // null when the text isn't a positive number; "$" and "," are allowed
    private static BigDecimal parseAmount(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim().replace("$", "").replace(",", "");
        if (cleaned.isEmpty()) {
            return null;
        }
        try {
            BigDecimal amount = new BigDecimal(cleaned);
            return amount.signum() > 0 ? amount : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // "$2,786" for whole dollars, "$1,393.50" when there are cents
    private static String formatDollars(BigDecimal amount) {
        boolean isWhole = amount.stripTrailingZeros().scale() <= 0;
        DecimalFormat format = new DecimalFormat(isWhole ? "$#,##0" : "$#,##0.00");
        return format.format(amount);
    }
}
