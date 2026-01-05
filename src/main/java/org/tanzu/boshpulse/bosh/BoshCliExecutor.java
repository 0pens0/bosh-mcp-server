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
import java.util.ArrayList;
import java.util.HashMap;
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

    public BoshCliExecutor(
            @Value("${bosh.director}") String director,
            @Value("${bosh.client}") String client,
            @Value("${bosh.clientSecret}") String clientSecret,
            @Value("${bosh.caCert:}") String caCert,
            @Value("${bosh.caCertPath:}") String caCertPath,
            @Value("${bosh.cliPath:bosh}") String cliPath,
            @Value("${bosh.connection.timeout:60}") int timeoutSeconds) {
        this.director = director;
        this.client = client;
        this.clientSecret = clientSecret;
        this.cliPath = cliPath;
        this.timeoutSeconds = timeoutSeconds;
        
        // Handle CA certificate - support both file path and content
        String finalCaCertPath = caCertPath;
        if ((caCertPath == null || caCertPath.trim().isEmpty()) && 
            caCert != null && !caCert.trim().isEmpty()) {
            // Write certificate content to temporary file
            try {
                Path tempFile = Files.createTempFile("bosh-ca-cert", ".pem");
                try (FileWriter writer = new FileWriter(tempFile.toFile())) {
                    writer.write(caCert);
                }
                finalCaCertPath = tempFile.toString();
                tempFile.toFile().deleteOnExit();
                logger.debug("Created temporary CA certificate file: {}", finalCaCertPath);
            } catch (IOException e) {
                throw new IllegalStateException("Failed to create temporary CA certificate file", e);
            }
        }
        this.caCertPath = finalCaCertPath;
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
        commandParts.add(cliPath);
        commandParts.add("-e");
        commandParts.add(director);
        
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
        if (caCertPath != null && !caCertPath.trim().isEmpty()) {
            env.put("BOSH_CA_CERT", caCertPath);
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
            ProcessBuilder processBuilder = new ProcessBuilder(cliPath, "--version");
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
