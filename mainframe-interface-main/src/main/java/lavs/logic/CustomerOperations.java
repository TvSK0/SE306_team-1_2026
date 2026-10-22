package lavs.logic;

// AI-assisted: service contract consumed by the application.
public interface CustomerOperations {
    ServiceResult<lavs.model.Customer> loadCustomer(String number);
    ServiceResult<lavs.model.Customer> createCustomer(CustomerInput input);
    ServiceResult<lavs.model.Customer> updateCustomer(String number, CustomerInput input);
    lavs.validation.ValidationResult validate(CustomerInput input);
}
