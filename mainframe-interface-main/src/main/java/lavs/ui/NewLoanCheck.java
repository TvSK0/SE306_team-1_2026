// AI-assisted: the Figma 08 banner for a loan that can't be started, from the eligibility check's errors.
package lavs.ui;

import lavs.logic.LoanService;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;

import java.util.Optional;

// US6 acceptance criterion 1: "GIVEN the customer has no mailing address, or has no phone
// number or email, WHEN the MMM tries to start a loan, THEN the application stops them and says
// what is missing." CustomerEligibilityService decides; this turns its field errors
// (LoanService.MAILING_ADDRESS, LoanService.CONTACT_DETAILS) into the banner and its link.
// Plain, so it is unit tested.
public final class NewLoanCheck {
    // which tab the banner's link opens
    public enum Fix { ADDRESSES, PHONE_NUMBERS }

    public record Blocked(BannerContent banner, Fix fix, String linkText) {
    }

    private NewLoanCheck() {
    }

    // Empty when the errors name neither a missing mailing address nor missing contact details.
    // customerName is the customer's full name; a blank one reads as "This customer".
    public static Optional<Blocked> blocked(String customerName, ValidationResult errors) {
        boolean noMailing = names(errors, LoanService.MAILING_ADDRESS);
        boolean noContact = names(errors, LoanService.CONTACT_DETAILS);
        if (!noMailing && !noContact) {
            return Optional.empty();
        }
        String who = UiText.hasText(customerName) ? customerName.trim() : "This customer";
        // the mailing address is listed first, and the link goes to the first thing missing
        String mailing = who + " has no mailing address. Add one, or mark an existing address as mailing";
        String contact = who + " has no phone number or email. Add one";
        String body = noMailing && noContact
                ? mailing + ". " + contact + ", then try again."
                : noMailing ? mailing + ", then try again." : contact + ", then try again.";
        BannerContent banner = new BannerContent(BannerVariant.ERROR, "Can't start a new loan", body);
        return Optional.of(noMailing
                ? new Blocked(banner, Fix.ADDRESSES, "Go to Addresses →")
                : new Blocked(banner, Fix.PHONE_NUMBERS, "Go to Phone numbers →"));
    }

    private static boolean names(ValidationResult errors, String fieldName) {
        return errors.getErrors().stream().map(FieldError::getFieldName).anyMatch(fieldName::equals);
    }
}
