package org.tanzu.boshpulse.bosh;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoshDeploymentServiceTest {

    @Mock
    private BoshCliExecutor cliExecutor;

    private BoshDeploymentService deploymentService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        deploymentService = new BoshDeploymentService(cliExecutor, 3, 2);
        objectMapper = new ObjectMapper();
    }

    @Test
    void testListDeployments_Success() throws Exception {
        // Given: Mock BOSH CLI response with deployments
        String jsonResponse = """
            {
              "Tables": [
                {
                  "Rows": [
                    {"name": "cf-deployment"},
                    {"name": "bosh-dns"},
                    {"name": "uaa"}
                  ]
                }
              ]
            }
            """;
        
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        when(cliExecutor.executeJson("deployments")).thenReturn(jsonNode);

        // When: List deployments
        List<String> deployments = deploymentService.listDeployments();

        // Then: Verify deployments are returned correctly
        assertNotNull(deployments);
        assertEquals(3, deployments.size());
        assertTrue(deployments.contains("cf-deployment"));
        assertTrue(deployments.contains("bosh-dns"));
        assertTrue(deployments.contains("uaa"));
    }

    @Test
    void testListDeployments_EmptyList() throws Exception {
        // Given: Mock BOSH CLI response with no deployments
        String jsonResponse = """
            {
              "Tables": [
                {
                  "Rows": []
                }
              ]
            }
            """;
        
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        when(cliExecutor.executeJson("deployments")).thenReturn(jsonNode);

        // When: List deployments
        List<String> deployments = deploymentService.listDeployments();

        // Then: Verify empty list is returned
        assertNotNull(deployments);
        assertTrue(deployments.isEmpty());
    }

    @Test
    void testListDeployments_NoTables() throws Exception {
        // Given: Mock BOSH CLI response with no Tables field
        String jsonResponse = "{}";
        
        JsonNode jsonNode = objectMapper.readTree(jsonResponse);
        when(cliExecutor.executeJson("deployments")).thenReturn(jsonNode);

        // When: List deployments
        List<String> deployments = deploymentService.listDeployments();

        // Then: Verify empty list is returned
        assertNotNull(deployments);
        assertTrue(deployments.isEmpty());
    }

    @Test
    void testListDeployments_ExceptionHandling() {
        // Given: Mock BOSH CLI to throw exception
        when(cliExecutor.executeJson(anyString()))
            .thenThrow(new RuntimeException("BOSH CLI command failed"));

        // When/Then: Verify exception is propagated
        assertThrows(RuntimeException.class, () -> {
            deploymentService.listDeployments();
        });
    }
}
