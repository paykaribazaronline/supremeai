package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthenticationController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @PostMapping("/firebase-login")
    public Map<String, Object> firebaseLogin(@RequestBody Map<String, String> request, HttpServletRequest httpRequest) {
        String idToken = request.get("idToken");

        try {
            // ১. Firebase Admin SDK দিয়ে টোকেন ভেরিফাই করা
            com.google.firebase.auth.FirebaseToken decodedToken =
                com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken);

            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = (String) decodedToken.getClaims().get("name");
            
            // ২. Firebase Custom Claims থেকে রোল (Role) চেক করা
            // আপনি Firebase Admin SDK বা Console থেকে ইউজারের জন্য {"role": "ADMIN"} সেট করতে পারেন
            Object roleClaim = decodedToken.getClaims().get("role");
            Object adminClaim = decodedToken.getClaims().get("admin");
            
            UserTier tier = UserTier.FREE; // ডিফল্ট টায়ার
            
            if ("ADMIN".equals(roleClaim) || Boolean.TRUE.equals(adminClaim)) {
                tier = UserTier.ADMIN;
            }

            // ৩. ডাটাবেসে ইউজার সিঙ্ক করা
            Optional<User> existingUser = userRepository.findByFirebaseUid(uid);
            User user;
            boolean isNewUser = false;

            if (existingUser.isPresent()) {
                user = existingUser.get();
                user.setTier(tier); // Firebase থেকে আসা রোল অনুযায়ী আপডেট
                user.setLastLoginAt(LocalDateTime.now());
                user.setUpdatedAt(LocalDateTime.now());
            } else {
                user = new User(uid, email, name != null ? name : email.split("@")[0]);
                user.setTier(tier);
                isNewUser = true;
            }

            userRepository.save(user);

            // Log successful login
            ActivityLog log = new ActivityLog(
                "LOGIN_SUCCESS",
                user.getFirebaseUid(),
                "USER",
                "LOW",
                "User '" + user.getDisplayName() + "' logged in successfully.",
                "SUCCESS",
                httpRequest.getRemoteAddr()
            );
            activityLogRepository.save(log);
            
            // ৪. স্প্রিং সিকিউরিটি সেশন তৈরি করা
            establishSession(user, httpRequest);

            // ৫. রেসপন্স পাঠানো
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

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            SecurityContext context = (SecurityContext) session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            if (context != null && context.getAuthentication() != null) {
                String uid = context.getAuthentication().getName();
                userRepository.findByFirebaseUid(uid).ifPresent(user -> {
                    ActivityLog log = new ActivityLog(
                        "LOGOUT_SUCCESS",
                        uid,
                        "USER",
                        "LOW",
                        "User '" + user.getDisplayName() + "' logged out.",
                        "SUCCESS",
                        request.getRemoteAddr()
                    );
                    activityLogRepository.save(log);
                });
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
}
