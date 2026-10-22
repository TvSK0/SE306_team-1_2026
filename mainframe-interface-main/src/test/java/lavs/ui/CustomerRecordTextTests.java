// AI-assisted: tests for the customer record frame's wording.
package lavs.ui;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomerRecordTextTests {
    // CR-R3
    @Test
    void anUnexpectedFailureShowsItsMessage() {
        Assertions.assertEquals(new BannerContent(BannerVariant.ERROR, "Couldn't load the customer",
                "Something went wrong: boom."), CustomerRecordText.unexpected(new IllegalStateException("boom")));
    }

    // CR-R3
    @Test
    void anUnexpectedFailureWithoutAMessageStillShowsABanner() {
        Assertions.assertEquals("Something went wrong.",
                CustomerRecordText.unexpected(new IllegalStateException()).body());
    }
}
