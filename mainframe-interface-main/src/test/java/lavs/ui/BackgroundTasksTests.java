// AI-assisted: tests BackgroundTasks with queues standing in for the threads, no JavaFX.
package lavs.ui;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundTasksTests {
    // runs queued jobs only when a test says so
    private static final class Queued implements Executor, Consumer<Runnable> {
        final Queue<Runnable> jobs = new ArrayDeque<>();

        @Override
        public void execute(Runnable job) {
            jobs.add(job);
        }

        @Override
        public void accept(Runnable job) {
            jobs.add(job);
        }

        void runAll() {
            while (!jobs.isEmpty()) {
                jobs.poll().run();
            }
        }
    }

    private final Queued background = new Queued();
    private final Queued ui = new Queued();
    private final BackgroundTasks tasks = new BackgroundTasks(background, ui);
    private final List<String> events = new ArrayList<>();

    @Test
    void successCallsStartThenDoneWithTheResult() {
        BackgroundTasks inline = new BackgroundTasks(Runnable::run, Runnable::run);

        inline.run(() -> "found", () -> events.add("start"),
                result -> events.add("done:" + result), error -> events.add("error"));

        assertEquals(List.of("start", "done:found"), events);
    }

    @Test
    void failureCallsOnErrorAndNotOnDone() {
        BackgroundTasks inline = new BackgroundTasks(Runnable::run, Runnable::run);

        inline.<String>run(() -> {
            throw new RuntimeException("boom");
        }, result -> events.add("done"), error -> events.add("error:" + error.getMessage()));

        assertEquals(List.of("error:boom"), events);
    }

    @Test
    void startRunsImmediatelyBeforeAnyBackgroundWork() {
        tasks.run(() -> {
            events.add("work");
            return 1;
        }, () -> events.add("start"), result -> events.add("done"), error -> events.add("error"));

        assertEquals(List.of("start"), events);
        background.runAll();
        assertEquals(List.of("start", "work"), events);
    }

    @Test
    void resultIsHandedToTheUiThreadNotCalledFromTheBackgroundThread() {
        tasks.run(() -> "x", result -> events.add("done"), error -> events.add("error"));

        background.runAll();
        assertTrue(events.isEmpty());
        ui.runAll();
        assertEquals(List.of("done"), events);
    }

    @Test
    void errorIsAlsoHandedToTheUiThread() {
        tasks.<String>run(() -> {
            throw new IllegalStateException("down");
        }, result -> events.add("done"), error -> events.add("error"));

        background.runAll();
        assertTrue(events.isEmpty());
        ui.runAll();
        assertEquals(List.of("error"), events);
    }

    @Test
    void cancelledBeforeItStartsNeverRunsTheWork() {
        BackgroundTasks.Handle handle = tasks.run(() -> {
            events.add("work");
            return 1;
        }, result -> events.add("done"), error -> events.add("error"));

        handle.cancel();
        background.runAll();
        ui.runAll();

        assertTrue(events.isEmpty());
    }

    @Test
    void cancelledWhileRunningDropsTheResult() {
        BackgroundTasks.Handle[] handle = new BackgroundTasks.Handle[1];
        handle[0] = tasks.run(() -> {
            handle[0].cancel();
            return 1;
        }, result -> events.add("done"), error -> events.add("error"));

        background.runAll();
        ui.runAll();

        assertTrue(events.isEmpty());
    }

    @Test
    void cancelledWhileRunningDropsTheError() {
        BackgroundTasks.Handle[] handle = new BackgroundTasks.Handle[1];
        handle[0] = tasks.<String>run(() -> {
            handle[0].cancel();
            throw new RuntimeException("late");
        }, result -> events.add("done"), error -> events.add("error"));

        background.runAll();
        ui.runAll();

        assertTrue(events.isEmpty());
    }

    @Test
    void cancelledAfterTheWorkButBeforeTheUiRunsDropsTheResult() {
        BackgroundTasks.Handle handle = tasks.run(() -> "x",
                result -> events.add("done"), error -> events.add("error"));

        background.runAll();
        handle.cancel();
        ui.runAll();

        assertTrue(events.isEmpty());
    }

    @Test
    void cancellingAfterCompletionChangesNothing() {
        BackgroundTasks inline = new BackgroundTasks(Runnable::run, Runnable::run);
        BackgroundTasks.Handle handle = inline.run(() -> "x",
                result -> events.add("done"), error -> events.add("error"));

        handle.cancel();

        assertEquals(List.of("done"), events);
        assertTrue(handle.isCancelled());
    }

    @Test
    void aNewHandleIsNotCancelled() {
        assertFalse(tasks.run(() -> 1, r -> { }, e -> { }).isCancelled());
    }
}
