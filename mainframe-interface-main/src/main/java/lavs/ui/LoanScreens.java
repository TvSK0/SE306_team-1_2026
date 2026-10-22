package lavs.ui;

import java.io.IOException;
import java.io.UncheckedIOException;

// Registers the loan screen (US6 details, US7 repayments, US9 submit) with the Navigator. Open it
// with navigator.goTo(Screen.LOAN, LoanScreenRequest.forNewLoan(customer)) or forLoan(customer, loan).
// "Back to <customer>" goes back to whichever screen opened the loan.
public class LoanScreens {
    private final LoanScreenServices _services;

    public LoanScreens(LoanScreenServices services) {
        _services = services;
    }

    public void register(Navigator navigator) {
        navigator.register(Screen.LOAN, parameter -> {
            LoanScreenRequest request = LoanScreenRequest.from(parameter);
            try {
                return LoanScreenController.open(_services, request.customer(), request.loan(), navigator::back);
            } catch (IOException e) {
                // a missing or broken FXML file: the Navigator reports it and stays where it is
                throw new UncheckedIOException(e);
            }
        });
    }
}
