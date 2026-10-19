// AI-assisted: runs service calls off the UI thread and hands the result back on it.
package lavs.ui;

import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Every view calls its service through here so the window never freezes. Free of
// JavaFX so it can be unit tested: the app passes a background executor and
// Platform::runLater, tests pass Runnable::run (or a queue they drain by hand).
public final class BackgroundTasks {
    private final Executor _background;
    private final Consumer<Runnable> _uiThread;

    public BackgroundTasks(Executor background, Consumer<Runnable> uiThread) {
        _background = background;
        _uiThread = uiThread;
    }

    // Calls onStart now (the caller is on the UI thread), runs work in the background, then
    // calls onDone with the result or onError with what work threw, both on the UI thread.
    // Cancelling the returned handle means neither callback is ever called.
    public <T> Handle run(Supplier<T> work, Runnable onStart, Consumer<T> onDone, Consumer<Throwable> onError) {
        Handle handle = new Handle();
        onStart.run();
        _background.execute(() -> {
            // cancelled while still waiting for a free thread: do not even start
            if (handle.isCancelled()) {
                return;
            }
            try {
                T result = work.get();
                deliver(handle, () -> onDone.accept(result));
            } catch (Exception e) {
                deliver(handle, () -> onError.accept(e));
            }
        });
        return handle;
    }

    public <T> Handle run(Supplier<T> work, Consumer<T> onDone, Consumer<Throwable> onError) {
        return run(work, () -> { }, onDone, onError);
    }

    // The cancel check is made on the UI thread, so a cancel that lands after the work
    // finished but before the callback runs still wins.
    private void deliver(Handle handle, Runnable callback) {
        _uiThread.accept(() -> {
            if (!handle.isCancelled()) {
                callback.run();
            }
        });
    }

    // Lets a screen cancel a call it no longer wants, e.g. the Cancel button while searching.
    // The work itself is not interrupted, its result is just dropped.
    public static final class Handle {
        private volatile boolean _cancelled;

        public void cancel() {
            _cancelled = true;
        }

        public boolean isCancelled() {
            return _cancelled;
        }
    }
}
