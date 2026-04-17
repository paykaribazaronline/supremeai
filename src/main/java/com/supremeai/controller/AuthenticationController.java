package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/firebase-login")
    public Map<String, Object> firebaseLogin(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String idToken = request.get("idToken");

        try {
            // Verify the token with Firebase Admin SDK
            com.google.firebase.auth.FirebaseToken decodedToken =
                com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = (String) decodedToken.getClaims().get("name");

            // Get or create user in database
            Optional<User> existingUser = userRepository.findByFirebaseUid(uid);
            User user;
            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
            } else {
                user = new User(uid, email, name != null ? name : email.split("@")[0]);
                // Check if this is an admin email (you might want to configure this)
                if (isAdminEmail(email)) {
                    user.setTier(UserTier.ADMIN);
                } else {
                    user.setTier(UserTier.FREE);
                }
                isNewUser = true;
            }

            userRepository.save(user);
            establishSession(user, httpRequest);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("isNewUser", isNewUser);
            // We still provide a token for the app's internal session management if needed,
            // but it's now tied to a verified Firebase identity.
            response.put("token", "fb_verified_" + UUID.randomUUID().toString());
            response.put("refreshToken", "fb_refresh_" + UUID.randomUUID().toString());
            response.put("type", "Bearer");
            response.put("expiresIn", 86400);

            Map<String, Object> userResponse = new HashMap<>();
            userResponse.put("id", user.getFirebaseUid());
            userResponse.put("username", user.getDisplayName());
            userResponse.put("email", user.getEmail());
            userResponse.put("tier", user.getTier().toString());
            userResponse.put("monthlyQuota", user.getMonthlyQuota());
            userResponse.put("role", user.getTier() == UserTier.ADMIN ? "admin" : "user");
            response.put("user", userResponse);

            return response;
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid Firebase token: " + e.getMessage());
            return errorResponse;
        }
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Map.of("status", "success");
    }

    private void establishSession(User user, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority("ROLE_USER"),
            new SimpleGrantedAuthority(user.getTier() == UserTier.ADMIN ? "ROLE_ADMIN" : "ROLE_" + user.getTier().name())
        );
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(user.getFirebaseUid(), null, authorities);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        HttpSession session = request.getSession(true);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    }

    private boolean isAdminEmail(String email) {
        // Configure admin emails - you might want to make this configurable
        return email != null && (
            email.endsWith("@supremeai.com") ||
            email.equals("admin@supremeai.com") ||
            email.equals("niloyjoy7@gmail.com") || // Added from your screenshot
            email.equals("nazifa@example.com")
        );
    }
}
