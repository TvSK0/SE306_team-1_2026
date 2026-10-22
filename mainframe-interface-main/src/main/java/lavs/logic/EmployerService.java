package lavs.logic;

// AI-assisted: US5 employer writes now go through SyncService (US11) instead of the Connection
// directly, so add/update/delete are queued locally first and uploaded when the mainframe is
// reachable — they work offline. The fields and their string forms match AddEmployer/
// UpdateEmployer/DeleteEmployer.send exactly, so MainframeGateway replays each change unchanged.
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.messages.customer.AddEmployer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

// US5: add, update and delete employer records for a customer.
//
// salary is accepted as a String so the UI can validate the format before
// parsing. Validators.currency() checks the shape; a blank salary is fine
// (the field is optional - defaults to 0 when not provided).
public final class EmployerService {
    private final SyncService _sync;

    public EmployerService(SyncService sync) {
        _sync = sync;
    }

    // Adds an employer record for a customer (customer.addEmployer).
    // type must be AddEmployer.TYPE_PRIMARY ("Primary") or TYPE_SECONDARY
    // ("Secondary"). salary may be blank; if non-blank it must match the
    // currency format (e.g. "80000" or "80000.00").
    public EmployerResult addEmployer(
            String customerNumber,
            String name,
            String position,
            String salary,
            String type) {
        ValidationResult validation = validateEmployer(customerNumber, name, type, salary);
        if (!validation.isValid()) {
            return EmployerResult.invalid(validation);
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("customer", customerNumber);
        fields.put("name", name);
        fields.put("position", position);
        fields.put("salary", Double.toString(parseSalary(salary)));
        fields.put("type", type);
        return queue("customer.addEmployer", customerNumber, fields);
    }

    // Updates an existing employer record (customer.updateEmployer).
    // Same validation rules as addEmployer.
    public EmployerResult updateEmployer(
            long id,
            String customerNumber,
            String name,
            String position,
            String salary,
            String type) {
        ValidationResult validation = validateEmployer(customerNumber, name, type, salary);
        if (!validation.isValid()) {
            return EmployerResult.invalid(validation);
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("customer", customerNumber);
        fields.put("id", Long.toString(id));
        fields.put("name", name);
        fields.put("position", position);
        fields.put("salary", Double.toString(parseSalary(salary)));
        fields.put("type", type);
        return queue("customer.updateEmployer", customerNumber, fields);
    }

    // Removes an employer record (customer.deleteEmployer).
    public EmployerResult deleteEmployer(long id, String customerNumber) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required("customerNumber", customerNumber));
        if (!validation.isValid()) {
            return EmployerResult.invalid(validation);
        }
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("customer", customerNumber);
        fields.put("id", Long.toString(id));
        return queue("customer.deleteEmployer", customerNumber, fields);
    }

    // AI-assisted: queue the change and wait for its first upload attempt. The service runs on a
    // BackgroundTasks thread (never the sync thread), so blocking here is safe; WAITING completes
    // straight away when offline, so the UI is not held on the network.
    private EmployerResult queue(String messageType, String customerNumber, Map<String, String> fields) {
        try {
            SyncStep step = _sync.queue(messageType, customerNumber, null, fields).join();
            switch (step.getOutcome()) {
                case SENT:
                    return EmployerResult.saved();
                case REJECTED:
                    return EmployerResult.rejected(step.getMessage());
                default: // WAITING, or PAUSED behind a possible-duplicate customer: saved, will upload
                    return EmployerResult.waiting(step.getMessage());
            }
        } catch (CompletionException e) {
            // the local save itself failed (e.g. the database could not be written)
            return EmployerResult.rejected("Could not save: " + e.getCause().getMessage());
        }
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
