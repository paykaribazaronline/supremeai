package org.example.service;

import org.example.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * Bootstrap Service
 * Creates default user on application startup (one-time only)
 * 
 * Default Credentials:
 * - Username: supremeai
 * - Email: supremeai@admin.com
 * - Password: Admin@123456!
 */
@Service
public class UserBootstrapService {
    private static final Logger logger = LoggerFactory.getLogger(UserBootstrapService.class);
    
    @Autowired
    private AuthenticationService authService;
    
    @PostConstruct
    public void initializeDefaultUser() {
        try {
            // Check if any users exist
            List<User> existingUsers = authService.getAllUsers();
            
            if (existingUsers != null && !existingUsers.isEmpty()) {
                logger.info("✅ System already initialized with {} user(s)", existingUsers.size());
                return;
            }
            
            // Create default admin user
            User defaultUser = authService.registerUser(
                "supremeai",
                "supremeai@admin.com",
                "Admin@123456!"
            );
            
            logger.info("════════════════════════════════════════════════════");
            logger.info("✅ DEFAULT USER CREATED SUCCESSFULLY");
            logger.info("════════════════════════════════════════════════════");
            logger.info("🔑 LOGIN CREDENTIALS:");
            logger.info("   Username: supremeai");
            logger.info("   Email: supremeai@admin.com");
            logger.info("   Password: Admin@123456!");
            logger.info("════════════════════════════════════════════════════");
            logger.info("📱 Access from:");
            logger.info("   Web (Local): http://localhost:8001");
            logger.info("   Web (Live): https://supremeai-565236080752.web.app");
            logger.info("   Flutter App: flutter run");
            logger.info("════════════════════════════════════════════════════");
            logger.info("⚠️  IMPORTANT: Change password after first login!");
            logger.info("════════════════════════════════════════════════════");
            
        } catch (Exception e) {
            logger.error("❌ Failed to create default user: {}", e.getMessage());
        }
    }
}
