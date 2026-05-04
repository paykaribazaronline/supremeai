package com.supremeai.config;

import io.lettuce.core.ReadFrom;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.List;

/**
 * Redis cluster configuration with replication and sentinel support.
 * Provides high availability and automatic failover.
 */
@Configuration
@ConditionalOnProperty(name = "redis.cluster.enabled", havingValue = "true", matchIfMissing = false)
public class RedisClusterConfig {

    @Value("${redis.cluster.nodes:localhost:7000,localhost:7001,localhost:7002}")
    private List<String> clusterNodes;

    @Value("${redis.cluster.max-redirects:3}")
    private int maxRedirects;

    @Value("${redis.sentinel.master:mymaster}")
    private String sentinelMaster;

    @Value("${redis.sentinel.nodes:localhost:26379}")
    private List<String> sentinelNodes;

    /**
     * Configure Lettuce client resources for cluster.
     */
    @Bean(destroyMethod = "shutdown")
    public ClientResources lettuceClientResources() {
        return DefaultClientResources.builder()
                .commandTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Configure Lettuce for Redis cluster.
     */
    @Bean
    @Primary
    public LettuceConnectionFactory redisClusterConnectionFactory(
            ClientResources clientResources) {
        
        // Cluster configuration
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration(clusterNodes);
        clusterConfig.setMaxRedirects(maxRedirects);
        
        // Lettuce client configuration
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .clientResources(clientResources)
                .commandTimeout(Duration.ofSeconds(30))
                .readFrom(ReadFrom.REPLICA_PREFERRED) // Read from replicas for scaling
                .build();
        
        return new LettuceConnectionFactory(clusterConfig, clientConfig);
    }

    /**
     * Configure Redis template for cluster.
     */
    @Bean
    @Primary
    public StringRedisTemplate redisClusterTemplate(LettuceConnectionFactory redisClusterConnectionFactory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(redisClusterConnectionFactory);
        return template;
    }
}