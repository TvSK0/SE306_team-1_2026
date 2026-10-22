package lavs.logic;

import lavs.data.MainframeGateway;
import lavs.model.Customer;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Arrays;

// AI-assisted: paged search shared by the main screen and borrower selection.
public final class CustomerSearchService {
    public static final int PAGE_SIZE = 25;
    public record Page(List<Customer> customers, long number, long total) {
        public Page { customers = List.copyOf(customers); }
        public boolean hasNext() { return !customers.isEmpty() && number * PAGE_SIZE < total; }
    }
    private final MainframeGateway gateway;

    public CustomerSearchService(MainframeGateway gateway) { this.gateway = gateway; }

    public ServiceResult<Page> search(String query, long page) {
        ValidationResult errors = new ValidationResult();
        errors.addIfPresent(Validators.required("search", query));
        if (page < 1) errors.add(new lavs.validation.FieldError("page", "must be at least 1"));
        if (!errors.isValid()) return ServiceResult.invalid(errors);
        var message = new mainframe.client.messages.customer.Search(page, query.trim(), PAGE_SIZE);
        var result = gateway.send(message);
        if (!result.isSuccess()) return ServiceResult.from(result, null);
        List<Customer> customers = Arrays.stream(message.getItems()).map(item -> {
            Customer customer = new Customer();
            customer.setNumber(item.getNumber());
            customer.setFirstName(item.getFirstName());
            customer.setFamilyName(item.getFamilyName());
            customer.setTitle(item.getTitle());
            customer.setStatus(item.getStatus());
            try { customer.setDateOfBirth(LocalDate.parse(item.getDateOfBirth())); }
            catch (DateTimeParseException | NullPointerException ignored) { /* Unknown date remains blank. */ }
            return customer;
        }).toList();
        return ServiceResult.from(result, new Page(customers, page, message.getTotal()));
    }
}
