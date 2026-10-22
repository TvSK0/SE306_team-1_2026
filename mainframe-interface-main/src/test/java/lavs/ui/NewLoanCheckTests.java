// AI-assisted: tests for the Figma 08 banner, including US6 acceptance criterion 1.
package lavs.ui;

import lavs.logic.LoanService;
import lavs.validation.FieldError;
import lavs.validation.ValidationResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NewLoanCheckTests {
    // LT-R3: GIVEN the customer has no mailing address, WHEN the MMM tries to start a loan,
    // THEN the application stops them and says what is missing.
    @Test
    void noMailingAddressShowsTheFigma08BannerWithALinkToAddresses() {
        // Act
        NewLoanCheck.Blocked blocked = NewLoanCheck.blocked("Jane Smith", errors(LoanService.MAILING_ADDRESS)).orElseThrow();

        // Assert
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Can't start a new loan",
                "Jane Smith has no mailing address. Add one, or mark an existing address as mailing, then try again."),
                blocked.banner());
        Assertions.assertEquals(NewLoanCheck.Fix.ADDRESSES, blocked.fix());
        Assertions.assertEquals("Go to Addresses →", blocked.linkText());
    }

    // LT-R3: GIVEN the customer has no phone number or email, WHEN the MMM tries to start a loan,
    // THEN the application stops them and says what is missing.
    @Test
    void noPhoneOrEmailLinksToPhoneNumbers() {
        // Act
        NewLoanCheck.Blocked blocked = NewLoanCheck.blocked("Jane Smith", errors(LoanService.CONTACT_DETAILS)).orElseThrow();

        // Assert
        Assertions.assertEquals("Jane Smith has no phone number or email. Add one, then try again.",
                blocked.banner().body());
        Assertions.assertEquals(NewLoanCheck.Fix.PHONE_NUMBERS, blocked.fix());
        Assertions.assertEquals("Go to Phone numbers →", blocked.linkText());
    }

    // LT-R3 (both missing: the mailing address first, and the link goes to Addresses)
    @Test
    void whenBothAreMissingTheMailingAddressComesFirst() {
        // Act
        NewLoanCheck.Blocked blocked = NewLoanCheck.blocked("Jane Smith",
                errors(LoanService.MAILING_ADDRESS, LoanService.CONTACT_DETAILS)).orElseThrow();

        // Assert
        Assertions.assertEquals("Jane Smith has no mailing address. Add one, or mark an existing address as mailing. "
                + "Jane Smith has no phone number or email. Add one, then try again.", blocked.banner().body());
        Assertions.assertEquals(NewLoanCheck.Fix.ADDRESSES, blocked.fix());
    }

    // LT-R3
    @Test
    void aBlankNameReadsAsThisCustomer() {
        Assertions.assertTrue(NewLoanCheck.blocked(" ", errors(LoanService.CONTACT_DETAILS)).orElseThrow()
                .banner().body().startsWith("This customer has no phone"));
        Assertions.assertTrue(NewLoanCheck.blocked(null, errors(LoanService.CONTACT_DETAILS)).orElseThrow()
                .banner().body().startsWith("This customer"));
    }

    // LT-R3 (other field errors are not this banner's to explain)
    @Test
    void otherErrorsGiveNoBanner() {
        Assertions.assertTrue(NewLoanCheck.blocked("Jane Smith", errors("customerNumber")).isEmpty());
        Assertions.assertTrue(NewLoanCheck.blocked("Jane Smith", new ValidationResult()).isEmpty());
    }

    private static ValidationResult errors(String... fieldNames) {
        ValidationResult result = new ValidationResult();
        for (String name : fieldNames) {
            result.add(new FieldError(name, "is missing"));
        }
        return result;
    }
}
