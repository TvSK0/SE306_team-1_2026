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
// AI-assisted: reads go to the mainframe through the MainframeGateway; saves go through the
// upload queue (SyncService), so they work offline (US11). A new customer is queued under a
// temporary number: the queue checks the mainframe for a possible existing customer before
// uploading it, and swaps the temporary number for the real one in later changes once it is up.
public class CustomerService {
    public static final String CUSTOMER_NUMBER = "customerNumber";
    // AI-assisted: why a customer made offline can't be read yet
    public static final String NOT_UPLOADED_YET = "isn't on the mainframe yet; it will be once the customer uploads";

    private final MainframeGateway _gateway;
    private final SyncService _sync;
    private final long _organisation;
    private final Clock _clock;

    // organisation is the one new customers are created in
    public CustomerService(MainframeGateway gateway, SyncService sync, long organisation) {
        this(gateway, sync, organisation, Clock.systemDefaultZone());
    }

    // clock decides what "today" is, so tests can fix it
    CustomerService(MainframeGateway gateway, SyncService sync, long organisation, Clock clock) {
        _gateway = gateway;
        _sync = sync;
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
        // AI-assisted: a customer made offline is read by the number the mainframe gave them once
        // uploaded; until then the mainframe has nothing to read (US11)
        String uploadedNumber = _sync.resolveNumber(number.trim());
        if (TemporaryNumbers.isTemporary(uploadedNumber)) {
            ValidationResult notUploaded = new ValidationResult();
            notUploaded.add(new FieldError(CUSTOMER_NUMBER, NOT_UPLOADED_YET));
            return ServiceResult.invalid(notUploaded);
        }
        Retrieve message = new Retrieve(uploadedNumber);
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
        // AI-assisted: queued under a temporary number. Sent straight away it comes back with the
        // number the mainframe gave them; still waiting (or held as a possible duplicate) it keeps
        // the temporary one, which later changes for this customer use until it uploads.
        String temporaryNumber = TemporaryNumbers.create();
        SyncStep step = QueuedWrites.queue(_sync, message, temporaryNumber, temporaryNumber);
        String number = step.getOutcome() == SyncStep.Outcome.SENT ? step.getMessage() : temporaryNumber;
        return ServiceResult.queued(step, toCustomer(number, message.getTitle(), message.getFirstName(),
                message.getFamilyName(), message.getDateOfBirth(), message.getNationality(), null));
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
        // AI-assisted: queued, so the change works offline. The customer's status isn't part of
        // the change and the mainframe's answer doesn't come back through the queue, so it is
        // left empty for the screen to keep the one it shows.
        SyncStep step = QueuedWrites.queue(_sync, message, number.trim(), null);
        return ServiceResult.queued(step, toCustomer(number.trim(), message.getTitle(), message.getFirstName(),
                message.getFamilyName(), message.getDateOfBirth(), message.getNationality(), null));
    }

    // Builds the model from a message's values: what the mainframe sent back for a read, or what
    // was queued for a save. The three messages share no type, so each passes its getters in here.
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
