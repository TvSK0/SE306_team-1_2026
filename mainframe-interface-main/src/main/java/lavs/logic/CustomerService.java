package lavs.logic;

import lavs.data.GatewayResult;
import lavs.data.MainframeGateway;
import lavs.model.Customer;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import lavs.validation.Validators;
import mainframe.client.messages.customer.Create;
import mainframe.client.messages.customer.Retrieve;
import mainframe.client.messages.customer.Update;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

// AI-assisted: US3, add and edit a customer, using customer.Create, customer.Update and
// customer.Retrieve through the MainframeGateway.
//
// A customer needs a first name, a family name and a date of birth: they identify the
// person, and the upload queue's duplicate check matches on exactly those three. Title
// and nationality are optional.
//
// Saves go through the gateway, which hands every write to the upload queue (QueuedWrites)
// once LavsApp has set it up. A customer created offline gets a temporary number until the
// mainframe gives them a real one.
public class CustomerService implements CustomerOperations {
    public static final String CUSTOMER_NUMBER = "customerNumber";

    private final MainframeGateway _gateway;
    private final long _organisation;
    private final Clock _clock;

    // organisation is the one new customers are created in
    public CustomerService(MainframeGateway gateway, long organisation) {
        this(gateway, organisation, Clock.systemDefaultZone());
    }

    // clock decides what "today" is, so tests can fix it
    CustomerService(MainframeGateway gateway, long organisation, Clock clock) {
        _gateway = gateway;
        _organisation = organisation;
        _clock = clock;
    }

    // Checks the form without contacting the mainframe. Each field's checks stop at its
    // first problem, so a field never reports two errors at once.
    public ValidationResult validate(CustomerInput input) {
        ValidationResult result = new ValidationResult();
        result.addIfPresent(Validators.required(CustomerInput.FIRST_NAME, input.getFirstName()));
        result.addIfPresent(Validators.required(CustomerInput.FAMILY_NAME, input.getFamilyName()));

        Optional<FieldError> dateOfBirthError = Validators.required(CustomerInput.DATE_OF_BIRTH, input.getDateOfBirth());
        if (dateOfBirthError.isEmpty()) {
            dateOfBirthError = Validators.date(CustomerInput.DATE_OF_BIRTH, input.getDateOfBirth().trim());
        }
        if (dateOfBirthError.isEmpty() && LocalDate.parse(input.getDateOfBirth().trim()).isAfter(LocalDate.now(_clock))) {
            dateOfBirthError = Optional.of(new FieldError(CustomerInput.DATE_OF_BIRTH, "cannot be in the future"));
        }
        result.addIfPresent(dateOfBirthError);
        return result;
    }

    // Reads a customer's details, e.g. to open their record.
    public ServiceResult<Customer> loadCustomer(String number) {
        ValidationResult validation = new ValidationResult();
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, number));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        Retrieve message = new Retrieve(number.trim());
        GatewayResult result = _gateway.send(message);
        return ServiceResult.from(result, toCustomer(message.getNumber(), message.getTitle(), message.getFirstName(),
                message.getFamilyName(), message.getDateOfBirth(), message.getNationality(), message.getStatus()));
    }

    // Creates the customer. On success the result holds them with the number the
    // mainframe gave them.
    public ServiceResult<Customer> createCustomer(CustomerInput input) {
        ValidationResult validation = validate(input);
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        Create message = new Create(input.getDateOfBirth().trim(), input.getFamilyName().trim(),
                input.getFirstName().trim(), optional(input.getNationality()), _organisation, optional(input.getTitle()));
        GatewayResult result = _gateway.send(message);
        if (result.isQueued()) {
            return ServiceResult.from(result, toCustomer(result.getValue("number"), input.getTitle(), input.getFirstName().trim(),
                    input.getFamilyName().trim(), input.getDateOfBirth().trim(), input.getNationality(), "Waiting to upload"));
        }
        return ServiceResult.from(result, toCustomer(message.getNumber(), message.getTitle(), message.getFirstName(),
                message.getFamilyName(), message.getDateOfBirth(), message.getNationality(), message.getStatus()));
    }

    // Saves changes to an existing customer.
    public ServiceResult<Customer> updateCustomer(String number, CustomerInput input) {
        ValidationResult validation = validate(input);
        validation.addIfPresent(Validators.required(CUSTOMER_NUMBER, number));
        if (!validation.isValid()) {
            return ServiceResult.invalid(validation);
        }
        Update message = new Update(input.getDateOfBirth().trim(), input.getFamilyName().trim(),
                input.getFirstName().trim(), optional(input.getNationality()), number.trim(), optional(input.getTitle()));
        GatewayResult result = _gateway.send(message);
        if (result.isQueued()) {
            return ServiceResult.from(result, toCustomer(number.trim(), input.getTitle(), input.getFirstName().trim(),
                    input.getFamilyName().trim(), input.getDateOfBirth().trim(), input.getNationality(), "Waiting to upload"));
        }
        return ServiceResult.from(result, toCustomer(message.getNumber(), message.getTitle(), message.getFirstName(),
                message.getFamilyName(), message.getDateOfBirth(), message.getNationality(), message.getStatus()));
    }

    // Builds the model from what the mainframe sent back. The three messages share no type,
    // so each passes its getters in here. Only called with a value that is used on success.
    private static Customer toCustomer(String number, String title, String firstName, String familyName,
                                       String dateOfBirth, String nationality, String status) {
        Customer customer = new Customer();
        customer.setNumber(number);
        customer.setTitle(title);
        customer.setFirstName(firstName);
        customer.setFamilyName(familyName);
        customer.setDateOfBirth(parseDate(dateOfBirth));
        customer.setNationality(nationality);
        customer.setStatus(status);
        return customer;
    }

    // the mainframe may send no date, or one we cannot read; the record then shows none
    private static LocalDate parseDate(String text) {
        if (text == null || Validators.date(CustomerInput.DATE_OF_BIRTH, text).isPresent()) {
            return null;
        }
        return LocalDate.parse(text);
    }

    // an optional field left blank is sent as nothing rather than as spaces
    private static String optional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
