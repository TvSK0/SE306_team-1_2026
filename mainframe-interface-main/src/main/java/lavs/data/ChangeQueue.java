package lavs.data;

import lavs.model.PendingChange;

import java.util.List;
import java.util.Optional;

// The queue of saves waiting for the mainframe. 
// SqlitePendingChangeStore keeps it on disk (within the sqlite file) and is used in production
// InMemoryChangeQueue is only ever used in tests
public interface ChangeQueue {

    // Stores the change, sets its id, returns the id.
    PendingChange add(PendingChange change);

    // The oldest change that is still PENDING (PAUSED and REJECTED changes are skipped)
    Optional<PendingChange> next();

    // Every single change in creation order (regardless of its status)
    List<PendingChange> all();

    // Saves a change's customer number, fields, status, message and duplicate flag.
    void update(PendingChange change);

    // Marks a change as REJECTED so it's shown to the MMM and never retried.
    void markRejected(long id, String reason);

    /** Atomically records accepted identifiers, updates dependants and removes accepted saves. */
    default void commit(List<PendingChange> updates, List<Long> removed, java.util.Map<String, String> numbers) {
        updates.forEach(this::update);
        removed.forEach(this::delete);
    }

    /** Previously accepted identifiers, retained across application restarts. */
    default java.util.Map<String, String> knownNumbers() { return java.util.Map.of(); }

    // Removes a change (eg once the mainframe has accepted it, no use for it anymore)
    void delete(long id);
}
