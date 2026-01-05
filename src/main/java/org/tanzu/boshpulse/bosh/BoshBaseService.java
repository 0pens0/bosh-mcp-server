package org.tanzu.boshpulse.bosh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.function.Supplier;

public abstract class BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshBaseService.class);
    
    protected final BoshCliExecutor cliExecutor;
    protected final int maxRetries;
    protected final Duration retryDelay;

    protected static final String DEPLOYMENT_PARAM = "Name of the BOSH deployment";
    protected static final String VM_PARAM = "Name or ID of the BOSH VM";
    protected static final String RELEASE_PARAM = "Name of the BOSH release";
    protected static final String STEMCELL_PARAM = "Name of the BOSH stemcell";
    protected static final String TASK_PARAM = "ID of the BOSH task";

    public BoshBaseService(BoshCliExecutor cliExecutor,
                          @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                          @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        this.cliExecutor = cliExecutor;
        this.maxRetries = maxRetries;
        this.retryDelay = Duration.ofSeconds(retryDelaySeconds);
    }

    /**
     * Execute a BOSH operation with retry logic for transient failures.
     * 
     * @param operation The operation to execute
     * @param operationName A descriptive name for logging purposes
     * @param <T> The return type of the operation
     * @return The result of the operation
     * @throws RuntimeException if all retry attempts fail
     */
    protected <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return operation.get();
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    logger.error("{} failed after {} attempts: {}", operationName, maxRetries, e.getMessage());
                    throw e;
                }
                
                // Check if this is a retryable error
                if (isRetryableError(e)) {
                    logger.warn("{} failed (attempt {}/{}), retrying in {}: {}", 
                               operationName, attempt, maxRetries, retryDelay, e.getMessage());
                    try {
                        Thread.sleep(retryDelay.toMillis());
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry", ie);
                    }
                } else {
                    // Non-retryable error, fail immediately
                    logger.error("{} failed with non-retryable error: {}", operationName, e.getMessage());
                    throw e;
                }
            }
        }
        throw new RuntimeException("All retry attempts failed for: " + operationName);
    }

    /**
     * Determine if an error is retryable based on the exception type and message.
     * 
     * @param e The exception to check
     * @return true if the error is retryable, false otherwise
     */
    private boolean isRetryableError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            return false;
        }
        
        // Retry on network-related errors
        return message.contains("timeout") ||
               message.contains("connection") ||
               message.contains("network") ||
               message.contains("unavailable") ||
               message.contains("temporary") ||
               message.contains("retry") ||
               e instanceof java.net.ConnectException ||
               e instanceof java.net.SocketTimeoutException ||
               e instanceof java.util.concurrent.TimeoutException;
    }
}
