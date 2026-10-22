// AI-assisted: small text checks shared by the UI controls.
package lavs.ui;

public final class UiText {
    private UiText() {
    }

    // true for anything other than null, empty or only spaces
    public static boolean hasText(String text) {
        return text != null && !text.isBlank();
    }

    // text with exactly one full stop at the end ("Add a mailing address" -> "Add a mailing address.")
    public static String endWithFullStop(String text) {
        return text.endsWith(".") ? text : text + ".";
    }
}
