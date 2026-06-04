package com.supremeai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * HikariCP conditional DataSource factory.
 *
 * Pool sizing is read from ${spring.datasource.hikari.*} defined in application.yml
 * — the single source of truth for all HikariCP tuning.  These fallback defaults
 * match application.yml so that HikariCPConfig remains in sync if that file is
 * overridden at runtime without a recompile.
 */
@Configuration
public class HikariCPConfig {

    @Value("${spring.datasource.hikari.maximum-pool-size:200}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:50}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:5000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:1800000}")
    private long maxLifetime;

    @Value("${spring.datasource.hikari.leak-detection-threshold:60000}")
    private long leakDetectionThreshold;

    @Value("${spring.datasource.url:}")
    private String jdbcUrl;

    @Value("${spring.datasource.username:sa}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    @Bean
    @ConditionalOnProperty(name = "spring.datasource.url", matchIfMissing = false)
    public DataSource hikariDataSource() {
        if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
            return null;
        }

        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        if (driverClassName != null && !driverClassName.isBlank()) {
            config.setDriverClassName(driverClassName);
        }

        // Pool sizing — sourced from application.yml (single source of truth)
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);

        // Connection timeouts
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);

        // Leak detection
        config.setLeakDetectionThreshold(leakDetectionThreshold);

        // Pool identity
        config.setPoolName("SupremeAIHikariPool");
        config.setRegisterMbeans(true);

        // Connection test
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);

        // Fail fast on startup (30 seconds timeout to allow H2 file creation/locking to resolve)
        config.setInitializationFailTimeout(30000);

        return new HikariDataSource(config);
    }
}
