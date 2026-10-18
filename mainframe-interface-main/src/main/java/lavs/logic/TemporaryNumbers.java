package lavs.logic;

import java.util.UUID;

// Temporary numbers given to customers and loans created whilst offline. They are
// swapped for the mainframe's real number in every pending change once the record's uploaded.
public final class TemporaryNumbers {

    public static final String PREFIX = "TMP-";

    private TemporaryNumbers() {
    }

    public static String create() {
        return PREFIX + UUID.randomUUID();
    }

    public static boolean isTemporary(String number) {
        return number != null && number.startsWith(PREFIX);
    }
}
