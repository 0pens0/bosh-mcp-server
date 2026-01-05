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
public class BoshVmService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshVmService.class);

    private static final String LIST_VMS = "List all VMs in a BOSH deployment";
    private static final String GET_VM_STATUS = "Get status and details of VMs in a BOSH deployment";
    private static final String START_VM = "Start a VM in a BOSH deployment";
    private static final String STOP_VM = "Stop a VM in a BOSH deployment";
    private static final String RESTART_VM = "Restart a VM in a BOSH deployment";
    private static final String RECREATE_VM = "Recreate a VM in a BOSH deployment";

    public BoshVmService(BoshCliExecutor cliExecutor,
                         @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                         @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = LIST_VMS)
    public JsonNode listVms(@ToolParam(description = DEPLOYMENT_PARAM) String deploymentName) {
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Listing VMs for deployment: {}", deploymentName);
            JsonNode result = cliExecutor.executeJson("vms -d " + deploymentName);
            logger.info("Retrieved VM list for deployment: {}", deploymentName);
            return result;
        }, "listVms");
    }

    @Tool(description = GET_VM_STATUS)
    public JsonNode getVmStatus(@ToolParam(description = DEPLOYMENT_PARAM) String deploymentName) {
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting VM status for deployment: {}", deploymentName);
            JsonNode result = cliExecutor.executeJson("vms -d " + deploymentName + " --details");
            logger.info("Retrieved VM status for deployment: {}", deploymentName);
            return result;
        }, "getVmStatus");
    }

    @Tool(description = START_VM)
    public void startVm(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name") String instanceGroup,
            @ToolParam(description = "Instance ID (optional)", required = false) String instanceId) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(instanceGroup)) {
            throw new IllegalArgumentException("Instance group is required");
        }
        
        executeWithRetry(() -> {
            String command = "start -d " + deploymentName + " " + instanceGroup;
            if (StringUtils.hasText(instanceId)) {
                command += "/" + instanceId;
            }
            logger.info("Starting VM: {} in deployment: {}", instanceGroup, deploymentName);
            cliExecutor.execute(command);
            logger.info("VM {} started successfully", instanceGroup);
            return null;
        }, "startVm");
    }

    @Tool(description = STOP_VM)
    public void stopVm(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name") String instanceGroup,
            @ToolParam(description = "Instance ID (optional)", required = false) String instanceId) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(instanceGroup)) {
            throw new IllegalArgumentException("Instance group is required");
        }
        
        executeWithRetry(() -> {
            String command = "stop -d " + deploymentName + " " + instanceGroup;
            if (StringUtils.hasText(instanceId)) {
                command += "/" + instanceId;
            }
            logger.info("Stopping VM: {} in deployment: {}", instanceGroup, deploymentName);
            cliExecutor.execute(command);
            logger.info("VM {} stopped successfully", instanceGroup);
            return null;
        }, "stopVm");
    }

    @Tool(description = RESTART_VM)
    public void restartVm(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name") String instanceGroup,
            @ToolParam(description = "Instance ID (optional)", required = false) String instanceId) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(instanceGroup)) {
            throw new IllegalArgumentException("Instance group is required");
        }
        
        executeWithRetry(() -> {
            String command = "restart -d " + deploymentName + " " + instanceGroup;
            if (StringUtils.hasText(instanceId)) {
                command += "/" + instanceId;
            }
            logger.info("Restarting VM: {} in deployment: {}", instanceGroup, deploymentName);
            cliExecutor.execute(command);
            logger.info("VM {} restarted successfully", instanceGroup);
            return null;
        }, "restartVm");
    }

    @Tool(description = RECREATE_VM)
    public void recreateVm(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name") String instanceGroup,
            @ToolParam(description = "Instance ID (optional)", required = false) String instanceId) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(instanceGroup)) {
            throw new IllegalArgumentException("Instance group is required");
        }
        
        executeWithRetry(() -> {
            String command = "recreate -d " + deploymentName + " " + instanceGroup;
            if (StringUtils.hasText(instanceId)) {
                command += "/" + instanceId;
            }
            logger.info("Recreating VM: {} in deployment: {}", instanceGroup, deploymentName);
            cliExecutor.execute(command);
            logger.info("VM {} recreated successfully", instanceGroup);
            return null;
        }, "recreateVm");
    }
}
