package org.tanzu.boshpulse.bosh;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class BoshStemcellService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshStemcellService.class);

    private static final String LIST_STEMCELLS = "List all available BOSH stemcells";
    private static final String UPLOAD_STEMCELL = "Upload a new BOSH stemcell";
    private static final String DELETE_STEMCELL = "Delete a BOSH stemcell";

    public BoshStemcellService(BoshCliExecutor cliExecutor,
                               @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                               @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = LIST_STEMCELLS)
    public JsonNode listStemcells() {
        return executeWithRetry(() -> {
            logger.info("Listing BOSH stemcells");
            JsonNode result = cliExecutor.executeJson("stemcells");
            logger.info("Retrieved stemcell list");
            return result;
        }, "listStemcells");
    }

    @Tool(description = UPLOAD_STEMCELL)
    public void uploadStemcell(@ToolParam(description = "Path to the stemcell file or URL") String stemcellPath) {
        if (!StringUtils.hasText(stemcellPath)) {
            throw new IllegalArgumentException("Stemcell path is required");
        }
        
        executeWithRetry(() -> {
            logger.info("Uploading stemcell: {}", stemcellPath);
            cliExecutor.execute("upload-stemcell " + stemcellPath);
            logger.info("Stemcell {} uploaded successfully", stemcellPath);
            return null;
        }, "uploadStemcell");
    }

    @Tool(description = DELETE_STEMCELL)
    public void deleteStemcell(
            @ToolParam(description = STEMCELL_PARAM) String stemcellName,
            @ToolParam(description = "Stemcell version", required = false) String version) {
        
        if (!StringUtils.hasText(stemcellName)) {
            throw new IllegalArgumentException("Stemcell name is required");
        }
        
        executeWithRetry(() -> {
            String command = "delete-stemcell " + stemcellName;
            if (StringUtils.hasText(version)) {
                command += "/" + version;
            }
            logger.warn("Deleting stemcell: {}", stemcellName);
            cliExecutor.execute(command + " --force");
            logger.info("Stemcell {} deleted successfully", stemcellName);
            return null;
        }, "deleteStemcell");
    }
}
