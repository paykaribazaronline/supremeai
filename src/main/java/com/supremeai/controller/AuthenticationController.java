package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.BruteForceProtectionService;
import com.supremeai.security.JwtUtil;
import com.supremeai.service.AuthenticationService;
import com.supremeai.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import com.supremeai.response.ApiResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private Environment env;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    @Autowired
    private BruteForceProtectionService bruteForceProtectionService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ConfigService configService;

    @Autowired
    private AuthenticationService authenticationService;

    @PostMapping("/firebase-login")
    public Mono<ApiResponse<Map<String, Object>>> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request, HttpServletRequest httpRequest) {
         String idToken = request.idToken();
         String remoteAddr = httpRequest.getRemoteAddr();
         log.info("firebase-login request received from {}", remoteAddr);

         return authenticationService.firebaseLogin(idToken, remoteAddr)
             .map(data -> {
                 User user = (User) data.get("user");
                 establishSession(user, httpRequest);
                 
                 Map<String, Object> response = new HashMap<>(data);
                 response.put("user", Map.of(
                     "id", user.getFirebaseUid(),
                     "email", user.getEmail(),
                     "username", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail(),
                     "role", user.getTier() == UserTier.ADMIN ? "admin" : "user",
                     "tier", user.getTier().toString()
                 ));
                 return ApiResponse.ok(response);
             })
            .onErrorResume(e -> {
                log.error("Login failed for request from {}: {}", remoteAddr, e.getMessage(), e);
                String cause = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                return Mono.just(ApiResponse.error("Authentication failed: " + cause));
            });
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> register(@Valid @RequestBody RegisterRequest request,
                                                          HttpServletRequest httpRequest) {
        String remoteAddr = httpRequest.getRemoteAddr();
        String email = request.email();

        if (bruteForceProtectionService.isLocked(remoteAddr)) {
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts. Please try again later."));
        }

        return authenticationService.register(email, request.password(), request.displayName(), remoteAddr)
            .map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of(
                    "message", "Registration successful. Please check your email to verify your account.",
                    "user", (Object) Map.of(
                            "id", savedUser.getFirebaseUid(),
                            "email", savedUser.getEmail(),
                            "displayName", savedUser.getDisplayName()
                    )
            ))))
            .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody Map<String, String> request,
                                                                   HttpServletRequest httpRequest) {
        String email = request.get("email");
        String remoteAddr = httpRequest.getRemoteAddr();

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (bruteForceProtectionService.isLocked(email) || bruteForceProtectionService.isLocked(remoteAddr)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many attempts.");
        }

        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().generatePasswordResetLink(email);
            return ResponseEntity.ok(ApiResponse.ok("If this email is registered, a password reset link has been sent."));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.ok("If this email is registered, a password reset link has been sent."));
        }
    }

    @PostMapping("/validate-token")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> validateToken(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        return Mono.fromCallable(() -> com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken))
            .flatMap(decodedToken -> userRepository.findByFirebaseUid(decodedToken.getUid()))
            .map(user -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                "valid", true,
                "user", Map.of(
                    "id", user.getFirebaseUid(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName(),
                    "tier", user.getTier().toString(),
                    "role", user.getTier() == UserTier.ADMIN ? "admin" : "user"
                )
            ))))
            .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token")));
    }

    @GetMapping("/me")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getCurrentUser(HttpServletRequest request) {
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
            .map(user -> {
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", user.getFirebaseUid());
                userData.put("email", user.getEmail());
                userData.put("displayName", user.getDisplayName());
                userData.put("tier", user.getTier().toString());
                userData.put("role", user.getTier() == UserTier.ADMIN ? "admin" : "user");
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("user", userData);
                
                return ResponseEntity.ok(ApiResponse.ok(responseData));
            })
            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found")));
    }

    @PostMapping("/logout")
    public Mono<ApiResponse<String>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return Mono.just(ApiResponse.ok("Logged out"));
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

    public record RegisterRequest(
            @NotBlank @Email String email,
            @NotBlank @Size(min = 8) String password,
            String displayName
    ) {}

    public record FirebaseLoginRequest(
            @NotBlank String idToken
    ) {}
}