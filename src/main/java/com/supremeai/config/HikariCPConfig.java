package com.supremeai.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;

/**
 * HikariCP configuration for Firestore connection pooling.
 * Optimized for high-concurrency scenarios with maxPoolSize=100, minIdle=20.
 * 
 * This bean is only created when a datasource URL is configured.
 * For Firestore-only deployments, this config is not needed.
 */
@Configuration
public class HikariCPConfig {

    @Value("${spring.datasource.hikari.maximum-pool-size:100}")
    private int maximumPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:20}")
    private int minimumIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:600000}")
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

    @Value("${spring.datasource.driver-class-name:org.h2.Driver}")
    private String driverClassName;

     /**
      * Configure HikariCP DataSource with optimized settings for Firestore.
      *
      * @return configured HikariDataSource or null if no URL provided
      */
     @Bean
     @ConditionalOnProperty(name = "spring.datasource.url", matchIfMissing = false)
     public DataSource hikariDataSource() {
         // If no JDBC URL is provided, don't create the bean (Firestore-only mode)
         if (jdbcUrl == null || jdbcUrl.trim().isEmpty()) {
             return null;
         }
         
         HikariConfig config = new HikariConfig();
         
         // JDBC connection settings
         config.setJdbcUrl(jdbcUrl);
         config.setUsername(username);
         config.setPassword(password);
         config.setDriverClassName(driverClassName);
         
         // Connection pool sizing
         config.setMaximumPoolSize(maximumPoolSize);
         config.setMinimumIdle(minimumIdle);
         
         // Connection timeouts
         config.setConnectionTimeout(connectionTimeout);
         config.setIdleTimeout(idleTimeout);
         config.setMaxLifetime(maxLifetime);
         
         // Leak detection
         config.setLeakDetectionThreshold(leakDetectionThreshold);
         
         // Performance optimizations
         config.setPoolName("SupremeAIHikariPool");
         config.setRegisterMbeans(true);
         
         // Connection testing
         config.setConnectionTestQuery("SELECT 1");
         config.setValidationTimeout(5000);
         
         // Fail fast on startup
         config.setInitializationFailTimeout(1);
         
         return new HikariDataSource(config);
     }
}
