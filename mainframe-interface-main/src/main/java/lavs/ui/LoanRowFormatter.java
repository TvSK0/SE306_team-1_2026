// AI-assisted: how a loan row's money, date, role and status are shown in the Loans table (Figma 03a).
package lavs.ui;

import lavs.logic.CustomerLoansService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

// Plain, so every rule is unit tested; the Loans section only copies these onto the table.
public final class LoanRowFormatter {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private LoanRowFormatter() {
    }

    // $450,000.00, or blank when the principal is not known
    public static String money(BigDecimal amount) {
        return amount == null ? "" : NumberFormat.getCurrencyInstance(Locale.US).format(amount);
    }

    // dd/MM/yyyy, or blank when the date is not known
    public static String date(LocalDate date) {
        return date == null ? "" : date.format(DATE);
    }

    // The three roles are info tags. Anything else is shown as the mainframe sent it in the
    // neutral colour, and a customer not found on the loan at all gets a blank neutral tag.
    public static CustomerSearchText.StatusTag roleTag(String role) {
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
