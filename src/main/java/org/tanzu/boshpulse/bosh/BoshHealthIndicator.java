package org.tanzu.boshpulse.bosh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Health indicator for BOSH Director connectivity.
 * Tests the connection to the BOSH Director and reports health status.
 */
@Component
public class BoshHealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(BoshHealthIndicator.class);
    
    private final BoshCliExecutor cliExecutor;
    private final String director;
    private final AtomicLong lastSuccessfulCheck = new AtomicLong(0);
    private final AtomicLong lastFailedCheck = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    public BoshHealthIndicator(BoshCliExecutor cliExecutor,
                              @Value("${bosh.director}") String director) {
        this.cliExecutor = cliExecutor;
        this.director = director;
    }

    /**
     * Perform a health check for BOSH Director connectivity.
     * 
     * @return true if the connection is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            logger.debug("Performing BOSH Director health check for: {}", director);
            
            // Test BOSH connectivity by listing deployments (lightweight operation)
            if (!cliExecutor.isCliAvailable()) {
                logger.warn("BOSH CLI is not available");
                return false;
            }
            
            cliExecutor.testConnection();
            
            // Update success metrics
            lastSuccessfulCheck.set(System.currentTimeMillis());
            failureCount.set(0); // Reset failure count on success
            
            logger.debug("BOSH Director health check successful");
            return true;
                
        } catch (Exception e) {
            // Update failure metrics
            lastFailedCheck.set(System.currentTimeMillis());
            failureCount.incrementAndGet();
            
            logger.warn("BOSH Director health check failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get detailed health information.
     * 
     * @return Health information as a string
     */
    public String getHealthInfo() {
        boolean healthy = isHealthy();
        return String.format("BOSH Health: %s, Director: %s, Last Success: %d, Failures: %d", 
                           healthy ? "UP" : "DOWN", 
                           director, 
                           lastSuccessfulCheck.get(), 
                           failureCount.get());
    }
    
    /**
     * Get the time since the last successful health check.
     * 
     * @return Duration since last successful check, or null if never successful
     */
    public Duration getTimeSinceLastSuccess() {
        long lastSuccess = lastSuccessfulCheck.get();
        if (lastSuccess == 0) {
            return null;
        }
        return Duration.ofMillis(System.currentTimeMillis() - lastSuccess);
    }
    
    /**
     * Get the number of consecutive failures.
     * 
     * @return Number of consecutive failures
     */
    public long getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * Check if the BOSH Director connection has been healthy recently.
     * 
     * @param maxAge Maximum age for a successful check to be considered recent
     * @return true if the connection was healthy within the specified time
     */
    public boolean isHealthyRecently(Duration maxAge) {
        long lastSuccess = lastSuccessfulCheck.get();
        if (lastSuccess == 0) {
            return false;
        }
        return Duration.ofMillis(System.currentTimeMillis() - lastSuccess).compareTo(maxAge) <= 0;
    }
}
