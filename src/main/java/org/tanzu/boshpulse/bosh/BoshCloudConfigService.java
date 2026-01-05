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
public class BoshCloudConfigService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshCloudConfigService.class);

    private static final String GET_CLOUD_CONFIG = "Get current BOSH cloud config";
    private static final String UPDATE_CLOUD_CONFIG = "Update BOSH cloud config";
    private static final String GET_CLOUD_CONFIG_DIFF = "Get diff of BOSH cloud config changes";

    public BoshCloudConfigService(BoshCliExecutor cliExecutor,
                                 @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                                 @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = GET_CLOUD_CONFIG)
    public String getCloudConfig() {
        return executeWithRetry(() -> {
            logger.info("Getting BOSH cloud config");
            String config = cliExecutor.execute("cloud-config");
            logger.info("Retrieved BOSH cloud config");
            return config;
        }, "getCloudConfig");
    }

    @Tool(description = UPDATE_CLOUD_CONFIG)
    public void updateCloudConfig(@ToolParam(description = "Path to the cloud config file") String configPath) {
        if (!StringUtils.hasText(configPath)) {
            throw new IllegalArgumentException("Cloud config path is required");
        }
        
        executeWithRetry(() -> {
            logger.info("Updating BOSH cloud config from: {}", configPath);
            cliExecutor.execute("update-cloud-config " + configPath);
            logger.info("BOSH cloud config updated successfully");
            return null;
        }, "updateCloudConfig");
    }

    @Tool(description = GET_CLOUD_CONFIG_DIFF)
    public String getCloudConfigDiff(@ToolParam(description = "Path to the new cloud config file") String configPath) {
        if (!StringUtils.hasText(configPath)) {
            throw new IllegalArgumentException("Cloud config path is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting cloud config diff for: {}", configPath);
            String diff = cliExecutor.execute("cloud-config --diff " + configPath);
            logger.info("Retrieved cloud config diff");
            return diff;
        }, "getCloudConfigDiff");
    }
}
