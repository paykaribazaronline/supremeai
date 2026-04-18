package com.supremeai.config;

import com.supremeai.command.CommandExecutor;
import com.supremeai.command.MonitoringCommands;
import com.supremeai.command.ProviderManagementCommands;
import com.supremeai.command.OptimizationCommands;
import com.supremeai.command.DeploymentCommands;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.service.HybridDataCollector;
import com.supremeai.service.BudgetManager;
import com.supremeai.service.QuotaTracker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CommandHubConfig {

    @Bean
    public OptimizationCommands optimizationCommands() {
        return new OptimizationCommands();
    }

    @Bean
    public DeploymentCommands deploymentCommands() {
        return new DeploymentCommands();
    }

    @Bean
    public CommandExecutor commandExecutor(
            HybridDataCollector dataCollector,
            BudgetManager budgetManager,
            QuotaTracker quotaTracker,
            AIProviderFactory providerFactory,
            OptimizationCommands optimizationCommands,
            DeploymentCommands deploymentCommands) {
        
        CommandExecutor executor = new CommandExecutor();
        
        // Register monitoring commands
        MonitoringCommands monitoring = new MonitoringCommands(
            dataCollector, budgetManager, quotaTracker
        );
        executor.register(monitoring.getHealthCheckCommand());
        executor.register(monitoring.getQuotaStatusCommand());
        executor.register(monitoring.getMetricsCommand());
        
        // Register provider management commands
        ProviderManagementCommands providerManagement = new ProviderManagementCommands(providerFactory);
        executor.register(providerManagement.getListProvidersCommand());
        executor.register(providerManagement.getProviderDetailsCommand());
        executor.register(providerManagement.getSetProviderBudgetCommand());

        // Register optimization commands
        executor.register(optimizationCommands.getAutoHealCommand());
        executor.register(optimizationCommands.getAdjustQuotaCommand());
        executor.register(optimizationCommands.getRotateKeysCommand());

        // Register deployment commands
        executor.register(deploymentCommands.getDeployCommand());
        executor.register(deploymentCommands.getRollbackCommand());
        executor.register(deploymentCommands.getDeploymentStatusCommand());

        return executor;
    }
}