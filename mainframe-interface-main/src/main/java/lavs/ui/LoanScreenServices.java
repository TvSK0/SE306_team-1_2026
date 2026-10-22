package lavs.ui;

import lavs.logic.CategoryValues;
import lavs.logic.LoanBorrowerService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanOperations;
import lavs.logic.LoanStatusService;

import lavs.logic.RecordNotesService;
import lavs.logic.RecordDocumentsService;
import java.util.Objects;

// AI-assisted: everything the loan screen and its tabs call, passed around as one value so a
// new tab only adds a field here instead of a parameter to every open/show method.
// tasks is how each of them runs those calls off the UI thread.
public record LoanScreenServices(LoanOperations loans, LoanRepaymentsService repayments,
                                 CategoryValues constants, LoanStatusService statuses,
                                 LoanBorrowerService borrowers, BackgroundTasks tasks, Navigator navigator,
                                     RecordNotesService notes, RecordDocumentsService documents) {
    public LoanScreenServices {
        Objects.requireNonNull(loans, "loans");
        Objects.requireNonNull(repayments, "repayments");
        Objects.requireNonNull(constants, "constants");
        Objects.requireNonNull(statuses, "statuses");
        Objects.requireNonNull(borrowers, "borrowers");
        Objects.requireNonNull(tasks, "tasks");
        Objects.requireNonNull(notes, "notes");
        Objects.requireNonNull(documents, "documents");
        Objects.requireNonNull(navigator, "navigator");
    }
}
