package com.supremeai.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables @Scheduled task execution only when app.scheduling.enabled=true (default). In the test
 * profile, set app.scheduling.enabled=false to prevent scheduled tasks (e.g.,
 * AutonomousSkillDiscoveryService) from running and making real HTTP calls.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class SchedulingConfig {}
