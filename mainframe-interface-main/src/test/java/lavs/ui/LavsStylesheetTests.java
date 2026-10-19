// AI-assisted: guards lavs.css against a token being used but never defined.
package lavs.ui;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

// JavaFX only warns at runtime about an unknown looked-up colour, so check it here
// without starting the toolkit.
class LavsStylesheetTests {
    private static final Pattern DEFINITION = Pattern.compile("(-lavs-[a-z-]+)\\s*:");
    private static final Pattern USE = Pattern.compile("(-lavs-[a-z-]+)\\s*[;}]");
    private static final Pattern HEX = Pattern.compile("#[0-9a-fA-F]{3,8}\\b");

    private static String css() throws IOException {
        try (InputStream in = LavsStylesheetTests.class.getResourceAsStream("/lavs/ui/lavs.css")) {
            assertNotNull(in, "lavs.css is missing from the classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void everyTokenUsedIsDefined() throws IOException {
        String css = css();
        Set<String> defined = new HashSet<>();
        Matcher d = DEFINITION.matcher(css);
        while (d.find()) {
            defined.add(d.group(1));
        }
        Matcher u = USE.matcher(css);
        int uses = 0;
        while (u.find()) {
            uses++;
            assertTrue(defined.contains(u.group(1)), "undefined token " + u.group(1));
        }
        assertTrue(uses > 0);
    }

    @Test
    void hexColoursOnlyAppearInTheTokenBlock() throws IOException {
        String css = css();
        String afterRoot = css.substring(css.indexOf('}', css.indexOf(".root")));
        assertFalse(HEX.matcher(afterRoot).find(), "one-off hex code outside .root");
    }
}
