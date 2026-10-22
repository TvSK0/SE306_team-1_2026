// AI-assisted: what the mainframe holds that decides whether a customer may start a loan.
package lavs.logic;

// The three facts LoanService.checkCanStartLoan judges (US6): a mailing address, and a phone
// number or an email.
public record StartLoanFacts(boolean hasMailingAddress, boolean hasPhoneNumber, boolean hasEmail) {
}
