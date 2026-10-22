// AI-assisted: loads one customer for the customer record header over the mainframe Connection.
// TEMPORARY: swap this package for Scott's logic package once his skeleton is on main.
package lavs.logic;

import lavs.model.Customer;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.Connection;
import mainframe.client.Status;
import mainframe.client.StatusMessage;
import mainframe.client.messages.customer.Retrieve;

import java.io.IOException;

// The customer record's header: name, status, number and date of birth, from customer.Retrieve.
// Reading needs a connection, so a network failure is returned for the screen to report.
// TEMPORARY: uses Connection directly until Scott's gateway lands; swap then.
public class CustomerRecordService {
    public static final String CUSTOMER_NUMBER = "customerNumber";

    private final Connection _connection;

    public CustomerRecordService(Connection connection) {
        _connection = connection;
    }

    public CustomerRecordResult load(String customerNumber) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, customerNumber));
        if (!validation.isValid()) {
            return CustomerRecordResult.invalid(validation);
        }
        Retrieve message = new Retrieve(customerNumber.trim());
        Status status;
        try {
            status = message.send(_connection);
        } catch (IOException ex) {
            return CustomerRecordResult.networkFailure(ex.getMessage());
        }
        if (status.getWasSuccessful()) {
            Customer customer = new Customer();
            customer.setNumber(message.getNumber());
            customer.setTitle(message.getTitle());
            customer.setFirstName(message.getFirstName());
            customer.setFamilyName(message.getFamilyName());
            customer.setDateOfBirth(CustomerSearchService.parseDate(message.getDateOfBirth()));
            customer.setNationality(message.getNationality());
            customer.setStatus(message.getStatus());
            return CustomerRecordResult.success(customer);
        }
        if (StatusMessage.NETWORK_FAILURE_UNAVAILABLE.getCode().equals(status.getErrorCode())) {
            return CustomerRecordResult.networkFailure(status.getErrorMessage());
        }
        return CustomerRecordResult.rejected(status.getErrorMessage());
    }
}
