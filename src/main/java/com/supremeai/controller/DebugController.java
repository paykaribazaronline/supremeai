package com.supremeai.controller;

import com.supremeai.security.JwtUtil;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Profile({"dev", "local", "test"})
@Validated
@RestController
@RequestMapping("/api/debug")
@PreAuthorize("hasRole('ADMIN')")
public class DebugController {

    private final JwtUtil jwtUtil;

    public DebugController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestBody ValidateTokenRequest request) {
        try {
            boolean valid = jwtUtil.validateToken(request.token());
            String username = jwtUtil.getUsername(request.token());
            String role = jwtUtil.getRole(request.token());
            return ResponseEntity.ok(Map.of(
                "valid", valid,
                "username", username,
                "role", role
            ));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getClass().getSimpleName(),
                "message", e.getMessage()
            ));
        }
    }

    public record ValidateTokenRequest(@NotBlank String token) {}
}
