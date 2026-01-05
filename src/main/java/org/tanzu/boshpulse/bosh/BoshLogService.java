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
public class BoshLogService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshLogService.class);

    private static final String GET_DEPLOYMENT_LOGS = "Get logs from a BOSH deployment";
    private static final String GET_VM_LOGS = "Get logs from a specific VM in a BOSH deployment";
    private static final String GET_TASK_LOGS = "Get logs from a BOSH task";
    private static final String STREAM_LOGS = "Stream logs from a BOSH deployment in real-time";

    public BoshLogService(BoshCliExecutor cliExecutor,
                         @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                         @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = GET_DEPLOYMENT_LOGS)
    public String getDeploymentLogs(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name", required = false) String instanceGroup,
            @ToolParam(description = "Instance ID (optional)", required = false) String instanceId) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting logs for deployment: {}", deploymentName);
            String command = "logs -d " + deploymentName;
            if (StringUtils.hasText(instanceGroup)) {
                command += " " + instanceGroup;
                if (StringUtils.hasText(instanceId)) {
                    command += "/" + instanceId;
                }
            }
            String logs = cliExecutor.execute(command);
            logger.info("Retrieved logs for deployment: {}", deploymentName);
            return logs;
        }, "getDeploymentLogs");
    }

    @Tool(description = GET_VM_LOGS)
    public String getVmLogs(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name") String instanceGroup,
            @ToolParam(description = "Instance ID (optional)", required = false) String instanceId) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(instanceGroup)) {
            throw new IllegalArgumentException("Instance group is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting logs for VM: {} in deployment: {}", instanceGroup, deploymentName);
            String command = "logs -d " + deploymentName + " " + instanceGroup;
            if (StringUtils.hasText(instanceId)) {
                command += "/" + instanceId;
            }
            String logs = cliExecutor.execute(command);
            logger.info("Retrieved logs for VM: {} in deployment: {}", instanceGroup, deploymentName);
            return logs;
        }, "getVmLogs");
    }

    @Tool(description = GET_TASK_LOGS)
    public String getTaskLogs(@ToolParam(description = TASK_PARAM) String taskId) {
        if (!StringUtils.hasText(taskId)) {
            throw new IllegalArgumentException("Task ID is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting logs for task: {}", taskId);
            String logs = cliExecutor.execute("task " + taskId + " --debug");
            logger.info("Retrieved logs for task: {}", taskId);
            return logs;
        }, "getTaskLogs");
    }

    @Tool(description = STREAM_LOGS)
    public String streamLogs(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name (optional)", required = false) String instanceGroup) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Streaming logs for deployment: {}", deploymentName);
            String command = "logs -d " + deploymentName + " --follow";
            if (StringUtils.hasText(instanceGroup)) {
                command += " " + instanceGroup;
            }
            // Note: This will return recent logs, true streaming would require async handling
            String logs = cliExecutor.execute(command);
            logger.info("Retrieved stream logs for deployment: {}", deploymentName);
            return logs;
        }, "streamLogs");
    }
}
