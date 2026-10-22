package lavs.data;

// AI-assisted: local transport outcomes, deliberately outside the mainframe status-code range.
// These are never sent to the server and never count as mainframe success.
public final class LocalSaveStatus {
    public static final int WAITING = -1001;
    public static final int PAUSED = -1002;
    private LocalSaveStatus() {}
}
