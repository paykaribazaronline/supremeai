package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.BruteForceProtectionService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.*;

import com.google.cloud.spring.data.firestore.FirestoreTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private FirestoreTemplate firestoreTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private BruteForceProtectionService bruteForceProtectionService;

    @PostMapping("/firebase-login")
    public Mono<Map<String, Object>> firebaseLogin(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String idToken = request.get("idToken");
        log.info("firebase-login request received from {}", httpRequest.getRemoteAddr());

        if (idToken == null || idToken.trim().isEmpty()) {
            log.error("idToken is null or empty");
            return Mono.just(Map.of("status", "error", "message", "Invalid token: idToken is required"));
        }

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(idToken))
            .flatMap(decodedToken -> {
                String uid = decodedToken.getUid();
                String email = decodedToken.getEmail();
                String name = (String) decodedToken.getClaims().get("name");

                return userRepository.findByFirebaseUid(uid)
                    .defaultIfEmpty(new User()) // Return empty user if not found
                    .flatMap(user -> {
                        boolean isNewUser = (user.getFirebaseUid() == null);
                        UserTier tier = UserTier.FREE;
                        
                        if (!isNewUser) {
                            tier = user.getTier();
                            user.setLastLoginAt(LocalDateTime.now().toString());
                            user.setUpdatedAt(LocalDateTime.now().toString());
                        } else {
                            Object roleClaim = decodedToken.getClaims().get("role");
                            Object adminClaim = decodedToken.getClaims().get("admin");
                            if ("ADMIN".equals(roleClaim) || Boolean.TRUE.equals(adminClaim)) {
                                tier = UserTier.ADMIN;
                            }
                            String displayName = (name != null && !name.trim().isEmpty())
                                ? name.trim()
                                : email.split("@")[0];
                            user.setFirebaseUid(uid);
                            user.setEmail(email);
                            user.setDisplayName(displayName);
                            user.setTier(tier);
                        }

                        return userRepository.save(user)
                            .doOnNext(savedUser -> {
                                ActivityLog logEntry = new ActivityLog(
                                    "LOGIN_SUCCESS", savedUser.getFirebaseUid(), "USER", "LOW",
                                    "User '" + savedUser.getDisplayName() + "' logged in successfully.",
                                    "SUCCESS", httpRequest.getRemoteAddr()
                                );
                                activityLogRepository.save(logEntry).subscribe();
                                establishSession(savedUser, httpRequest);
                            })
                            .map(savedUser -> {
                                Map<String, Object> response = new HashMap<>();
                                response.put("status", "success");
                                response.put("isNewUser", isNewUser);
                                response.put("user", Map.of(
                                    "id", savedUser.getFirebaseUid(),
                                    "username", savedUser.getDisplayName(),
                                    "email", savedUser.getEmail(),
                                    "role", savedUser.getTier() == UserTier.ADMIN ? "admin" : "user",
                                    "tier", savedUser.getTier().toString()
                                ));
                                return (Map<String, Object>) response;
                            });
                    });
            })
            .onErrorResume(e -> {
                log.error("Login failed: {}", e.getMessage());
                return Mono.just(Map.of("status", "error", "message", "Authentication failed: " + e.getMessage()));
            });
    }

    /**
     * Register a new user with email and password via Firebase Authentication.
     */
    @PostMapping("/register")
    public Mono<ResponseEntity<Map<String, Object>>> register(@Valid @RequestBody RegisterRequest request,
                                                         HttpServletRequest httpRequest) {
        String remoteAddr = httpRequest.getRemoteAddr();
        String email = request.email();

        // Check if IP or email is locked due to brute force
        if (bruteForceProtectionService.isLocked(remoteAddr)) {
            long remainingSeconds = bruteForceProtectionService.getRemainingLockTimeSeconds(remoteAddr);
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please try again in " + (remainingSeconds / 60) + " minutes."));
        }
        if (bruteForceProtectionService.isLocked(email)) {
            long remainingSeconds = bruteForceProtectionService.getRemainingLockTimeSeconds(email);
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts for this email. Please try again in " + (remainingSeconds / 60) + " minutes."));
        }

        // Record this attempt for brute force protection (rate limiting)
        bruteForceProtectionService.recordFailedAttempt(remoteAddr);
        bruteForceProtectionService.recordFailedAttempt(email);

        return Mono.fromCallable(() -> {
            // Validate password strength
            if (request.password().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
            }

            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                    .setEmail(request.email())
                    .setPassword(request.password())
                    .setDisplayName(request.displayName() != null ? request.displayName() : request.email().split("@")[0])
                    .setEmailVerified(false)
                    .setDisabled(false);

            return FirebaseAuth.getInstance().createUser(createRequest);
        }).flatMap(userRecord -> {
            User user = new User(userRecord.getUid(), request.email(),
                    request.displayName() != null ? request.displayName() : request.email().split("@")[0]);
            user.setTier(UserTier.FREE);
            user.setCreatedAt(LocalDateTime.now().toString());
            user.setUpdatedAt(LocalDateTime.now().toString());
            
            return userRepository.save(user).doOnNext(savedUser -> {
                ActivityLog log = new ActivityLog(
                        "REGISTER_SUCCESS", userRecord.getUid(), "USER", "LOW",
                        "User '" + savedUser.getDisplayName() + "' registered successfully.",
                        "SUCCESS", httpRequest.getRemoteAddr()
                );
                activityLogRepository.save(log).subscribe();
            }).map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "message", "Registration successful. Please check your email to verify your account.",
                    "user", Map.of(
                            "id", userRecord.getUid(),
                            "email", savedUser.getEmail(),
                            "displayName", savedUser.getDisplayName()
                    )
            )));
        });
    }

    /**
     * Send a password reset email to the user.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody Map<String, String> request,
                                                                 HttpServletRequest httpRequest) {
        String email = request.get("email");
        String remoteAddr = httpRequest.getRemoteAddr();

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        // Check if email or IP is locked
        if (bruteForceProtectionService.isLocked(email)) {
            long remainingSeconds = bruteForceProtectionService.getRemainingLockTimeSeconds(email);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts for this email. Please try again in " + (remainingSeconds / 60) + " minutes.");
        }
        if (bruteForceProtectionService.isLocked(remoteAddr)) {
            long remainingSeconds = bruteForceProtectionService.getRemainingLockTimeSeconds(remoteAddr);
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many attempts. Please try again in " + (remainingSeconds / 60) + " minutes.");
        }

        // Record attempt for rate limiting
        bruteForceProtectionService.recordFailedAttempt(email);
        bruteForceProtectionService.recordFailedAttempt(remoteAddr);

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
    public Mono<ResponseEntity<Map<String, Object>>> validateToken(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        if (idToken == null || idToken.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "idToken is required"));
        }

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(idToken))
            .flatMap(decodedToken -> userRepository.findByFirebaseUid(decodedToken.getUid()))
            .map(user -> {
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
            })
            .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")));
    }

    /**
     * Get the currently authenticated user's profile.
     */
    @GetMapping("/me")
    public Mono<ResponseEntity<Map<String, Object>>> getCurrentUser(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No active session"));
        }

        SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        if (context == null || context.getAuthentication() == null || !context.getAuthentication().isAuthenticated()) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
        }

        String uid = context.getAuthentication().getName();
        return userRepository.findByFirebaseUid(uid)
            .map(user -> ResponseEntity.ok(Map.of(
                "status", "success",
                "user", Map.of(
                        "id", user.getFirebaseUid(),
                        "email", user.getEmail(),
                        "displayName", user.getDisplayName(),
                        "tier", user.getTier().toString(),
                        "role", user.getTier() == UserTier.ADMIN ? "admin" : "user",
                        "lastLoginAt", user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : null
                )
            )))
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));
    }

    @PostMapping("/logout")
    public Mono<Map<String, Object>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context != null && context.getAuthentication() != null) {
                String uid = context.getAuthentication().getName();
                return userRepository.findByFirebaseUid(uid)
                    .doOnNext(user -> {
                        ActivityLog log = new ActivityLog(
                            "LOGOUT_SUCCESS", uid, "USER", "LOW",
                            "User '" + user.getDisplayName() + "' logged out.",
                            "SUCCESS", request.getRemoteAddr()
                        );
                        activityLogRepository.save(log).subscribe();
                    })
                    .then(Mono.fromRunnable(() -> {
                        session.invalidate();
                        SecurityContextHolder.clearContext();
                    }))
                    .thenReturn(Map.of("status", "success"));
            }
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Mono.just(Map.of("status", "success"));
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