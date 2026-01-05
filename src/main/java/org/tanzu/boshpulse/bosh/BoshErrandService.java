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
public class BoshErrandService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshErrandService.class);

    private static final String LIST_ERRANDS = "List all errands for a BOSH deployment";
    private static final String RUN_ERRAND = "Run an errand for a BOSH deployment";
    private static final String GET_ERRAND_STATUS = "Get execution status of an errand";

    public BoshErrandService(BoshCliExecutor cliExecutor,
                            @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                            @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = LIST_ERRANDS)
    public JsonNode listErrands(@ToolParam(description = DEPLOYMENT_PARAM) String deploymentName) {
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Listing errands for deployment: {}", deploymentName);
            JsonNode result = cliExecutor.executeJson("errands -d " + deploymentName);
            logger.info("Retrieved errand list for deployment: {}", deploymentName);
            return result;
        }, "listErrands");
    }

    @Tool(description = RUN_ERRAND)
    public JsonNode runErrand(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Name of the errand to run") String errandName) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(errandName)) {
            throw new IllegalArgumentException("Errand name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Running errand: {} for deployment: {}", errandName, deploymentName);
            JsonNode result = cliExecutor.executeJson("run-errand -d " + deploymentName + " " + errandName);
            logger.info("Errand {} executed successfully for deployment: {}", errandName, deploymentName);
            return result;
        }, "runErrand");
    }

    @Tool(description = GET_ERRAND_STATUS)
    public JsonNode getErrandStatus(@ToolParam(description = TASK_PARAM) String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("Task ID is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting status for errand task: {}", taskId);
            JsonNode result = cliExecutor.executeJson("task " + taskId);
            logger.info("Retrieved status for errand task: {}", taskId);
            return result;
        }, "getErrandStatus");
    }
}
