// AI-assisted: the answers in select mode's Contact details column, row by row.
package lavs.ui;

import lavs.logic.CustomerContactService.Check;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

// Plain, so it is unit tested; the screen reads tag(number) each time it draws a row. A row
// with no answer yet is "Checking…". Answers carry the generation they were asked in, so
// one that arrives after a newer search has replaced the rows is dropped (like a cancelled
// search, it never changes the screen).
public final class ContactDetailsColumn {
    // the tag shown in the cell
    public record ContactTag(TagVariant variant, String text) {
    }

    private final Map<String, Check> _answers = new HashMap<>();
    private int _generation;

    // A new search replaced the rows: forget every answer and ignore any still on their way.
    public void reset() {
        _generation++;
        _answers.clear();
    }

    // the generation to hand to finish(...) with each answer asked now
    public int generation() {
        return _generation;
    }

    public void finish(int generation, String customerNumber, Check check) {
        if (generation == _generation) {
            _answers.put(customerNumber, check);
        }
    }

    // The checking stopped unexpectedly: rows still waiting for an answer become FAILED, so
    // none is left saying "Checking…" for ever.
    public void failUnfinished(int generation, Collection<String> customerNumbers) {
        customerNumbers.forEach(number -> {
            if (generation == _generation) {
                _answers.putIfAbsent(number, Check.FAILED);
            }
        });
    }

    public ContactTag tag(String customerNumber) {
        Check check = _answers.get(customerNumber);
        if (check == null) {
            return new ContactTag(TagVariant.NEUTRAL, "Checking…");
        }
        return switch (check) {
            case COMPLETE -> new ContactTag(TagVariant.SUCCESS, "Complete");
            case MISSING -> new ContactTag(TagVariant.DANGER, "Missing phone or email");
            case FAILED -> new ContactTag(TagVariant.NEUTRAL, "Couldn't check");
        };
    }
}
