// TEMPORARY: swap this package for Scott's ui package once his skeleton is on main.
package lavs.ui;

import lavs.logic.ConstantsService;
import lavs.logic.LoanRepaymentsService;
import lavs.logic.LoanService;

import java.io.IOException;
import java.io.UncheckedIOException;

// Registers the loan screen (US6 details, US7 repayments) with the Navigator. Open it with
// navigator.goTo(Screen.LOAN, LoanScreenRequest.forNewLoan(customer)) or forLoan(customer, loan).
// "Back to <customer>" goes back to whichever screen opened the loan.
public class LoanScreens {
    private final LoanService _loanService;
    private final LoanRepaymentsService _repaymentsService;
    private final ConstantsService _constantsService;

    public LoanScreens(LoanService loanService, LoanRepaymentsService repaymentsService,
                       ConstantsService constantsService) {
        _loanService = loanService;
        _repaymentsService = repaymentsService;
        _constantsService = constantsService;
    }

    public void register(Navigator navigator) {
        navigator.register(Screen.LOAN, parameter -> {
            LoanScreenRequest request = LoanScreenRequest.from(parameter);
            try {
                return LoanScreenController.open(_loanService, _repaymentsService, _constantsService,
                        request.customer(), request.loan(), navigator::back);
            } catch (IOException e) {
                // a missing or broken FXML file: the Navigator reports it and stays where it is
                throw new UncheckedIOException(e);
            }
        });
    }
}
