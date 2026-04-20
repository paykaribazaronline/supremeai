package com.supremeai.controller;

import com.supremeai.model.UserSimulatorProfile;
import com.supremeai.repository.UserSimulatorProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/simulator")
public class SimulatorController {

    private final UserSimulatorProfileRepository repository;

    @Autowired
    public SimulatorController(UserSimulatorProfileRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/profile/{userId}")
    public Mono<UserSimulatorProfile> getProfile(@PathVariable String userId) {
        return repository.findById(userId)
                .defaultIfEmpty(new UserSimulatorProfile(userId));
    }

    @PostMapping("/profile/{userId}/save")
    public Mono<Map<String, Object>> saveProfile(@PathVariable String userId, @RequestBody UserSimulatorProfile profile) {
        profile.setUserId(userId);
        return repository.save(profile)
                .map(saved -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("status", "success");
                    return response;
                });
    }

    @PostMapping("/admin/set-quota/{userId}")
    public Mono<Map<String, Object>> setQuota(@PathVariable String userId, @RequestParam int quota) {
        return repository.findById(userId)
                .defaultIfEmpty(new UserSimulatorProfile(userId))
                .flatMap(profile -> {
                    profile.setInstallQuota(quota);
                    return repository.save(profile);
                })
                .map(saved -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("newQuota", quota);
                    return response;
                });
    }
}
