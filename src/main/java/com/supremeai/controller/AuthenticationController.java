package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @PostMapping("/firebase-login")
    public Map<String, Object> firebaseLogin(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String idToken = request.get("idToken");

        try {
            com.google.firebase.auth.FirebaseToken decodedToken =
                com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = (String) decodedToken.getClaims().get("name");

            // Resolve role: Firestore document takes priority, then Firebase token claims
            UserTier tier = UserTier.FREE;

            User user = userRepository.findByFirebaseUid(uid).block();
            boolean isNewUser = false;

            if (user != null) {
                // Existing user: use their persisted tier from Firestore
                tier = user.getTier();
                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
            } else {
                // New user: check Firebase custom claims for initial role
                Object roleClaim = decodedToken.getClaims().get("role");
                Object adminClaim = decodedToken.getClaims().get("admin");
                if ("ADMIN".equals(roleClaim) || Boolean.TRUE.equals(adminClaim)) {
                    tier = UserTier.ADMIN;
                }
                user = new User(uid, email, name != null ? name : email.split("@")[0]);
                user.setTier(tier);
                isNewUser = true;
            }

            userRepository.save(user).block();

            ActivityLog log = new ActivityLog(
                "LOGIN_SUCCESS",
                user.getFirebaseUid(),
                "USER",
                "LOW",
                "User '" + user.getDisplayName() + "' logged in successfully.",
                "SUCCESS",
                httpRequest.getRemoteAddr()
            );
            activityLogRepository.save(log).subscribe();
            
            establishSession(user, httpRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("isNewUser", isNewUser);
            response.put("user", Map.of(
                "id", user.getFirebaseUid(),
                "username", user.getDisplayName(),
                "email", user.getEmail(),
                "role", user.getTier() == UserTier.ADMIN ? "admin" : "user",
                "tier", user.getTier().toString()
            ));

            return response;
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            return Map.of("status", "error", "message", "Auth failed: " + e.getMessage());
        }
    }

    /**
     * Register a new user with email and password via Firebase Authentication.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request,
                                                        HttpServletRequest httpRequest) {
        try {
            // Validate password strength
            if (request.password().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
            }

            // Check if email already exists
            try {
                UserRecord existing = FirebaseAuth.getInstance().getUserByEmail(request.email());
                if (existing != null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
                }
            } catch (FirebaseAuthException e) {
                if (!e.getMessage().contains("No user record")) {
                    throw e;
                }
                // User doesn't exist - proceed
            }

            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.email())
                    .setPassword(request.password())
                    .setDisplayName(request.displayName() != null ? request.displayName() : request.email().split("@")[0])
                    .setEmailVerified(false)
                    .setDisabled(false);

            UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);

            // Create Firestore user document
            User user = new User(userRecord.getUid(), request.email(),
                    request.displayName() != null ? request.displayName() : request.email().split("@")[0]);
            user.setTier(UserTier.FREE);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user).block();

            ActivityLog log = new ActivityLog(
                    "REGISTER_SUCCESS",
                    userRecord.getUid(),
                    "USER",
                    "LOW",
                    "User '" + user.getDisplayName() + "' registered successfully.",
                    "SUCCESS",
                    httpRequest.getRemoteAddr()
            );
            activityLogRepository.save(log).subscribe();

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "message", "Registration successful. Please check your email to verify your account.",
                    "user", Map.of(
                            "id", userRecord.getUid(),
                            "email", user.getEmail(),
                            "displayName", user.getDisplayName()
                    )
            ));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (FirebaseAuthException e) {
            if (e.getMessage() != null && e.getMessage().contains("EMAIL_EXISTS")) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration failed: " + e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Registration failed: " + e.getMessage());
        }
    }

    /**
     * Send a password reset email to the user.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        try {
            String link = FirebaseAuth.getInstance().generatePasswordResetLink(email);
            // In production, you would send this link via email. Here we log and return success.
            log.info("Password reset link generated for {}", email);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "If this email is registered, a password reset link has been sent."
            ));
        } catch (FirebaseAuthException e) {
            // Always return success to prevent email enumeration
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "If this email is registered, a password reset link has been sent."
            ));
        }
    }

    /**
     * Validate an ID token and return current user information.
     */
    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "idToken is required");
        }

        try {
            com.google.firebase.auth.FirebaseToken decodedToken =
                    FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid = decodedToken.getUid();
            User user = userRepository.findByFirebaseUid(uid).block();

            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
            }

            if (Boolean.TRUE.equals(user.getIsActive()) == false) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is deactivated");
            }

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "valid", true,
                    "user", Map.of(
                            "id", user.getFirebaseUid(),
                            "email", user.getEmail(),
                            "displayName", user.getDisplayName(),
                            "tier", user.getTier().toString(),
                            "role", user.getTier() == UserTier.ADMIN ? "admin" : "user"
                    )
            ));

        } catch (ResponseStatusException e) {
            throw e;
        } catch (FirebaseAuthException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    /**
     * Get the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session");
        }

        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (context == null || context.getAuthentication() == null || !context.getAuthentication().isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String uid = context.getAuthentication().getName();
        User user = userRepository.findByFirebaseUid(uid).block();
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found");
        }

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "user", Map.of(
                        "id", user.getFirebaseUid(),
                        "email", user.getEmail(),
                        "displayName", user.getDisplayName(),
                        "tier", user.getTier().toString(),
                        "role", user.getTier() == UserTier.ADMIN ? "admin" : "user",
                        "lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null
                )
        ));
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context != null && context.getAuthentication() != null) {
                String uid = context.getAuthentication().getName();
                User user = userRepository.findByFirebaseUid(uid).block();
                if (user != null) {
                    ActivityLog log = new ActivityLog(
                        "LOGOUT_SUCCESS",
                        uid,
                        "USER",
                        "LOW",
                        "User '" + user.getDisplayName() + "' logged out.",
                        "SUCCESS",
                        request.getRemoteAddr()
                    );
                    activityLogRepository.save(log).subscribe();
                }
            }
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Map.of("status", "success");
    }

    private void establishSession(User user, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        if (user.getTier() == UserTier.ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(user.getFirebaseUid(), null, authorities);
        
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    /**
     * Request body for user registration.
     */
    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            String displayName
    ) {}
}