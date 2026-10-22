package lavs.logic;

import lavs.data.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

// AI-assisted: pending changes belong to the signed-in user, never the next user of the laptop.
public final class SyncSessions {
    private SyncSessions() {}
    public static SyncService open(Path directory, String username, MainframeGateway gateway) {
        if (username == null || username.isBlank()) throw new IllegalArgumentException("A signed-in user is required");
        try { Files.createDirectories(directory); }
        catch (IOException error) { throw new LocalStoreException("Could not create the local data folder", error); }
        String key = UUID.nameUUIDFromBytes(username.trim().getBytes(StandardCharsets.UTF_8)).toString();
        var queue = new SqlitePendingChangeStore(new LocalDatabase(directory.resolve("pending-" + key + ".db")));
        SyncService sync = SyncService.create(queue, new VerifiedUploads(gateway));
        gateway.setWriteDispatcher(new QueuedWrites(sync));
        gateway.setNumberResolver(sync::resolveNumber);
        gateway.setLocalReads(new PendingRecordReads(sync));
        return sync;
    }
}
