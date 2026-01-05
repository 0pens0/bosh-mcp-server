package org.tanzu.boshpulse.bosh;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

/**
 * Installs BOSH CLI binary in the container if not already available.
 * Downloads the binary at application startup and makes it executable.
 */
@Component
public class BoshCliInstaller {

    private static final Logger logger = LoggerFactory.getLogger(BoshCliInstaller.class);
    
    private static final String BOSH_CLI_VERSION = "7.9.5";
    private static final String BOSH_CLI_DOWNLOAD_URL = 
        "https://github.com/cloudfoundry/bosh-cli/releases/download/v" + BOSH_CLI_VERSION + "/bosh-cli-" + BOSH_CLI_VERSION + "-linux-amd64";
    
    @Value("${bosh.cliPath:bosh}")
    private String configuredCliPath;
    
    @Value("${bosh.cli.install.enabled:true}")
    private boolean installEnabled;
    
    @Value("${bosh.cli.install.path:${java.io.tmpdir}/bosh-cli}")
    private String installPath;
    
    private String resolvedCliPath;

    /**
     * Install BOSH CLI at application startup if needed.
     * Runs before BoshConfigurationValidator (Order 1 vs Order 2).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void installBoshCli() {
        logger.info("BoshCliInstaller: Starting BOSH CLI installation check...");
        // If a custom path is configured and it exists, use it
        if (StringUtils.hasText(configuredCliPath) && !configuredCliPath.equals("bosh")) {
            Path cliPath = Paths.get(configuredCliPath);
            if (Files.exists(cliPath) && Files.isExecutable(cliPath)) {
                logger.info("Using configured BOSH CLI at: {}", configuredCliPath);
                resolvedCliPath = configuredCliPath;
                return;
            }
        }
        
        // Check if 'bosh' command is available in PATH
        if (isBoshCliInPath()) {
            logger.info("BOSH CLI found in PATH");
            resolvedCliPath = "bosh";
            return;
        }
        
        // If installation is disabled, don't proceed
        if (!installEnabled) {
            logger.warn("BOSH CLI not found and installation is disabled. Set bosh.cli.install.enabled=true to enable auto-installation.");
            resolvedCliPath = "bosh"; // Will fail at runtime, but allows app to start
            return;
        }
        
        // Install BOSH CLI
        try {
            Path installDir = Paths.get(installPath);
            Path cliBinary = installDir.resolve("bosh");
            
            // Create directory if it doesn't exist
            if (!Files.exists(installDir)) {
                Files.createDirectories(installDir);
                logger.info("Created BOSH CLI installation directory: {}", installDir);
            }
            
            // Check if already installed
            if (Files.exists(cliBinary) && Files.isExecutable(cliBinary)) {
                logger.info("BOSH CLI already installed at: {}", cliBinary);
                resolvedCliPath = cliBinary.toString();
                return;
            }
            
            // Download and install
            logger.info("Installing BOSH CLI v{} to: {}", BOSH_CLI_VERSION, cliBinary);
            downloadBoshCli(cliBinary);
            makeExecutable(cliBinary);
            
            resolvedCliPath = cliBinary.toString();
            logger.info("BOSH CLI successfully installed at: {}", resolvedCliPath);
            
        } catch (Exception e) {
            logger.error("Failed to install BOSH CLI: {}", e.getMessage(), e);
            logger.warn("Falling back to 'bosh' command in PATH. Operations may fail if BOSH CLI is not available.");
            resolvedCliPath = "bosh";
        }
    }
    
    /**
     * Check if BOSH CLI is available in PATH.
     */
    private boolean isBoshCliInPath() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bosh", "--version");
            Process process = processBuilder.start();
            boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            if (finished && process.exitValue() == 0) {
                return true;
            }
        } catch (Exception e) {
            logger.debug("BOSH CLI not found in PATH: {}", e.getMessage());
        }
        return false;
    }
    
    /**
     * Download BOSH CLI binary from GitHub releases.
     */
    private void downloadBoshCli(Path targetPath) throws IOException {
        logger.info("Downloading BOSH CLI from: {}", BOSH_CLI_DOWNLOAD_URL);
        
        URI uri = URI.create(BOSH_CLI_DOWNLOAD_URL);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(60000);
        connection.setRequestMethod("GET");
        
        try (InputStream inputStream = connection.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(targetPath.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            logger.info("Downloaded BOSH CLI: {} bytes", totalBytes);
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Make the BOSH CLI binary executable.
     */
    private void makeExecutable(Path cliBinary) throws IOException {
        try {
            Set<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_READ);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_READ);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            
            Files.setPosixFilePermissions(cliBinary, permissions);
            logger.debug("Set executable permissions on BOSH CLI binary");
        } catch (UnsupportedOperationException e) {
            // Windows or file system doesn't support POSIX permissions
            // Try to make it executable using Java's method
            boolean success = cliBinary.toFile().setExecutable(true, false);
            if (success) {
                logger.debug("Set executable permissions on BOSH CLI binary (non-POSIX)");
            } else {
                logger.warn("Could not set executable permissions on BOSH CLI binary");
            }
        }
    }
    
    /**
     * Get the resolved BOSH CLI path.
     * This will be the installed path, configured path, or "bosh" if in PATH.
     */
    public String getResolvedCliPath() {
        return resolvedCliPath != null ? resolvedCliPath : configuredCliPath;
    }
}
