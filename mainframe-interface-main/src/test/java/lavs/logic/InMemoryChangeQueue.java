package lavs.logic;

import lavs.data.ChangeQueue;
import lavs.model.PendingChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// AI-assisted: test helper generated with Claude (team-approved).
/** A ChangeQueue kept in a list, so sync rules can be tested without SQLite. */
class InMemoryChangeQueue implements ChangeQueue {

    private final List<PendingChange> changes = new ArrayList<>();
    private long nextId = 1;

    @Override
    public synchronized PendingChange add(PendingChange change) {
        change.setId(nextId++);
        changes.add(change);
        return change;
    }

    @Override
    public synchronized Optional<PendingChange> next() {
        return changes.stream().filter(c -> c.getStatus() == PendingChange.Status.PENDING).findFirst();
    }

    @Override
    public synchronized List<PendingChange> all() {
        return new ArrayList<>(changes);
    }

    @Override
    public synchronized void update(PendingChange change) {
        // the list holds the same objects, so changes are already visible
    }

    @Override
    public synchronized void markRejected(long id, String reason) {
        for (PendingChange change : changes) {
            if (change.getId() == id) {
                change.setStatus(PendingChange.Status.REJECTED);
                change.setStatusMessage(reason);
            }
        }
    }

    @Override
    public synchronized void delete(long id) {
        changes.removeIf(c -> c.getId() == id);
    }
}
