package org.tanzu.boshpulse.bosh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads BOSH configuration from .env folder.
 * Supports reading from .env/bosh-env.ini (shell export format) and .env/bosh.pem (certificate file).
 */
@Component
public class BoshEnvConfigReader {

    private static final Logger logger = LoggerFactory.getLogger(BoshEnvConfigReader.class);
    
    private static final String ENV_DIR = ".env";
    private static final String ENV_INI_FILE = ENV_DIR + "/bosh-env.ini";
    private static final String CERT_FILE = ENV_DIR + "/bosh.pem";
    
    private Map<String, String> config = new HashMap<>();
    private String certificateContent;
    private boolean initialized = false;

    /**
     * Initialize and read configuration from .env folder.
     * This method is safe to call multiple times (idempotent).
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        logger.info("Reading BOSH configuration from .env folder...");
        
        // Read bosh-env.ini file
        Path envIniPath = Paths.get(ENV_INI_FILE);
        if (Files.exists(envIniPath)) {
            logger.info("Found .env/bosh-env.ini, reading configuration...");
            readEnvIni(envIniPath);
        } else {
            logger.debug(".env/bosh-env.ini not found, skipping...");
        }
        
        // Read certificate file
        Path certPath = Paths.get(CERT_FILE);
        if (Files.exists(certPath)) {
            logger.info("Found .env/bosh.pem, reading certificate...");
            readCertificate(certPath);
        } else {
            logger.debug(".env/bosh.pem not found, skipping...");
        }
        
        initialized = true;
        logger.info("BOSH configuration from .env folder loaded successfully");
    }

    private void readEnvIni(Path envIniPath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(envIniPath.toFile()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse export statements: export KEY=value or export KEY="value"
                if (line.startsWith("export ")) {
                    String content = line.substring(7).trim(); // Remove "export "
                    int equalsIndex = content.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = content.substring(0, equalsIndex).trim();
                        String value = content.substring(equalsIndex + 1).trim();
                        
                        // Remove quotes if present
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        
                        config.put(key, value);
                        logger.debug("Loaded config: {} = {}", key, 
                                   key.contains("SECRET") ? "***" : value);
                    }
                }
            }
        } catch (IOException e) {
            logger.warn("Failed to read .env/bosh-env.ini: {}", e.getMessage());
        }
    }

    private void readCertificate(Path certPath) {
        try {
            certificateContent = Files.readString(certPath).trim();
            logger.info("Certificate loaded from .env/bosh.pem ({} bytes)", certificateContent.length());
        } catch (IOException e) {
            logger.warn("Failed to read .env/bosh.pem: {}", e.getMessage());
        }
    }

    /**
     * Get a configuration value by key.
     * 
     * @param key The configuration key
     * @return The value, or null if not found
     */
    public String getConfig(String key) {
        if (!initialized) {
            initialize();
        }
        return config.get(key);
    }

    /**
     * Get the BOSH Director URL (BOSH_ENVIRONMENT).
     * 
     * @return The director URL, or null if not found
     */
    public String getDirector() {
        return getConfig("BOSH_ENVIRONMENT");
    }

    /**
     * Get the BOSH client name.
     * 
     * @return The client name, or null if not found
     */
    public String getClient() {
        return getConfig("BOSH_CLIENT");
    }

    /**
     * Get the BOSH client secret.
     * 
     * @return The client secret, or null if not found
     */
    public String getClientSecret() {
        return getConfig("BOSH_CLIENT_SECRET");
    }

    /**
     * Get the CA certificate content.
     * If BOSH_CA_CERT in the config points to a file (like "bosh.pem"),
     * this will return the content of that file.
     * Otherwise, returns the certificate content directly.
     * 
     * @return The certificate content, or null if not found
     */
    public String getCaCert() {
        if (!initialized) {
            initialize();
        }
        
        // If certificate content was read from .env/bosh.pem, return it
        if (certificateContent != null && !certificateContent.isEmpty()) {
            return certificateContent;
        }
        
        // Otherwise, check if BOSH_CA_CERT points to a file
        String caCertPath = getConfig("BOSH_CA_CERT");
        if (caCertPath != null && !caCertPath.isEmpty()) {
            // If it's just "bosh.pem", resolve it relative to .env folder
            if (caCertPath.equals("bosh.pem") || caCertPath.endsWith("/bosh.pem")) {
                Path certPath = Paths.get(CERT_FILE);
                if (Files.exists(certPath)) {
                    try {
                        return Files.readString(certPath).trim();
                    } catch (IOException e) {
                        logger.warn("Failed to read certificate from {}: {}", caCertPath, e.getMessage());
                    }
                }
            }
        }
        
        return null;
    }

    /**
     * Check if configuration from .env folder is available.
     * 
     * @return true if .env folder configuration exists, false otherwise
     */
    public boolean isAvailable() {
        Path envIniPath = Paths.get(ENV_INI_FILE);
        return Files.exists(envIniPath);
    }
}
