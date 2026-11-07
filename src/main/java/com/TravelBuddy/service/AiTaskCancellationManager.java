package com.TravelBuddy.service;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * Manages tracking and cancellation of asynchronous AI generation tasks.
 */
@Component
public class AiTaskCancellationManager {
    public static final long DEFAULT_AI_TIMEOUT_SECONDS = 60;

    private final Map<String, Thread> activeThreads = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> activeFutures = new ConcurrentHashMap<>();

    /**
     * Terminates an in-progress AI generation task.
     *
     * @param requestId unique identifier of the task to cancel; blank IDs are ignored
     */
    public void cancel(String requestId) {
        if (!hasRequestId(requestId)) {
            return;
        }

        CompletableFuture<String> future = activeFutures.remove(requestId);
        if (future != null) {
            future.cancel(true);
        }

        Thread thread = activeThreads.remove(requestId);
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    /**
     * Executes an AI task asynchronously with support for cancellation and timeout.
     *
     * @param requestId identifier used to track and cancel this task
     * @param task supplier providing the AI generation logic
     * @param timeoutSeconds maximum duration to wait for task completion
     * @return result of the AI task
     * @throws InterruptedException if the task was cancelled or interrupted
     * @throws ExecutionException if the AI generation task failed
     * @throws TimeoutException if execution exceeded the timeout
     */
    public String executeWithCancellation(
            String requestId,
            Supplier<String> task,
            long timeoutSeconds) throws InterruptedException, ExecutionException, TimeoutException {

        if (hasRequestId(requestId)) {
            activeThreads.put(requestId, Thread.currentThread());
        }

        try {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("AI generation cancelled.");
            }

            CompletableFuture<String> future = CompletableFuture.supplyAsync(task);
            if (hasRequestId(requestId)) {
                activeFutures.put(requestId, future);
            }

            try {
                return future.get(timeoutSeconds, TimeUnit.SECONDS);
            } catch (CancellationException e) {
                throw new InterruptedException("AI generation cancelled.");
            } catch (InterruptedException | TimeoutException e) {
                future.cancel(true);
                throw e;
            }
        } finally {
            if (hasRequestId(requestId)) {
                activeThreads.remove(requestId);
                activeFutures.remove(requestId);
            }
        }
    }

    /**
     * Checks whether a request identifier is valid for tracking.
     *
     * @param requestId request identifier to validate
     * @return true if non-null and not blank
     */
    public boolean hasRequestId(String requestId) {
        return requestId != null && !requestId.isBlank();
    }
}
