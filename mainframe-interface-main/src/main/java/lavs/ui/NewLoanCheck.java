// AI-assisted: decides whether "+ New loan" may go ahead, and the Figma 08 banner when it may not.
package lavs.ui;

import lavs.logic.LoanService;
import lavs.logic.LookupResult;
import lavs.logic.StartLoanFacts;
import lavs.validation.ValidationResult;

import java.util.Optional;

// US6 acceptance criterion 1: "GIVEN the customer has no mailing address, or has no phone
// number or email, WHEN the MMM tries to start a loan, THEN the application stops them and
// says what is missing." Plain (no JavaFX), so it can be called from a test. It asks
// LoanService.checkCanStartLoan and returns either "go" or the banner to show.
public final class NewLoanCheck {
    // banner: why not (present when canStart is false). linkSection and linkText: the link under
    // the banner's body that takes the MMM to the section to fix (empty when there is no link).
    public record Decision(boolean canStart, Optional<BannerContent> banner, Optional<Section> linkSection,
                           String linkText) {
        static Decision go() {
            return new Decision(true, Optional.empty(), Optional.empty(), "");
        }

        static Decision blocked(BannerContent banner, Section linkSection, String linkText) {
            return new Decision(false, Optional.of(banner), Optional.ofNullable(linkSection), linkText);
        }
    }

    private NewLoanCheck() {
    }

    // facts is what CustomerLoansService.startLoanFacts read; a failure to read it blocks the
    // loan with the usual banner (offline, or the mainframe's reason)
    public static Decision decide(LoanService loanService, String customerName, LookupResult<StartLoanFacts> facts) {
        return switch (facts.getOutcome()) {
            case SUCCESS -> decide(loanService, customerName, facts.getValue());
            case NETWORK_FAILURE -> Decision.blocked(BannerContent.offline(), null, "");
            case REJECTED, INVALID -> Decision.blocked(couldntStart(facts.getMessage()), null, "");
        };
    }

    // customerName is the customer's full name; a blank one reads as "This customer"
    public static Decision decide(LoanService loanService, String customerName, StartLoanFacts facts) {
        ValidationResult missing = loanService.checkCanStartLoan(facts.hasMailingAddress(), facts.hasPhoneNumber(),
                facts.hasEmail());
        if (missing.isValid()) {
            return Decision.go();
        }
        String who = UiText.hasText(customerName) ? customerName.trim() : "This customer";
        boolean noMailing = !facts.hasMailingAddress();
        boolean noContact = !facts.hasPhoneNumber() && !facts.hasEmail();
        // the mailing address is listed first, and the link goes to the first thing missing
        String mailing = who + " has no mailing address. Add one, or mark an existing address as mailing";
        String contact = who + " has no phone number or email. Add one";
        String body = noMailing && noContact
                ? mailing + ". " + contact + ", then try again."
                : noMailing ? mailing + ", then try again." : contact + ", then try again.";
        BannerContent banner = new BannerContent(BannerVariant.ERROR, "Can't start a new loan", body);
        return noMailing
                ? Decision.blocked(banner, Section.ADDRESSES, "Go to Addresses →")
                : Decision.blocked(banner, Section.PHONE_NUMBERS, "Go to Phone numbers →");
    }

    static BannerContent couldntStart(String mainframeMessage) {
        String reason = UiText.hasText(mainframeMessage)
                ? "Reason given: " + UiText.endWithFullStop(mainframeMessage.trim())
                : "The mainframe gave no reason.";
        return new BannerContent(BannerVariant.ERROR, "Couldn't start a new loan", reason + " Try again.");
    }
}
