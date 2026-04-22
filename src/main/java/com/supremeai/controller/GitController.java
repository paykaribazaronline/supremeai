package com.supremeai.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/git")
@CrossOrigin(origins = "*")
public class GitController {

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(runGitCommand("status", "--short"));
    }

    @GetMapping("/log")
    public ResponseEntity<Map<String, Object>> getLog(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(runGitCommand("log", "--oneline", "-" + limit));
    }

    @GetMapping("/branch")
    public ResponseEntity<Map<String, Object>> getBranch() {
        return ResponseEntity.ok(runGitCommand("branch", "-a"));
    }

    @PostMapping("/commit")
    public ResponseEntity<Map<String, Object>> commit(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Commit message is required"));
        }
        Map<String, Object> addResult = runGitCommand("add", ".");
        if (!(boolean) addResult.getOrDefault("success", false)) {
            return ResponseEntity.status(500).body(addResult);
        }
        return ResponseEntity.ok(runGitCommand("commit", "-m", message));
    }

    @PostMapping("/push")
    public ResponseEntity<Map<String, Object>> push() {
        return ResponseEntity.ok(runGitCommand("push"));
    }

    @PostMapping("/pull")
    public ResponseEntity<Map<String, Object>> pull() {
        return ResponseEntity.ok(runGitCommand("pull"));
    }

    private Map<String, Object> runGitCommand(String... args) {
        List<String> command = new ArrayList<>();
        command.add("git");
        for (String arg : args) {
            command.add(arg);
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new java.io.File("."));
            Process process = pb.start();

            List<String> output = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }

            List<String> errorOutput = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.add(line);
                }
            }

            int exitCode = process.waitFor();
            return Map.of(
                    "success", exitCode == 0,
                    "exitCode", exitCode,
                    "output", output,
                    "errors", errorOutput
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}
