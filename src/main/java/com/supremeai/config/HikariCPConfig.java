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
    public HikariCPConfig(int maximumPoolSize, int minimumIdle, long connectionTimeout, long idleTimeout, long maxLifetime, long leakDetectionThreshold, String jdbcUrl, String username, String password, String driverClassName) {
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
        this.leakDetectionThreshold = leakDetectionThreshold;
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
        this.driverClassName = driverClassName;
    }












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

        // Fail fast on startup
        config.setInitializationFailTimeout(1);

        return new HikariDataSource(config);
    }
}
