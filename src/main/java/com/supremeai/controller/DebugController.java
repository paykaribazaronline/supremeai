package com.supremeai.controller;

import com.supremeai.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping("/validate-token")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        try {
            boolean valid = jwtUtil.validateToken(token);
            String username = jwtUtil.getUsername(token);
            String role = jwtUtil.getRole(token);
            return ResponseEntity.ok(Map.of(
                "valid", valid,
                "username", username,
                "role", role
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", e.getClass().getSimpleName(),
                "message", e.getMessage()
            ));
        }
    }
}
