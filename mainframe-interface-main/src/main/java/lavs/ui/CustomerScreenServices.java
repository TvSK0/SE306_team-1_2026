package lavs.ui;

import lavs.logic.AddressService;
import lavs.logic.EmployerService;
import lavs.logic.CustomerLoansService;
import lavs.logic.CustomerEligibilityService;
import lavs.logic.LoanOperations;
import lavs.logic.CategoryValues;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.CustomerOperations;
import lavs.logic.EmailService;
import lavs.logic.PhoneNumberService;

import lavs.logic.RecordNotesService;
import lavs.logic.RecordDocumentsService;
import java.util.Objects;

// AI-assisted: everything the customer record and its tabs call, passed around as one
// value (like LoanScreenServices) so a new tab only adds a field here. tasks is how each
// of them runs those calls off the UI thread.
public record CustomerScreenServices(CustomerOperations customers, AddressService addresses,
                                     PhoneNumberService phoneNumbers, EmailService emails,
                                     CustomerRelationshipService relationships,
                                     CategoryValues constants, BackgroundTasks tasks, EmployerService employers,
                                     CustomerLoansService customerLoans, CustomerEligibilityService eligibility,
                                     LoanOperations loans, Navigator navigator,
                                     RecordNotesService notes, RecordDocumentsService documents) {
    public CustomerScreenServices {
        Objects.requireNonNull(customers, "customers");
        Objects.requireNonNull(addresses, "addresses");
        Objects.requireNonNull(phoneNumbers, "phoneNumbers");
        Objects.requireNonNull(emails, "emails");
        Objects.requireNonNull(relationships, "relationships");
        Objects.requireNonNull(constants, "constants");
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(employers, "employers");
        Objects.requireNonNull(customerLoans, "customerLoans");
        Objects.requireNonNull(eligibility, "eligibility");
        Objects.requireNonNull(loans, "loans");
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(documents, "documents");
        Objects.requireNonNull(navigator, "navigator");
    }
}
