// AI-assisted: how a loan row's money, date, role and status are shown in the Loans table (Figma 03a).
package lavs.ui;

import lavs.logic.CustomerLoansService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

// Plain, so every rule is unit tested; CustomerLoansPanel only copies these onto the table.
// The mainframe sends the principal and start date as text (e.g. "450000.0", "2026-01-05"), so
// they are read here; anything unreadable is shown as sent rather than hidden.
public final class LoanRowFormatter {
    static final String CHECKING = "Checking…";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LoanRowFormatter() {
    }

    // $450,000.00; blank when there is none
    public static String money(String amount) {
        if (amount == null || amount.isBlank()) {
            return "";
        }
        try {
            return NumberFormat.getCurrencyInstance(Locale.US).format(new BigDecimal(amount.trim()));
        } catch (NumberFormatException e) {
            return amount;
        }
    }

    // dd/MM/yyyy; blank when there is none
    public static String date(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return "";
        }
        try {
            return LocalDate.parse(isoDate.trim()).format(DATE);
        } catch (DateTimeParseException e) {
            return isoDate;
        }
    }

    // While roles are still being looked up: neutral "Checking…". The three roles are info tags;
    // anything else is neutral as sent, and a customer not found on the loan gets a blank tag.
    public static CustomerSearchText.StatusTag roleTag(String role, boolean rolesKnown) {
        if (!rolesKnown) {
            return new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, CHECKING);
        }
        boolean known = CustomerLoansService.MAIN_BORROWER.equals(role)
                || CustomerLoansService.CO_BORROWER.equals(role)
                || CustomerLoansService.GUARANTOR.equals(role);
        return new CustomerSearchText.StatusTag(known ? TagVariant.INFO : TagVariant.NEUTRAL, role == null ? "" : role);
    }

    // Active is success and New is warning; any other status is neutral, as the mainframe sent it
    public static CustomerSearchText.StatusTag statusTag(String status) {
        if ("Active".equalsIgnoreCase(status)) {
            return new CustomerSearchText.StatusTag(TagVariant.SUCCESS, "Active");
        }
        if ("New".equalsIgnoreCase(status)) {
            return new CustomerSearchText.StatusTag(TagVariant.WARNING, "New");
        }
        return new CustomerSearchText.StatusTag(TagVariant.NEUTRAL, status == null ? "" : status);
    }
}
