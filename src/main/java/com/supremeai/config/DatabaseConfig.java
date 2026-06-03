package com.supremeai.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

/**
 * Optimized database configuration for SupremeAI with connection pooling
 */
@Configuration
@ConditionalOnProperty(name = "spring.datasource.url")
public class DatabaseConfig {
    public DatabaseConfig(String dbUrl, String dbUsername, String dbPassword, int maxPoolSize, int minIdle, long connectionTimeout, long idleTimeout, long maxLifetime, long leakDetectionThreshold) {
        this.dbUrl = dbUrl;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.maxPoolSize = maxPoolSize;
        this.minIdle = minIdle;
        this.connectionTimeout = connectionTimeout;
        this.idleTimeout = idleTimeout;
        this.maxLifetime = maxLifetime;
        this.leakDetectionThreshold = leakDetectionThreshold;
    }











    @Bean
    @Primary
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();

        // Configure connection pool settings
        if (dbUrl != null) {
            dataSource.setJdbcUrl(dbUrl);
        }
        if (dbUsername != null) {
            dataSource.setUsername(dbUsername);
        }
        if (dbPassword != null) {
            dataSource.setPassword(dbPassword);
        }

        // Optimized pool settings for high-performance
        dataSource.setMaximumPoolSize(maxPoolSize);
        dataSource.setMinimumIdle(minIdle);
        dataSource.setConnectionTimeout(connectionTimeout);
        dataSource.setIdleTimeout(idleTimeout);
        dataSource.setMaxLifetime(maxLifetime);
        dataSource.setLeakDetectionThreshold(leakDetectionThreshold);

        // Performance optimizations
        dataSource.addDataSourceProperty("cachePrepStmts", "true");
        dataSource.addDataSourceProperty("prepStmtCacheSize", "250");
        dataSource.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        dataSource.addDataSourceProperty("useServerPrepStmts", "true");
        dataSource.addDataSourceProperty("useLocalSessionState", "true");
        dataSource.addDataSourceProperty("rewriteBatchedStatements", "true");
        dataSource.addDataSourceProperty("cacheResultSetMetadata", "true");
        dataSource.addDataSourceProperty("cacheServerConfiguration", "true");
        dataSource.addDataSourceProperty("elideSetAutoCommits", "true");
        dataSource.addDataSourceProperty("maintainTimeStats", "false");

        // Connection validation
        dataSource.setConnectionTestQuery("SELECT 1");
        dataSource.setValidationTimeout(5000);

        return dataSource;
    }
}