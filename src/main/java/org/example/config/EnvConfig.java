package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Environment Configuration Loader
 * 
 * Reads from .env files in the following order:
 * 1. System environment variables
 * 2. .env file in project root
 * 3. .env.local file (for local overrides)
 * 4. .env.{profile} file (for environment-specific configs)
 * 
 * Usage:
 * String apiKey = EnvConfig.get("FIREBASE_API_KEY");
 * String defaultValue = EnvConfig.get("MISSING_KEY", "default_value");
 */
@Configuration
public class EnvConfig {
    private static final Logger logger = LoggerFactory.getLogger(EnvConfig.class);
    private static final Map<String, String> envVars = new HashMap<>();
    
    static {
        loadEnvFiles();
    }
    
    /**
     * Load .env files in priority order
     */
    private static void loadEnvFiles() {
        String activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        if (activeProfile == null) {
            activeProfile = System.getProperty("spring.profiles.active", "development");
        }
        
        // List of .env files to load in order (later files override earlier ones)
        String[] envFiles = {
            ".env",
            ".env.local",
            ".env." + activeProfile,
            ".env." + activeProfile + ".local"
        };
        
        for (String envFile : envFiles) {
            loadEnvFile(envFile);
        }
        
        logger.info("✅ Environment configuration loaded (profile: {})", activeProfile);
        logLoadedKeys();
    }
    
    /**
     * Load a single .env file
     */
    private static void loadEnvFile(String filePath) {
        File file = new File(filePath);
        
        if (!file.exists()) {
            logger.debug("Environment file not found: {}", filePath);
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();
                
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse KEY=VALUE format
                int equalsIndex = line.indexOf('=');
                if (equalsIndex <= 0) {
                    logger.warn("Invalid line in {}: line {}", filePath, lineNumber);
                    continue;
                }
                
                String key = line.substring(0, equalsIndex).trim();
                String value = line.substring(equalsIndex + 1).trim();
                
                // Remove quotes if present
                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                
                // Store in map
                envVars.put(key, value);
                logger.debug("Loaded env var: {}", key);
            }
            
            logger.info("✅ Loaded environment file: {}", filePath);
        } catch (IOException e) {
            logger.error("Error reading environment file {}: {}", filePath, e.getMessage());
        }
    }
    
    /**
     * Get environment variable (checks system env first, then .env files)
     */
    public static String get(String key) {
        return get(key, null);
    }
    
    /**
     * Get environment variable with default value
     */
    public static String get(String key, String defaultValue) {
        // Priority 1: System environment variable
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Priority 2: Java system property
        value = System.getProperty(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Priority 3: Loaded from .env files
        value = envVars.get(key);
        if (value != null && !value.isEmpty()) {
            return value;
        }
        
        // Priority 4: Default value
        if (defaultValue != null) {
            logger.debug("Using default value for {}", key);
            return defaultValue;
        }
        
        logger.warn("Environment variable not found: {} (returning null)", key);
        return null;
    }
    
    /**
     * Get environment variable as integer
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                logger.error("Invalid integer value for {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get environment variable as long
     */
    public static long getLong(String key, long defaultValue) {
        String value = get(key);
        if (value != null) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                logger.error("Invalid long value for {}: {}", key, value);
            }
        }
        return defaultValue;
    }
    
    /**
     * Get environment variable as boolean
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value != null) {
            return value.equalsIgnoreCase("true") || 
                   value.equalsIgnoreCase("yes") || 
                   value.equalsIgnoreCase("1");
        }
        return defaultValue;
    }
    
    /**
     * Get all loaded environment variables (avoid logging sensitive data!)
     */
    public static Map<String, String> getAll() {
        return new HashMap<>(envVars);
    }
    
    /**
     * Check if environment variable exists
     */
    public static boolean has(String key) {
        return get(key) != null;
    }
    
    /**
     * Log loaded configuration keys (without sensitive values!)
     */
    private static void logLoadedKeys() {
        if (logger.isDebugEnabled() && !envVars.isEmpty()) {
            logger.debug("Loaded {} environment variables", envVars.size());
            
            // List keys without values for security
            StringBuilder keys = new StringBuilder();
            for (String key : envVars.keySet()) {
                if (keys.length() > 0) keys.append(", ");
                keys.append(key);
            }
            logger.debug("Environment keys: {}", keys);
        }
    }
    
    /**
     * Get commonly used configurations
     */
    public static class Firebase {
        public static String getProjectId() {
            return get("FIREBASE_PROJECT_ID", "supremeai-a");
        }
        
        public static String getDatabaseUrl() {
            return get("FIREBASE_DATABASE_URL", 
                "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/");
        }
        
        public static String getApiKey() {
            return get("FIREBASE_API_KEY");
        }
    }
    
    public static class Database {
        public static String getUrl() {
            return get("DATABASE_URL", "jdbc:mysql://localhost:3306/supremeai");
        }
        
        public static String getUsername() {
            return get("DATABASE_USERNAME", "root");
        }
        
        public static String getPassword() {
            return get("DATABASE_PASSWORD");
        }
    }
    
    public static class Security {
        public static String getJwtSecret() {
            String secret = get("JWT_SECRET_KEY");
            if (secret == null || secret.length() < 32) {
                logger.warn("⚠️ JWT_SECRET_KEY is not set or too short (<32 chars)");
                return "default-secret-key-change-in-production"; // Fallback
            }
            return secret;
        }
        
        public static long getJwtExpirationMs() {
            return getLong("JWT_EXPIRATION_MS", 86400000); // 24 hours default
        }
        
        public static String getAdminApiKey() {
            return get("ADMIN_API_KEY");
        }
    }
    
    public static class Logging {
        public static String getLogLevel() {
            return get("LOG_LEVEL", "INFO");
        }
        
        public static String getLogsPath() {
            return get("LOGS_PATH", "./logs/");
        }
    }
    
    public static class AI {
        public static String getOpenAiKey() {
            return get("OPENAI_API_KEY");
        }
        
        public static String getGeminiKey() {
            return get("GOOGLE_GEMINI_API_KEY");
        }
        
        public static String getAnthropicKey() {
            return get("ANTHROPIC_API_KEY");
        }
        
        public static String getDeepSeekKey() {
            return get("DEEPSEEK_API_KEY");
        }
    }
}
