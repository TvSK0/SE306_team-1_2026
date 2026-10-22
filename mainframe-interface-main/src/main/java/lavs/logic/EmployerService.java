package lavs.logic;

// AI-assisted.
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.messages.customer.AddEmployer;
import mainframe.client.messages.customer.DeleteEmployer;
import mainframe.client.messages.customer.UpdateEmployer;

import java.io.IOException;

// US5: add, update and delete employer records for a customer.
//
// TEMPORARY: uses Connection directly. Once Scott's mainframe gateway is
// on main, swap Connection for SyncService so writes also work offline.
//
// salary is accepted as a String so the UI can validate the format before
// parsing. Validators.currency() checks the shape; a blank salary is fine
// (the field is optional - defaults to 0 when not provided).
public final class EmployerService {
    private final Connection _connection;

    public EmployerService(Connection connection) {
        _connection = connection;
    }

    // Adds an employer record for a customer using customer.addEmployer.
    // type must be AddEmployer.TYPE_PRIMARY ("Primary") or TYPE_SECONDARY
    // ("Secondary"). salary may be blank; if non-blank it must match the
    // currency format (e.g. "80000" or "80000.00").
    public EmployerResult addEmployer(
            String customerNumber,
            String name,
            String position,
            String salary,
            String type) throws IOException {
        ValidationResult validation = validateEmployer(customerNumber, name, type, salary);
        if (!validation.isValid()) {
            return EmployerResult.invalid(validation);
        }
        AddEmployer message = new AddEmployer(
                customerNumber, name, position, parseSalary(salary), type);
        return EmployerResult.fromStatus(message.send(_connection));
    }

    // Updates an existing employer record using customer.updateEmployer.
    // Same validation rules as addEmployer.
    public EmployerResult updateEmployer(
            long id,
            String customerNumber,
            String name,
            String position,
            String salary,
            String type) throws IOException {
        ValidationResult validation = validateEmployer(customerNumber, name, type, salary);
        if (!validation.isValid()) {
            return EmployerResult.invalid(validation);
        }
        UpdateEmployer message = new UpdateEmployer(
                customerNumber, id, name, position, parseSalary(salary), type);
        return EmployerResult.fromStatus(message.send(_connection));
    }

    // Removes an employer record using customer.deleteEmployer.
    public EmployerResult deleteEmployer(long id, String customerNumber) throws IOException {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("customerNumber", customerNumber));
        if (!validation.isValid()) {
            return EmployerResult.invalid(validation);
        }
        return EmployerResult.fromStatus(new DeleteEmployer(customerNumber, id).send(_connection));
    }

    private ValidationResult validateEmployer(
            String customerNumber, String name, String type, String salary) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("customerNumber", customerNumber));
        validation.addIfPresent(Validators.required("name", name));
        validation.addIfPresent(Validators.required("type", type));
        if (type != null && !type.isBlank()
                && !AddEmployer.TYPE_PRIMARY.equals(type)
                && !AddEmployer.TYPE_SECONDARY.equals(type)) {
            validation.add(new FieldError("type",
                    "must be '" + AddEmployer.TYPE_PRIMARY
                            + "' or '" + AddEmployer.TYPE_SECONDARY + "'"));
        }
        validation.addIfPresent(Validators.currency("salary", salary));
        return validation;
    }

    // salary is optional in the form, default to 0 when blank. 
    // parseDouble is safe here cuz currency() already accepted the format.
    private static double parseSalary(String salary) {
        return (salary == null || salary.isBlank()) ? 0.0 : Double.parseDouble(salary);
    }
}
