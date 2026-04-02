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
 * Ensures auth storage is reachable but does not create hardcoded users.
 */
@Service
public class UserBootstrapService {
    private static final Logger logger = LoggerFactory.getLogger(UserBootstrapService.class);
    
    @Autowired
    private AuthenticationService authService;
    
    @PostConstruct
    public void initializeDefaultUser() {
        try {
            List<User> existingUsers = authService.getAllUsers();
            int existingCount = existingUsers == null ? 0 : existingUsers.size();
            logger.info("✅ User bootstrap check complete. Existing users: {}", existingCount);
            logger.info("ℹ️ No hardcoded default user is created. Authentication follows Firebase Auth users only.");
            
        } catch (Exception e) {
            logger.error("❌ User bootstrap check failed: {}", e.getMessage());
        }
    }
}
