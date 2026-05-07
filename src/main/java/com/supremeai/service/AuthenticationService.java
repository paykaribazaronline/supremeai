package com.supremeai.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.supremeai.model.ActivityLog;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.BruteForceProtectionService;
import com.supremeai.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final JwtUtil jwtUtil;
    private final ConfigService configService;

    public Mono<Map<String, Object>> firebaseLogin(String idToken, String remoteAddr) {
        if (idToken == null || idToken.trim().isEmpty()) {
            return Mono.error(new IllegalArgumentException("idToken is required"));
        }

        return Mono.fromCallable(() -> FirebaseAuth.getInstance().verifyIdToken(idToken))
            .flatMap(token -> {
                String uid = token.getUid();
                String email = token.getEmail();
                Map<String, Object> claims = token.getClaims();

                log.info("Processing login for UID: {}, email: {}", uid, email);

                return userRepository.findByFirebaseUid(uid)
                    .defaultIfEmpty(new User())
                    .flatMap(user -> {
                        boolean isNewUser = (user.getFirebaseUid() == null);
                        UserTier tier = UserTier.FREE;

                        if (!isNewUser) {
                            tier = user.getTier();
                            Object roleClaim = claims.get("role");
                            Object adminClaim = claims.get("admin");

                            boolean isAdminEmail = configService.getConfig() != null &&
                                configService.getConfig().getAdminEmails() != null &&
                                configService.getConfig().getAdminEmails().contains(email);

                            boolean shouldBeAdmin = "ADMIN".equals(roleClaim) || Boolean.TRUE.equals(adminClaim)
                                || isAdminEmail;

                            if (tier != UserTier.ADMIN && shouldBeAdmin) {
                                tier = UserTier.ADMIN;
                                user.setTier(tier);
                            }
                            user.setLastLoginAt(LocalDateTime.now().toString());
                            user.setUpdatedAt(LocalDateTime.now().toString());
                        } else {
                            Object roleClaim = claims.get("role");
                            Object adminClaim = claims.get("admin");

                            boolean isAdminEmail = configService.getConfig() != null &&
                                configService.getConfig().getAdminEmails() != null &&
                                configService.getConfig().getAdminEmails().contains(email);

                            boolean shouldBeAdmin = "ADMIN".equals(roleClaim) || Boolean.TRUE.equals(adminClaim)
                                || isAdminEmail;

                            if (shouldBeAdmin) {
                                tier = UserTier.ADMIN;
                            }
                            String displayName = (email != null && email.contains("@"))
                                ? email.split("@")[0]
                                : "User";
                            user.setFirebaseUid(uid);
                            user.setEmail(email);
                            user.setDisplayName(displayName);
                            user.setTier(tier);
                            user.setCreatedAt(LocalDateTime.now().toString());
                            user.setUpdatedAt(LocalDateTime.now().toString());
                        }

                        return userRepository.save(user)
                            .map(savedUser -> {
                                String userRole = savedUser.getTier() == UserTier.ADMIN ? "ADMIN" : "USER";
                                String accessToken = jwtUtil.generateAccessToken(savedUser.getFirebaseUid(), userRole);
                                String refreshToken = jwtUtil.generateRefreshToken(savedUser.getFirebaseUid(), userRole);

                                Map<String, Object> response = new HashMap<>();
                                response.put("status", "success");
                                response.put("token", accessToken);
                                response.put("refreshToken", refreshToken);
                                response.put("isNewUser", isNewUser);
                                response.put("user", savedUser);
                                return response;
                            });
                    });
            });
    }

    public Mono<User> register(String email, String password, String displayName, String remoteAddr) {
        return Mono.fromCallable(() -> {
            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(email)
                .setPassword(password)
                .setDisplayName(displayName != null ? displayName : email.split("@")[0])
                .setEmailVerified(false);

            return FirebaseAuth.getInstance().createUser(createRequest);
        }).flatMap(userRecord -> {
            User user = new User(userRecord.getUid(), email, 
                displayName != null ? displayName : email.split("@")[0]);
            user.setTier(UserTier.FREE);
            user.setCreatedAt(LocalDateTime.now().toString());
            user.setUpdatedAt(LocalDateTime.now().toString());
            
            return userRepository.save(user).doOnNext(savedUser -> {
                logActivity("REGISTER_SUCCESS", userRecord.getUid(), "User registered successfully", remoteAddr);
            });
        });
    }

    public void logActivity(String type, String userId, String message, String remoteAddr) {
        ActivityLog logEntry = new ActivityLog(type, userId, "USER", "LOW", message, "SUCCESS", remoteAddr);
        activityLogRepository.save(logEntry).subscribe();
    }
}
