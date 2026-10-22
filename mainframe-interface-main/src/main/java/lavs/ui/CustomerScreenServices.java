package lavs.ui;

import lavs.logic.ConstantsService;
import lavs.logic.CustomerRelationshipService;
import lavs.logic.CustomerService;

import java.util.Objects;

// AI-assisted: everything the customer record and its tabs call, passed around as one
// value (like LoanScreenServices) so a new tab only adds a field here. tasks is how each
// of them runs those calls off the UI thread.
public record CustomerScreenServices(CustomerService customers, CustomerRelationshipService relationships,
                                     ConstantsService constants, BackgroundTasks tasks) {
    public CustomerScreenServices {
        Objects.requireNonNull(customers, "customers");
        Objects.requireNonNull(relationships, "relationships");
        Objects.requireNonNull(constants, "constants");
        Objects.requireNonNull(tasks, "tasks");
    }
}
