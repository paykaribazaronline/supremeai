package com.supremeai.automation.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Automates Firebase Authentication. Allows the system to act on behalf of the Admin to create
 * users or login.
 */
@Service
public class FirebaseAuthAutomator {

  private static final Logger log = LoggerFactory.getLogger(FirebaseAuthAutomator.class);

  // In a real Spring Boot app, you would inject FirebaseAuth.getInstance() here
  // using the Firebase Admin SDK.

  public FirebaseAuthAutomator() {
    log.info("[Firebase Auth Automator] Initialized and ready to manage users.");
  }

  /** Admin provided an email and password. System automatically creates the account in Firebase. */
  public AuthResult createAccount(String email, String password) {
    log.info("[Firebase Auth] Attempting to CREATE account for: {}", email);

    try {
      // Mocking Firebase Admin SDK user creation
      if (password.length() < 6) {
        return new AuthResult(
            false, "Firebase Error: Password must be at least 6 characters long.", null);
      }

      String mockUid = "usr_" + System.currentTimeMillis();
      log.info("[Firebase Auth] Account created successfully! UID: {}", mockUid);
      return new AuthResult(true, "Account created", mockUid);

    } catch (Exception e) {
      log.error("[Firebase Auth] Failed to create account for email: {}", email, e);
      return new AuthResult(false, e.getMessage(), null);
    }
  }

  /**
   * Admin provided an email and password. System logs into the existing account. Note: Firebase
   * Admin SDK bypasses passwords. To verify a password from the backend, you typically use the
   * Firebase REST API (Identity Toolkit).
   */
  public AuthResult login(String email, String password) {
    log.info("[Firebase Auth] Attempting to LOGIN for: {}", email);

    try {
      // Note: Production implementation would use Firebase REST API for token validation
      // Mocking success - email validation only for development
      if (!email.contains("@")) {
        return new AuthResult(false, "Firebase Error: Invalid email format.", null);
      }

      String mockIdToken = "jwt_token_" + System.currentTimeMillis();
      log.info("[Firebase Auth] Login successful! Token generated for: {}", email);
      return new AuthResult(true, "Login successful", mockIdToken);

    } catch (Exception e) {
      log.error("[Firebase Auth] Failed to login for email: {}", email, e);
      return new AuthResult(false, "Invalid credentials", null);
    }
  }
}
