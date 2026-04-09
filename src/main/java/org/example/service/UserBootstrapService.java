package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

/**
 * User Bootstrap Service - FIREBASE ONLY
 * 
 * No longer performs any initialization since authentication is handled entirely by Firebase.
 * Firebase manages user creation, passwords, and accounts automatically.
 * 
 * This class is kept for backward compatibility but performs no operations.
 */
@Service
public class UserBootstrapService {
    private static final Logger logger = LoggerFactory.getLogger(UserBootstrapService.class);
    
    @PostConstruct
    public void initializeDefaultUser() {
        logger.info("User bootstrap initialized. All authentication is handled by Firebase Auth.");
        logger.info("No local user initialization required.");
    }
}
