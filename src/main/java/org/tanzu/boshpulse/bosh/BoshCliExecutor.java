package org.tanzu.boshpulse.bosh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class BoshCliExecutor {

    private static final Logger logger = LoggerFactory.getLogger(BoshCliExecutor.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final String director;
    private final String client;
    private final String clientSecret;
    private final String caCertPath;
    private final String cliPath;
    private final int timeoutSeconds;
    private BoshCliInstaller cliInstaller;
    private final BoshEnvConfigReader envConfigReader;

    public BoshCliExecutor(
            @Value("${bosh.director:}") String director,
            @Value("${bosh.client:}") String client,
            @Value("${bosh.clientSecret:}") String clientSecret,
            @Value("${bosh.caCert:}") String caCert,
            @Value("${bosh.caCertPath:}") String caCertPath,
            @Value("${bosh.cliPath:bosh}") String cliPath,
            @Value("${bosh.connection.timeout:60}") int timeoutSeconds,
            BoshEnvConfigReader envConfigReader) {
        this.envConfigReader = envConfigReader;
        
        // Initialize .env config reader
        envConfigReader.initialize();
        
        // Use environment variables first, fallback to .env folder
        this.director = (director != null && !director.trim().isEmpty()) 
            ? director 
            : envConfigReader.getDirector();
        this.client = (client != null && !client.trim().isEmpty()) 
            ? client 
            : envConfigReader.getClient();
        this.clientSecret = (clientSecret != null && !clientSecret.trim().isEmpty()) 
            ? clientSecret 
            : envConfigReader.getClientSecret();
        this.cliPath = cliPath;
        this.timeoutSeconds = timeoutSeconds;
        
        // Handle CA certificate - support both file path and content
        // Priority: Environment variables > .env folder
        // If BOSH_CA_CERT (content) is provided, use it even if BOSH_CA_CERT_PATH is set
        String finalCaCert = (caCert != null && !caCert.trim().isEmpty()) 
            ? caCert 
            : envConfigReader.getCaCert();
        String finalCaCertPath = caCertPath;
        
        logger.info("Certificate configuration - caCertPath: '{}', caCert provided: {}, .env available: {}", 
                   caCertPath, (finalCaCert != null && !finalCaCert.trim().isEmpty()), 
                   envConfigReader.isAvailable());
        
        // If certificate content is provided, always use it (create temp file)
        // This takes precedence over caCertPath to allow overriding via BOSH_CA_CERT
        if (finalCaCert != null && !finalCaCert.trim().isEmpty()) {
            // Write certificate content to temporary file
            try {
                Path tempFile = Files.createTempFile("bosh-ca-cert", ".pem");
                try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                    writer.write(finalCaCert);
                }
                finalCaCertPath = tempFile.toString();
                tempFile.toFile().deleteOnExit();
                logger.info("Created temporary CA certificate file: {} (size: {} bytes)", 
                           finalCaCertPath, finalCaCert.length());
            } catch (IOException e) {
                logger.error("Failed to create temporary CA certificate file", e);
                throw new IllegalStateException("Failed to create temporary CA certificate file", e);
            }
        } else if (caCertPath != null && !caCertPath.trim().isEmpty()) {
            logger.info("Using provided CA certificate path: {}", caCertPath);
        } else {
            logger.warn("No CA certificate configured (neither caCertPath nor caCert provided)");
        }
        this.caCertPath = finalCaCertPath;
    }
    
    /**
     * Set the CLI installer (injected after construction to avoid circular dependency).
     */
    public void setCliInstaller(BoshCliInstaller cliInstaller) {
        this.cliInstaller = cliInstaller;
    }
    
    /**
     * Get the effective CLI path, using the installed path if available.
     */
    private String getEffectiveCliPath() {
        if (cliInstaller != null) {
            String installedPath = cliInstaller.getResolvedCliPath();
            if (installedPath != null && !installedPath.equals("bosh")) {
                return installedPath;
            }
        }
        return cliPath;
    }

    /**
     * Execute a BOSH CLI command and return JSON output as JsonNode.
     * 
     * @param command The BOSH command (e.g., "deployments", "vms -d deployment-name")
     * @return Parsed JSON response
     * @throws RuntimeException if command fails
     */
    public JsonNode executeJson(String command) {
        String output = execute(command + " --json");
        try {
            return objectMapper.readTree(output);
        } catch (Exception e) {
            logger.error("Failed to parse JSON output: {}", output);
            throw new RuntimeException("Failed to parse BOSH CLI JSON output", e);
        }
    }

    /**
     * Execute a BOSH CLI command and return raw output.
     * 
     * @param command The BOSH command
     * @return Raw command output
     * @throws RuntimeException if command fails
     */
    public String execute(String command) {
        List<String> commandParts = new ArrayList<>();
        commandParts.add(getEffectiveCliPath());
        commandParts.add("-e");
        commandParts.add(director);
        
        // Add CA certificate as command flag if available
        if (caCertPath != null && !caCertPath.trim().isEmpty()) {
            try {
                // Verify the certificate file exists
                Path certFile = Paths.get(caCertPath);
                if (Files.exists(certFile)) {
                    commandParts.add("--ca-cert");
                    commandParts.add(caCertPath);
                    logger.debug("Using CA certificate from file: {}", caCertPath);
                } else {
                    logger.warn("CA certificate file not found: {}", caCertPath);
                }
            } catch (Exception e) {
                logger.warn("Error processing CA certificate path {}: {}", caCertPath, e.getMessage());
            }
        }
        
        // Add command parts
        String[] parts = command.split("\\s+");
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                commandParts.add(part.trim());
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(commandParts);
        
        // Set environment variables for BOSH authentication
        Map<String, String> env = processBuilder.environment();
        env.put("BOSH_ENVIRONMENT", director);
        env.put("BOSH_CLIENT", client);
        env.put("BOSH_CLIENT_SECRET", clientSecret);
        
        // Set BOSH_CA_CERT environment variable if certificate path is available
        if (caCertPath != null && !caCertPath.trim().isEmpty()) {
            try {
                Path certFile = Paths.get(caCertPath);
                if (Files.exists(certFile)) {
                    // BOSH CLI expects BOSH_CA_CERT to be the certificate content
                    String certContent = Files.readString(certFile).trim();
                    env.put("BOSH_CA_CERT", certContent);
                    logger.info("Set BOSH_CA_CERT environment variable from file: {} (content length: {} chars)", caCertPath, certContent.length());
                }
            } catch (Exception e) {
                logger.warn("Failed to read CA certificate from {}: {}", caCertPath, e.getMessage());
            }
        }

        try {
            logger.debug("Executing BOSH command: {}", String.join(" ", commandParts));
            Process process = processBuilder.start();

            // Read output
            StringBuilder output = new StringBuilder();
            StringBuilder errorOutput = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            try (BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = errorReader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                }
            }

            // Wait for process with timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("BOSH CLI command timed out after " + timeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorMsg = errorOutput.length() > 0 ? errorOutput.toString() : output.toString();
                logger.error("BOSH CLI command failed with exit code {}: {}", exitCode, errorMsg);
                throw new RuntimeException("BOSH CLI command failed: " + errorMsg);
            }

            String result = output.toString().trim();
            logger.debug("BOSH CLI command succeeded, output length: {}", result.length());
            return result;

        } catch (IOException e) {
            logger.error("Failed to execute BOSH CLI command", e);
            throw new RuntimeException("Failed to execute BOSH CLI command: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("BOSH CLI command interrupted", e);
            throw new RuntimeException("BOSH CLI command interrupted", e);
        }
    }

    /**
     * Check if BOSH CLI is available.
     * 
     * @return true if BOSH CLI is available, false otherwise
     */
    public boolean isCliAvailable() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(getEffectiveCliPath(), "--version");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            logger.debug("BOSH CLI not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Test connectivity to BOSH Director.
     * 
     * @return true if connection is successful, false otherwise
     */
    public boolean testConnection() {
        try {
            execute("deployments --json");
            return true;
        } catch (Exception e) {
            logger.debug("BOSH Director connection test failed: {}", e.getMessage());
            return false;
        }
    }
}
