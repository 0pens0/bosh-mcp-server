package org.tanzu.boshpulse;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tanzu.boshpulse.bosh.*;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public List<ToolCallback> registerTools(
            BoshDeploymentService boshDeploymentService,
            BoshVmService boshVmService,
            BoshLogService boshLogService,
            BoshStemcellService boshStemcellService,
            BoshReleaseService boshReleaseService,
            BoshErrandService boshErrandService,
            BoshCloudConfigService boshCloudConfigService,
            BoshSshService boshSshService) {

        return List.of(ToolCallbacks.from(
                boshDeploymentService,
                boshVmService,
                boshLogService,
                boshStemcellService,
                boshReleaseService,
                boshErrandService,
                boshCloudConfigService,
                boshSshService));
    }
    
    /**
     * Wire the CLI installer to the executor after both are created.
     */
    @Bean
    public BoshCliExecutorConfigurer boshCliExecutorConfigurer(
            BoshCliExecutor cliExecutor,
            BoshCliInstaller cliInstaller) {
        return new BoshCliExecutorConfigurer(cliExecutor, cliInstaller);
    }
    
    /**
     * Helper class to configure the executor with the installer.
     */
    private static class BoshCliExecutorConfigurer {
        public BoshCliExecutorConfigurer(BoshCliExecutor executor, BoshCliInstaller installer) {
            executor.setCliInstaller(installer);
        }
    }
}
