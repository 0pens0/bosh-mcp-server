package org.tanzu.boshpulse.bosh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Validates BOSH configuration on application startup
 */
@Component
public class BoshConfigurationValidator {

    private static final Logger logger = LoggerFactory.getLogger(BoshConfigurationValidator.class);

    private final String director;
    private final String client;
    private final String clientSecret;
    private final String caCert;
    private final String caCertPath;
    private final BoshCliExecutor cliExecutor;

    public BoshConfigurationValidator(
            @Value("${bosh.director:}") String director,
            @Value("${bosh.client:}") String client,
            @Value("${bosh.clientSecret:}") String clientSecret,
            @Value("${bosh.caCert:}") String caCert,
            @Value("${bosh.caCertPath:}") String caCertPath,
            BoshCliExecutor cliExecutor) {
        this.director = director;
        this.client = client;
        this.clientSecret = clientSecret;
        this.caCert = caCert;
        this.caCertPath = caCertPath;
        this.cliExecutor = cliExecutor;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void validateConfiguration() {
        logger.info("Validating BOSH configuration...");
        
        boolean hasErrors = false;
        
        if (!StringUtils.hasText(director)) {
            logger.error("BOSH Director is not configured. Set bosh.director or BOSH_DIRECTOR environment variable.");
            hasErrors = true;
        }
        
        if (!StringUtils.hasText(client)) {
            logger.error("BOSH client is not configured. Set bosh.client or BOSH_CLIENT environment variable.");
            hasErrors = true;
        }
        
        if (!StringUtils.hasText(clientSecret)) {
            logger.error("BOSH client secret is not configured. Set bosh.clientSecret or BOSH_CLIENT_SECRET environment variable.");
            hasErrors = true;
        }
        
        if (!StringUtils.hasText(caCert) && !StringUtils.hasText(caCertPath)) {
            logger.warn("BOSH CA certificate is not configured. Set bosh.caCert or bosh.caCertPath (or BOSH_CA_CERT/BOSH_CA_CERT_PATH environment variables).");
        }
        
        // Check if BOSH CLI is available
        if (!cliExecutor.isCliAvailable()) {
            logger.error("BOSH CLI is not available. Please ensure BOSH CLI is installed and in PATH.");
            hasErrors = true;
        }
        
        if (hasErrors) {
            logger.error("BOSH configuration validation failed. Please check the configuration and restart the application.");
            throw new IllegalStateException("BOSH configuration is incomplete. Check logs for details.");
        }
        
        // Test connectivity
        logger.info("Testing BOSH Director connectivity...");
        if (!cliExecutor.testConnection()) {
            logger.warn("BOSH Director connectivity test failed. The server will start but operations may fail.");
        } else {
            logger.info("BOSH Director connectivity test passed.");
        }
        
        logger.info("BOSH configuration validation passed. Director: {}, Client: {}", 
                   director, client);
    }
}
