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
public class BoshSshService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshSshService.class);

    private static final String SSH_TO_VM = "Get SSH connection information for a VM in a BOSH deployment";
    private static final String EXECUTE_COMMAND_ON_VM = "Execute a command on a VM via SSH";

    public BoshSshService(BoshCliExecutor cliExecutor,
                         @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                         @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = SSH_TO_VM)
    public String sshToVm(
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
            logger.info("Getting SSH info for VM: {} in deployment: {}", instanceGroup, deploymentName);
            String command = "ssh -d " + deploymentName + " " + instanceGroup;
            if (StringUtils.hasText(instanceId)) {
                command += "/" + instanceId;
            }
            command += " --json";
            JsonNode result = cliExecutor.executeJson(command);
            logger.info("Retrieved SSH info for VM: {} in deployment: {}", instanceGroup, deploymentName);
            return result.toString();
        }, "sshToVm");
    }

    @Tool(description = EXECUTE_COMMAND_ON_VM)
    public String executeCommandOnVm(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Instance group/job name") String instanceGroup,
            @ToolParam(description = "Command to execute") String command,
            @ToolParam(description = "Instance ID (optional)", required = false) String instanceId) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(instanceGroup)) {
            throw new IllegalArgumentException("Instance group is required");
        }
        if (!StringUtils.hasText(command)) {
            throw new IllegalArgumentException("Command is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Executing command on VM: {} in deployment: {}", instanceGroup, deploymentName);
            String boshCommand = "ssh -d " + deploymentName + " " + instanceGroup;
            if (StringUtils.hasText(instanceId)) {
                boshCommand += "/" + instanceId;
            }
            boshCommand += " -c '" + command + "'";
            String result = cliExecutor.execute(boshCommand);
            logger.info("Command executed on VM: {} in deployment: {}", instanceGroup, deploymentName);
            return result;
        }, "executeCommandOnVm");
    }
}
