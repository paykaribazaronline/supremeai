package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import com.supremeai.security.BruteForceProtectionService;
import com.supremeai.service.AuthenticationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import com.supremeai.response.ApiResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AuthenticationController - Handles user registration, login, and session management.
 * Refactored for better clarity and separation of concerns.
 */
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthenticationController {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationController.class);

    private final UserRepository userRepository;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final AuthenticationService authenticationService;
public AuthenticationController(
            UserRepository userRepository,
            BruteForceProtectionService bruteForceProtectionService,
            AuthenticationService authenticationService
    ) {
        this.userRepository = userRepository;
        this.bruteForceProtectionService = bruteForceProtectionService;
        this.authenticationService = authenticationService;
    }

    @PostMapping("/firebase-login")
    public Mono<ApiResponse<Map<String, Object>>> firebaseLogin(@Valid @RequestBody FirebaseLoginRequest request, HttpServletRequest httpRequest) {
        String idToken = request.idToken();
        String remoteAddr = httpRequest.getRemoteAddr();
        log.info("Firebase login attempt from {}", remoteAddr);

        return authenticationService.firebaseLogin(idToken, remoteAddr)
            .map(data -> {
                User user = (User) data.get("user");
                establishSession(user, httpRequest);
                
                Map<String, Object> response = new HashMap<>(data);
                response.put("user", convertToPublicUserMap(user));
                return ApiResponse.ok(response);
            })
            .onErrorResume(e -> {
                log.error("Login failed: {}", e.getMessage());
                return Mono.just(ApiResponse.error("Authentication failed: " + e.getMessage()));
            });
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> register(@Valid @RequestBody RegisterRequest request,
                                                           HttpServletRequest httpRequest) {
        String remoteAddr = httpRequest.getRemoteAddr();
        if (bruteForceProtectionService.isLocked(remoteAddr)) {
            return Mono.error(new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Rate limit exceeded"));
        }

        return authenticationService.register(request.email(), request.password(), request.displayName(), remoteAddr)
            .map(savedUser -> ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(Map.of(
                    "message", "Registration successful",
                    "user", convertToPublicUserMap(savedUser)
            ))))
            .onErrorResume(e -> Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage())));
    }

    @PostMapping("/validate-token")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> validateToken(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        return Mono.fromCallable(() -> com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken))
            .flatMap(decodedToken -> userRepository.findByFirebaseUid(decodedToken.getUid()))
            .map(user -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                "valid", true,
                "user", convertToPublicUserMap(user)
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

        return userRepository.findByFirebaseUid(context.getAuthentication().getName())
            .map(user -> ResponseEntity.ok(ApiResponse.ok(Map.of("user", (Object) convertToPublicUserMap(user)))))
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

    private Map<String, Object> convertToPublicUserMap(User user) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("id", user.getFirebaseUid());
        userData.put("email", user.getEmail());
        userData.put("username", user.getDisplayName() != null ? user.getDisplayName() : user.getEmail());
        userData.put("role", user.getTier() == UserTier.ADMIN ? "admin" : "user");
        userData.put("tier", user.getTier().toString());
        return userData;
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