// AI-assisted: checks every banner and tag variant points at a class that lavs.css defines.
package lavs.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariantStyleTests {
    private static String css() throws IOException {
        try (InputStream in = VariantStyleTests.class.getResourceAsStream("/lavs/ui/lavs.css")) {
            assertNotNull(in);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyBannerVariantHasACssRule() throws IOException {
        String css = css();
        for (BannerVariant variant : BannerVariant.values()) {
            assertTrue(css.contains("." + variant.getStyleClass()), variant.name());
        }
    }

    @Test
    void everyTagVariantHasACssRule() throws IOException {
        String css = css();
        for (TagVariant variant : TagVariant.values()) {
            assertTrue(css.contains("." + variant.getStyleClass()), variant.name());
        }
    }

    @Test
    void variantsHaveTheExpectedClassNames() {
        assertEquals("lavs-banner-error", BannerVariant.ERROR.getStyleClass());
        assertEquals("lavs-banner-warning", BannerVariant.WARNING.getStyleClass());
        assertEquals("lavs-banner-info", BannerVariant.INFO.getStyleClass());
        assertEquals("lavs-tag-success", TagVariant.SUCCESS.getStyleClass());
        assertEquals("lavs-tag-neutral", TagVariant.NEUTRAL.getStyleClass());
        assertEquals("lavs-tag-warning", TagVariant.WARNING.getStyleClass());
        assertEquals("lavs-tag-danger", TagVariant.DANGER.getStyleClass());
        assertEquals("lavs-tag-info", TagVariant.INFO.getStyleClass());
    }
}
