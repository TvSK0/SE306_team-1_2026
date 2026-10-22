package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// AI-assisted: lists the customer's loans without exposing mainframe message classes to UI.
public final class CustomerLoansService {
    public record Row(String number, String principal, String startDate, String status) {}
    // the customer's part in a loan, shown in the Loans tab's Role column (Figma 03a)
    public static final String MAIN_BORROWER = "Main borrower";
    public static final String CO_BORROWER = "Co-borrower";
    public static final String GUARANTOR = "Guarantor";
    private final MainframeGateway gateway;
    public CustomerLoansService(MainframeGateway gateway) { this.gateway = gateway; }

    public ServiceResult<List<Row>> list(String customerNumber) {
        ValidationResult errors = new ValidationResult();
        errors.addIfPresent(Validators.required("customerNumber", customerNumber));
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        List<Row> rows = new ArrayList<>();
        for (long page = 1; ; page++) {
            var message = new mainframe.client.messages.loan.List(customerNumber, page, 50);
            var result = gateway.send(message);
            if (!result.isSuccess()) return ServiceResult.from(result, null);
            for (var item : message.getItems()) rows.add(new Row(item.getLoanNumber(), item.getPrincipal(),
                    item.getStartDate(), item.getStatus()));
            if (message.getItems().length == 0 || rows.size() >= message.getTotal()) {
                return ServiceResult.from(result, List.copyOf(rows));
            }
        }
    }

    // AI-assisted: the customer's role on each loan, for the Role column. loan.list doesn't say,
    // so each loan is retrieved for its main borrower, and only a loan the customer doesn't own
    // costs a second call, to find them among its co-borrowers and guarantors. Loans whose role
    // couldn't be read are left out of the map (the screen shows them blank). Separate from list()
    // so the table shows straight away and the roles follow.
    public Map<String, String> roles(String customerNumber, List<String> loanNumbers) {
        Map<String, String> roles = new HashMap<>();
        for (String loanNumber : loanNumbers) {
            ServiceResult<String> role = role(customerNumber, loanNumber);
            if (role.isSuccessful() && role.getValue() != null) roles.put(loanNumber, role.getValue());
        }
        return roles;
    }

    // Main borrower, Co-borrower, Guarantor, the mainframe's own word for any other type, or null
    // (as a successful result) when the customer isn't on the loan at all
    public ServiceResult<String> role(String customerNumber, String loanNumber) {
        ValidationResult errors = new ValidationResult();
        errors.addIfPresent(Validators.required("customerNumber", customerNumber));
        errors.addIfPresent(Validators.required("loanNumber", loanNumber));
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        var loan = new mainframe.client.messages.loan.Retrieve(loanNumber);
        var result = gateway.send(loan);
        if (!result.isSuccess()) return ServiceResult.from(result, null);
        if (customerNumber.equals(loan.getCustomerNumber())) return ServiceResult.from(result, MAIN_BORROWER);
        // a loan has only a few co-borrowers and guarantors, so one page of 50 is enough
        var borrowers = new mainframe.client.messages.loanCoborrower.List(loanNumber, 1, 50);
        result = gateway.send(borrowers);
        if (!result.isSuccess()) return ServiceResult.from(result, null);
        for (var borrower : borrowers.getItems()) {
            if (customerNumber.equals(borrower.getCoborrowerNumber())) {
                return ServiceResult.from(result, roleName(borrower.getType()));
            }
        }
        return ServiceResult.from(result, null);
    }

    // "Coborrower" is the mainframe's spelling; the screen says Co-borrower
    private static String roleName(String type) {
        if (LoanBorrowerService.COBORROWER.equalsIgnoreCase(type)) return CO_BORROWER;
        if (LoanBorrowerService.GUARANTOR.equalsIgnoreCase(type)) return GUARANTOR;
        return type;
    }
}
