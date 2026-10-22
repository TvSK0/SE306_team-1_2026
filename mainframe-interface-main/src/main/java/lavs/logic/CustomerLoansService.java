package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import java.util.ArrayList;
import java.util.List;

// AI-assisted: lists the customer's loans without exposing mainframe message classes to UI.
public final class CustomerLoansService {
    public record Row(String number, String principal, String startDate, String status) {}
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
}
