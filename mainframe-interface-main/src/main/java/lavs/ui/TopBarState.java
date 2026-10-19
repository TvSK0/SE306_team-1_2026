// AI-assisted: what the top bar shows (status text and style, user, title, visibility), with no JavaFX.
package lavs.ui;

// The top bar's decisions, kept apart from the TopBar control so they can be unit tested.
// It reads the StatusFeed each time it is asked, so after a feed change TopBar only has to
// ask again.
public final class TopBarState {
    static final String ONLINE = "online";
    static final String OFFLINE = "offline";

    private final StatusFeed _feed;
    private String _userName;
    private String _title = "";

    public TopBarState(StatusFeed feed) {
        _feed = feed;
    }

    // "online" or "offline": the style class that colours the pill and its dot
    public String getStatusStyle() {
        return _feed.isOnline() ? ONLINE : OFFLINE;
    }

    // the bold part of the pill: "Online" or "Offline"
    public String getStatusLabel() {
        return _feed.isOnline() ? "Online" : "Offline";
    }

    // the lighter part of the pill: "All changes uploaded" or "3 waiting to upload"
    // (the control puts an 8px gap between it and the label, no separator)
    public String getStatusDetail() {
        int pending = _feed.pendingCount();
        return pending == 0 ? "All changes uploaded" : pending + " waiting to upload";
    }

    // null or blank means nobody is signed in
    public void setUserName(String userName) {
        _userName = userName;
    }

    // "Signed in: J. Smith", or empty when nobody is signed in
    public String getUserText() {
        String shown = shortName(_userName);
        return shown.isEmpty() ? "" : "Signed in: " + shown;
    }

    // First initial and surname: "Jane Smith" and "Jane Mary Smith" both become "J. Smith".
    // A single word (a username such as "jsmith") is shown as it is; null or blank gives "".
    static String shortName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String[] words = name.trim().split("\\s+");
        if (words.length == 1) {
            return words[0];
        }
        return Character.toUpperCase(words[0].charAt(0)) + ". " + words[words.length - 1];
    }

    public void setTitle(String title) {
        _title = title;
    }

    public String getTitle() {
        return _title;
    }

    // the title to use until a screen sets a more specific one (the customer record sets
    // "Customer: Jane Smith (C-10234)" itself once the customer has loaded)
    public static String defaultTitleFor(Screen screen) {
        return switch (screen) {
            case LOGIN -> "";
            case CUSTOMER_SEARCH -> "Customer search";
            case CUSTOMER_RECORD -> "Customer";
            case LOAN -> "Loan";
            case CUSTOMER_SELECT -> "Select customer";
        };
    }

    // the login screen is a card on its own, with no top bar
    public static boolean isShownOn(Screen screen) {
        return screen != Screen.LOGIN;
    }
}
