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
public class BoshDeploymentService extends BoshBaseService {

    private static final Logger logger = LoggerFactory.getLogger(BoshDeploymentService.class);

    private static final String LIST_DEPLOYMENTS = "List all BOSH deployments";
    private static final String GET_DEPLOYMENT = "Get detailed information about a BOSH deployment";
    private static final String DEPLOY_DEPLOYMENT = "Deploy a BOSH deployment from a manifest file";
    private static final String DELETE_DEPLOYMENT = "Delete a BOSH deployment";
    private static final String RECREATE_DEPLOYMENT = "Recreate all VMs in a BOSH deployment";
    private static final String UPDATE_DEPLOYMENT = "Update a BOSH deployment configuration";

    public BoshDeploymentService(BoshCliExecutor cliExecutor,
                                @Value("${bosh.retry.maxAttempts:3}") int maxRetries,
                                @Value("${bosh.retry.delay:2}") int retryDelaySeconds) {
        super(cliExecutor, maxRetries, retryDelaySeconds);
    }

    @Tool(description = LIST_DEPLOYMENTS)
    public List<String> listDeployments() {
        return executeWithRetry(() -> {
            logger.info("Listing BOSH deployments");
            JsonNode result = cliExecutor.executeJson("deployments");
            List<String> deployments = new ArrayList<>();
            
            if (result.has("Tables") && result.get("Tables").isArray()) {
                for (JsonNode table : result.get("Tables")) {
                    if (table.has("Rows") && table.get("Rows").isArray()) {
                        for (JsonNode row : table.get("Rows")) {
                            if (row.has("name")) {
                                deployments.add(row.get("name").asText());
                            }
                        }
                    }
                }
            }
            
            logger.info("Found {} deployments", deployments.size());
            return deployments;
        }, "listDeployments");
    }

    @Tool(description = GET_DEPLOYMENT)
    public JsonNode getDeployment(@ToolParam(description = DEPLOYMENT_PARAM) String deploymentName) {
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        return executeWithRetry(() -> {
            logger.info("Getting deployment details for: {}", deploymentName);
            JsonNode result = cliExecutor.executeJson("deployment -d " + deploymentName);
            logger.info("Retrieved deployment details for: {}", deploymentName);
            return result;
        }, "getDeployment");
    }

    @Tool(description = DEPLOY_DEPLOYMENT)
    public void deployDeployment(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Path to the BOSH deployment manifest file") String manifestPath) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(manifestPath)) {
            throw new IllegalArgumentException("Manifest path is required");
        }
        
        executeWithRetry(() -> {
            logger.info("Deploying deployment: {} with manifest: {}", deploymentName, manifestPath);
            cliExecutor.execute("deploy -d " + deploymentName + " " + manifestPath);
            logger.info("Deployment {} deployed successfully", deploymentName);
            return null;
        }, "deployDeployment");
    }

    @Tool(description = DELETE_DEPLOYMENT)
    public void deleteDeployment(@ToolParam(description = DEPLOYMENT_PARAM) String deploymentName) {
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        executeWithRetry(() -> {
            logger.warn("Deleting deployment: {}", deploymentName);
            cliExecutor.execute("delete-deployment -d " + deploymentName + " --force");
            logger.info("Deployment {} deleted successfully", deploymentName);
            return null;
        }, "deleteDeployment");
    }

    @Tool(description = RECREATE_DEPLOYMENT)
    public void recreateDeployment(@ToolParam(description = DEPLOYMENT_PARAM) String deploymentName) {
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        
        executeWithRetry(() -> {
            logger.info("Recreating deployment: {}", deploymentName);
            cliExecutor.execute("recreate -d " + deploymentName);
            logger.info("Deployment {} recreated successfully", deploymentName);
            return null;
        }, "recreateDeployment");
    }

    @Tool(description = UPDATE_DEPLOYMENT)
    public void updateDeployment(
            @ToolParam(description = DEPLOYMENT_PARAM) String deploymentName,
            @ToolParam(description = "Path to the updated BOSH deployment manifest file") String manifestPath) {
        
        if (!StringUtils.hasText(deploymentName)) {
            throw new IllegalArgumentException("Deployment name is required");
        }
        if (!StringUtils.hasText(manifestPath)) {
            throw new IllegalArgumentException("Manifest path is required");
        }
        
        executeWithRetry(() -> {
            logger.info("Updating deployment: {} with manifest: {}", deploymentName, manifestPath);
            cliExecutor.execute("deploy -d " + deploymentName + " " + manifestPath);
            logger.info("Deployment {} updated successfully", deploymentName);
            return null;
        }, "updateDeployment");
    }
}
