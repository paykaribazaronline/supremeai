package org.example.config;

import org.example.service.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

/**
 * Spring Configuration to provide missing beans for legacy components
 * Allows the application to start without all dependencies being fully implemented
 * This is a comprehensive stub configuration providing all legacy services needed for startup
 */
@Configuration
public class LegacyComponentConfiguration {
    
    // ============= Core Services =============
    
    @Bean
    @ConditionalOnMissingBean
    public MemoryManager memoryManager() {
        return new MemoryManager();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public FileOrchestrator fileOrchestrator(MemoryManager memoryManager) {
        return new FileOrchestrator("./projects", memoryManager);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public TemplateManager templateManager(FileOrchestrator fileOrchestrator) {
        return new TemplateManager("./templates", fileOrchestrator);
    }
    
    @Bean
    @ConditionalOnMissingBean
    public RotationManager rotationManager() {
        return new RotationManager();
    }
    
    // ============= Validation & Fixing Services =============
    
    @Bean
    @ConditionalOnMissingBean
    public CodeValidationService codeValidationService() {
        return new CodeValidationService();
    }
    
    @Bean
    @ConditionalOnMissingBean
    public ErrorFixingSuggestor errorFixingSuggestor() {
        return new ErrorFixingSuggestor();
    }
    
    // ============= Logging & Metrics =============
    
    @Bean
    @ConditionalOnMissingBean
    public ExecutionLogManager executionLogManager() {
        return new ExecutionLogManager();
    }
    
    // ============= Consensus & Decision Making =============
    
    @Bean
    @ConditionalOnMissingBean
    public ConsensusEngine consensusEngine() {
        return new ConsensusEngine(0.70); // 70% consensus threshold
    }

    // ============= Safety & Protection =============

    @Bean
    @ConditionalOnMissingBean
    public SafeZoneManager safeZoneManager() {
        return new SafeZoneManager();
    }
}
