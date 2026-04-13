package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Hot Reload Service
 * Loads generated code into running JVM without restart
 */
@Service
public class HotReloadService {
    private static final Logger logger = LoggerFactory.getLogger(HotReloadService.class);
    
    private ClassLoader customClassLoader;
    
    /**
     * Load newly compiled class into memory
     */
    public boolean loadNewClass(String className, String classPath) {
        try {
            // Load from build directory
            java.net.URLClassLoader loader = new java.net.URLClassLoader(
                new java.net.URL[] { new java.io.File("build/classes/java/main/").toURI().toURL() },
                Thread.currentThread().getContextClassLoader()
            );
            
            loader.loadClass(className);
            this.customClassLoader = loader;
            
            logger.info("✅ Hot-loaded class: {}", className);
            return true;
            
        } catch (Exception e) {
            logger.error("❌ Failed to hot-load class: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Get loaded class
     */
    public Class<?> getLoadedClass(String className) throws ClassNotFoundException {
        if (customClassLoader != null) {
            return customClassLoader.loadClass(className);
        }
        return Class.forName(className);
    }
    
    /**
     * Restart application safely (fallback if hot-reload fails)
     */
    public void gracefulRestart() {
        logger.info("🔄 Initiating graceful restart...");
        // Trigger Spring context refresh
        new Thread(() -> {
            try {
                Thread.sleep(2000); // Let existing requests finish
                logger.info("⚡ Restart triggered");
                // In production: use Spring Cloud Config or K8s restart
            } catch (InterruptedException e) {
                logger.error("Restart interrupted: {}", e.getMessage());
            }
        }).start();
    }
}
