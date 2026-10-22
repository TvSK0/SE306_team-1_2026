package lavs.ui;

import lavs.logic.AddressService;
import lavs.logic.ConstantsService;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.CustomerService;
import lavs.logic.EmailService;
import lavs.logic.PhoneNumberService;

import java.util.Objects;

// AI-assisted: everything the customer record and its tabs call, passed around as one
// value (like LoanScreenServices) so a new tab only adds a field here. tasks is how each
// of them runs those calls off the UI thread.
public record CustomerScreenServices(CustomerService customers, AddressService addresses,
                                     PhoneNumberService phoneNumbers, EmailService emails,
                                     CustomerRelationshipService relationships,
                                     ConstantsService constants, BackgroundTasks tasks) {
    public CustomerScreenServices {
        Objects.requireNonNull(customers, "customers");
        Objects.requireNonNull(addresses, "addresses");
        Objects.requireNonNull(phoneNumbers, "phoneNumbers");
        Objects.requireNonNull(emails, "emails");
        Objects.requireNonNull(relationships, "relationships");
        Objects.requireNonNull(constants, "constants");
        Objects.requireNonNull(tasks, "tasks");
    }
}
