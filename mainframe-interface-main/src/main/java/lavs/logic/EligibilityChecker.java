package lavs.logic;

import java.util.Collection;

// TEMPORARY: lives in lavs.logic until Scott's logic package skeleton is on
// main (see the same note on ConstantsService); move/adapt this once it is.
//
// The shared eligibility rule used by both US6 (first working slice) and US9
// (submit a loan for verification): a customer is eligible once they have a
// mailing address and at least one way to reach them - a phone number or an
// email.
//
// NOTE: whether a main borrower with no employer must also have a guarantor
// before a loan can become Pending is a separate, unresolved question (raised
// with James on Ed) and is deliberately NOT enforced here. Do not add it to
// this shared rule, or to US9's submit action, until that comes back answered.
public final class EligibilityChecker {
    private EligibilityChecker() {
    }

    public static boolean isEligible(boolean hasMailingAddress, boolean hasPhoneNumber, boolean hasEmail) {
        return hasMailingAddress && (hasPhoneNumber || hasEmail);
    }

    // convenience overload for callers holding the customer's address/contact
    // lists as returned by the mainframe gateway, e.g. CustomerAddress.List,
    // CustomerPhoneNumber.List and CustomerEmail.List results. A null or empty
    // collection counts as "doesn't have one".
    public static boolean isEligible(Collection<?> mailingAddresses, Collection<?> phoneNumbers, Collection<?> emails) {
        return isEligible(isPresent(mailingAddresses), isPresent(phoneNumbers), isPresent(emails));
    }

    private static boolean isPresent(Collection<?> values) {
        return values != null && !values.isEmpty();
    }
}
