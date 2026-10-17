package lavs.logic;

import lavs.data.ChangeQueue;
import lavs.data.UploadGateway;
import lavs.model.PendingChange;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// AI-assisted.

// The upload queue the app talks to. Every save is queued, then sent straight away if 
// the mainframe is reachable. Everything runs on one background thread so changes are sent in order
// and the UI won't wait on the network.

// queue() gives back a future that's filled in once the change has been tried:
// SENT, WAITING (offline, still queued), REJECTED or PAUSED.
// Screens that need the result (eg the loan screen before the summary) wait on it
// through the background-task helper so the UI doesn't freeze.
public class SyncService implements AutoCloseable {

    private final ChangeQueue queue;
    private final UploadGateway gateway;
    private final SyncProcessor processor;
    private final ScheduledExecutorService executor;
    private final Map<Long, CompletableFuture<SyncStep>> waiting = new ConcurrentHashMap<>();
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private volatile RuntimeException lastError;

    public SyncService(ChangeQueue queue, UploadGateway gateway, ScheduledExecutorService executor) {
        this.queue = queue;
        this.gateway = gateway;
        this.processor = new SyncProcessor(queue, gateway);
        this.executor = executor;
    }

    /** A service with its own background thread. */
    public static SyncService create(ChangeQueue queue, UploadGateway gateway) {
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "lavs-sync");
            thread.setDaemon(true);
            return thread;
        });
        return new SyncService(queue, gateway, executor);
    }

    /** Sends anything left from last time, then retries every interval while changes are waiting. */
    public void start(Duration retryInterval) {
        long millis = retryInterval.toMillis();
        executor.execute(this::drain);
        executor.scheduleWithFixedDelay(this::retryIfConnected, millis, millis, TimeUnit.MILLISECONDS);
    }

    /**
     * Queues a save and tries to send it straight away.
     *
     * @param messageType    the mainframe message, e.g. "customer.addAddress"
     * @param customerNumber the customer the change belongs to (may be temporary)
     * @param createsNumber  the temporary number this change creates, or null
     * @param fields         the message fields
     */
    public CompletableFuture<SyncStep> queue(String messageType, String customerNumber, String createsNumber,
                                             Map<String, String> fields) {
        CompletableFuture<SyncStep> result = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                PendingChange change = new PendingChange(messageType, customerNumber, createsNumber, fields);
                processor.prepareNew(change);
                queue.add(change);
                waiting.put(change.getId(), result);
                notifyListeners();
                drain();
            } catch (RuntimeException e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    /** Removes a rejected change once the MMM has seen it. */
    public CompletableFuture<Void> dismissRejected(long id) {
        return runOnSyncThread(() -> {
            for (PendingChange change : queue.all()) {
                if (change.getId() == id && change.getStatus() == PendingChange.Status.REJECTED) {
                    queue.delete(id);
                }
            }
        });
    }

    /** Duplicate found: the MMM picked the existing customer. */
    public CompletableFuture<Void> useExistingCustomer(String temporaryNumber, String existingNumber) {
        return runOnSyncThread(() -> processor.useExistingCustomer(temporaryNumber, existingNumber));
    }

    /** Duplicate found: the MMM confirmed it is a new customer. */
    public CompletableFuture<Void> confirmNewCustomer(String temporaryNumber) {
        return runOnSyncThread(() -> processor.confirmNewCustomer(temporaryNumber));
    }

    /** Every queued change in creation order, for the pending uploads panel. */
    public List<PendingChange> getChanges() {
        return queue.all();
    }

    /** How many changes are still to be uploaded (pending or paused), for the top bar. */
    public int getPendingCount() {
        int count = 0;
        for (PendingChange change : queue.all()) {
            if (change.getStatus() != PendingChange.Status.REJECTED) {
                count++;
            }
        }
        return count;
    }

    /** The real number for a temporary one once the mainframe has assigned it. */
    public String resolveNumber(String number) {
        return processor.resolveNumber(number);
    }

    /** Called on the sync thread whenever the queue changes. UI listeners must hop back to the UI thread. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    /** The last unexpected error on the sync thread (e.g. the local file could not be written), or null. */
    public RuntimeException getLastError() {
        return lastError;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }

    void retryIfConnected() {
        if (gateway.isConnected()) {
            drain();
        }
    }

    /** Sends changes oldest first until the queue is empty or the mainframe is unreachable. */
    void drain() {
        try {
            while (true) {
                Optional<SyncStep> step = processor.processNext();
                if (step.isEmpty()) {
                    break;
                }
                complete(step.get().getChange().getId(), step.get());
                notifyListeners();
                if (step.get().getOutcome() == SyncStep.Outcome.WAITING) {
                    break;
                }
            }
            settleWaiting();
        } catch (RuntimeException e) {
            lastError = e;
            notifyListeners();
        }
    }

    // Completes the futures of changes that were not sent this round, from their current status.
    private void settleWaiting() {
        for (PendingChange change : queue.all()) {
            complete(change.getId(), new SyncStep(change, outcomeFor(change.getStatus()), change.getStatusMessage()));
        }
    }

    private static SyncStep.Outcome outcomeFor(PendingChange.Status status) {
        switch (status) {
            case PAUSED:
                return SyncStep.Outcome.PAUSED;
            case REJECTED:
                return SyncStep.Outcome.REJECTED;
            default:
                return SyncStep.Outcome.WAITING;
        }
    }

    private void complete(Long id, SyncStep step) {
        CompletableFuture<SyncStep> future = waiting.remove(id);
        if (future != null) {
            future.complete(step);
        }
    }

    private CompletableFuture<Void> runOnSyncThread(Runnable action) {
        CompletableFuture<Void> done = new CompletableFuture<>();
        executor.execute(() -> {
            try {
                action.run();
                notifyListeners();
                drain();
                done.complete(null);
            } catch (RuntimeException e) {
                done.completeExceptionally(e);
            }
        });
        return done;
    }

    private void notifyListeners() {
        for (Runnable listener : listeners) {
            listener.run();
        }
    }
}
