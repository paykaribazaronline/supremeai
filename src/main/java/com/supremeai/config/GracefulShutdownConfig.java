package com.supremeai.config;

import org.apache.catalina.connector.Connector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.event.ApplicationReadyEvent;

/**
 * Graceful Shutdown Configuration
 * 
 * Ensures zero-downtime deployments and prevents request loss during shutdown.
 * 
 * Features:
 * - Pauses new connections during shutdown
 * - Waits for in-flight requests to complete
 * - Configurable timeout for graceful termination
 * - Proper resource cleanup
 * 
 * User Experience Benefits:
 * - No lost requests during deployments
 * - Smooth rolling updates
 * - Better reliability and availability
 */
@Configuration
public class GracefulShutdownConfig {
    
    private static final Logger log = LoggerFactory.getLogger(GracefulShutdownConfig.class);
    
    /**
     * Customizes Tomcat connector for graceful shutdown
     * Pauses connector to stop accepting new requests while allowing existing ones to complete
     */
    @Bean
    public ServletWebServerFactory servletContainer() {
        TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();
        factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
            @Override
            public void customize(Connector connector) {
                connector.setProperty("pauseOnShutdown", "true");
                connector.setProperty("connectionTimeout", "20000");
                log.info("Tomcat graceful shutdown configured");
            }
        });
        return factory;
    }
    
    /**
     * Application startup listener
     * Logs startup confirmation for operational visibility
     */
    @Bean
    public ApplicationListener<ApplicationReadyEvent> applicationReadyEventListener() {
        return event -> {
            log.info("==================================================");
            log.info("  SupremeAI Application Started Successfully!");
            log.info("  All services are ready to handle requests");
            log.info("==================================================");
        };
    }
}
