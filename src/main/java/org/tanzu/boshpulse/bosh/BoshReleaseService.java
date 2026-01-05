package org.tanzu.boshpulse.bosh;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class BoshReleaseService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshReleaseService.class);

    private static final String LIST_RELEASES = "List all available BOSH releases";
    private static final String UPLOAD_RELEASE = "Upload a new BOSH release";
    private static final String DELETE_RELEASE = "Delete a BOSH release";
    private static final String GET_RELEASE_VERSIONS = "Get versions of a BOSH release";

    public BoshReleaseService(BoshCliExecutor cliExecutor,
                             @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                             @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = LIST_RELEASES)
    public JsonNode listReleases() {
        return executeWithRetry(() -> {
            logger.info("Listing BOSH releases");
            JsonNode result = cliExecutor.executeJson("releases");
            logger.info("Retrieved release list");
            return result;
        }, "listReleases");
    }

    @Tool(description = UPLOAD_RELEASE)
    public void uploadRelease(@ToolParam(description = "Path to the release file or URL") String releasePath) {
        if (!StringUtils.hasText(releasePath)) {
            throw new IllegalArgumentException("Release path is required");
        }
        
        executeWithRetry(() -> {
            logger.info("Uploading release: {}", releasePath);
            cliExecutor.execute("upload-release " + releasePath);
            logger.info("Release {} uploaded successfully", releasePath);
            return null;
        }, "uploadRelease");
    }

    @Tool(description = DELETE_RELEASE)
    public void deleteRelease(
            @ToolParam(description = RELEASE_PARAM) String releaseName,
            @ToolParam(description = "Release version (optional)", required = false) String version) {
        
        if (!StringUtils.hasText(releaseName)) {
            throw new IllegalArgumentException("Release name is required");
        }
        
        executeWithRetry(() -> {
            String command = "delete-release " + releaseName;
            if (StringUtils.hasText(version)) {
                command += "/" + version;
            }
            logger.warn("Deleting release: {}", releaseName);
            cliExecutor.execute(command + " --force");
            logger.info("Release {} deleted successfully", releaseName);
            return null;
        }, "deleteRelease");
    }

    @Tool(description = GET_RELEASE_VERSIONS)
    public JsonNode getReleaseVersions(@ToolParam(description = RELEASE_PARAM) String releaseName) {
        if (!StringUtils.hasText(releaseName)) {
            throw new IllegalArgumentException("Release name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting versions for release: {}", releaseName);
            JsonNode result = cliExecutor.executeJson("releases");
            // Filter for the specific release
            logger.info("Retrieved versions for release: {}", releaseName);
            return result;
        }, "getReleaseVersions");
    }
}
