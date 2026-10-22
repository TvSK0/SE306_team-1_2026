// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.logic.ConstantsService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanService;
import lavs.logic.LoanStatusService;

import java.util.Objects;

// AI-assisted: everything the loan screen and its tabs call, passed around as one value so a
// new tab only adds a field here instead of a parameter to every open/show method.
// tasks is how each of them runs those calls off the UI thread.
public record LoanScreenServices(LoanService loans, LoanRepaymentsService repayments,
                                 ConstantsService constants, LoanStatusService statuses,
                                 BackgroundTasks tasks) {
    public LoanScreenServices {
        Objects.requireNonNull(loans, "loans");
        Objects.requireNonNull(repayments, "repayments");
        Objects.requireNonNull(constants, "constants");
        Objects.requireNonNull(statuses, "statuses");
        Objects.requireNonNull(tasks, "tasks");
    }
}
