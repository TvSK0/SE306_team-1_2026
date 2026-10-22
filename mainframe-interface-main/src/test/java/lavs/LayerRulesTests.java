package lavs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

// AI-assisted. The working rules for the layers, checked on every build so they can't
// drift: "ui never imports Connection, the message classes or JDBC. logic never imports
// JDBC or JavaFX. data is called only by logic." Each layer's package-info.java says the
// same in words. Reads the source files, so it catches fully qualified names as well as
// imports, and ignores comments.
class LayerRulesTests {
    private static final Path SOURCES = Path.of("src", "main", "java", "lavs");

    // what each layer must not refer to, as package or class name prefixes
    private static final Map<String, List<String>> FORBIDDEN = Map.of(
            "ui", List.of("java.sql", "lavs.data", "mainframe.client.messages",
                    "mainframe.client.Connection", "mainframe.client.HttpConnection"),
            "logic", List.of("javafx", "java.sql", "lavs.ui"),
            "data", List.of("javafx", "lavs.ui", "lavs.logic"),
            "model", List.of("javafx", "java.sql", "mainframe", "lavs.ui", "lavs.logic", "lavs.data", "lavs.validation"),
            "validation", List.of("javafx", "java.sql", "mainframe", "lavs.ui", "lavs.logic", "lavs.data", "lavs.model"));

    // LavsApp starts the app and builds every service, so it is the one ui class that
    // creates data classes and the Connection to hand to logic
    private static final Set<String> COMPOSITION_ROOT = Set.of("ui/LavsApp.java");

    @ParameterizedTest
    @ValueSource(strings = {"ui", "logic", "data", "model", "validation"})
    void eachLayerKeepsToItsRules(String layer) throws IOException {
        List<String> violations = new ArrayList<>();
        for (Path file : javaFiles(SOURCES.resolve(layer))) {
            String name = layer + "/" + SOURCES.resolve(layer).relativize(file).toString().replace('\\', '/');
            if (COMPOSITION_ROOT.contains(name)) {
                continue;
            }
            for (String reference : references(Files.readString(file), FORBIDDEN.get(layer))) {
                violations.add(name + " uses " + reference);
            }
        }

        assertEquals(List.of(), violations, "the " + layer + " layer must not use " + FORBIDDEN.get(layer));
    }

    @Test
    void everyClassIsInOneOfTheLayers() throws IOException {
        List<String> outside = new ArrayList<>();
        for (Path file : javaFiles(SOURCES)) {
            String layer = SOURCES.relativize(file).getName(0).toString();
            if (!FORBIDDEN.containsKey(layer)) {
                outside.add(SOURCES.relativize(file).toString());
            }
        }

        // a new package needs its own rules above (and a package-info.java) first
        assertEquals(List.of(), outside);
    }

    @Test
    void theCompositionRootStillExists() {
        // if LavsApp moves, the exception above must move with it rather than go stale
        for (String file : COMPOSITION_ROOT) {
            assertTrue(Files.exists(SOURCES.resolve(file)), file);
        }
    }

    // --- the checker itself, so the rules above can't pass by accident ---

    @Test
    void findsAnImport() {
        assertEquals(List.of("java.sql"), references("import java.sql.Connection;\nclass A {}", List.of("java.sql")));
    }

    @Test
    void findsAFullyQualifiedName() {
        String source = "class A { void f() { new mainframe.client.messages.loan.Retrieve(\"L-1\"); } }";

        assertEquals(List.of("mainframe.client.messages"), references(source, List.of("mainframe.client.messages")));
    }

    @Test
    void ignoresComments() {
        String source = """
                // swap javafx.scene.Node for something else
                /* a lavs.ui note
                   over two lines */
                class A {}""";

        assertEquals(List.of(), references(source, List.of("javafx", "lavs.ui")));
    }

    @Test
    void matchesWholeNamesOnly() {
        String source = "import lavs.database.Thing;\nimport mainframe.client.ConnectionPool;\nimport my.javafx.Fake;";

        assertEquals(List.of(), references(source, List.of("lavs.data", "mainframe.client.Connection", "javafx")));
    }

    // the forbidden prefixes the source refers to outside comments, in the order given
    static List<String> references(String source, List<String> forbidden) {
        String code = source.replaceAll("(?s)/\\*.*?\\*/", " ").replaceAll("//[^\\n]*", " ");
        List<String> found = new ArrayList<>();
        for (String prefix : forbidden) {
            // a whole name: not part of a longer name either side (lavs.data is not lavs.database)
            Pattern pattern = Pattern.compile("(?<![\\w.])" + Pattern.quote(prefix) + "(?!\\w)");
            if (pattern.matcher(code).find()) {
                found.add(prefix);
            }
        }
        return found;
    }

    private static List<Path> javaFiles(Path folder) throws IOException {
        assertTrue(Files.isDirectory(folder), "missing " + folder.toAbsolutePath());
        try (Stream<Path> files = Files.walk(folder)) {
            return files.filter(file -> file.toString().endsWith(".java")).sorted().toList();
        }
    }
}
