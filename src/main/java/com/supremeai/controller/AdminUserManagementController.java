package com.supremeai.controller;

import com.supremeai.model.ActivityLog;
import com.supremeai.model.UserTier;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.repository.UserRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.AdminDashboardFacadeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.supremeai.service.UserAccountService;
import com.supremeai.model.User;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * AdminUserManagementController - Handles user-related admin tasks.
 */
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserManagementController extends BaseAdminController<Object, String> {

    private static final Logger log = LoggerFactory.getLogger(AdminUserManagementController.class);

    private final UserRepository userRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AdminDashboardFacadeService facadeService;
    private final UserAccountService userAccountService;

    @Autowired
    public AdminUserManagementController(UserRepository userRepository,
                                          ActivityLogRepository activityLogRepository,
                                          AdminDashboardFacadeService facadeService,
                                          UserAccountService userAccountService) {
        this.userRepository = userRepository;
        this.activityLogRepository = activityLogRepository;
        this.facadeService = facadeService;
        this.userAccountService = userAccountService;
    }

    @GetMapping
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getUsers() {
        return wrapList(
                userRepository.findAll().map(facadeService::toUserMap),
                "users"
        );
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getUser(@PathVariable String userId) {
        return userAccountService.getUser(userId)
                .map(u -> ResponseEntity.ok(ApiResponse.ok(facadeService.toUserMap(u))))
                .switchIfEmpty(Mono.just(ResponseEntity.status(404).body(ApiResponse.error("User not found"))));
    }

    @PostMapping("/create")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> createAccount(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String displayName = body.get("displayName");
        String tierStr = body.getOrDefault("tier", "FREE");

        try {
            UserTier tier = UserTier.valueOf(tierStr.toUpperCase());
            return userAccountService.createAccount(email, password, displayName, tier)
                    .map(user -> ResponseEntity.ok(ApiResponse.ok(facadeService.toUserMap(user))))
                    .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()))));
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("Invalid tier: " + tierStr)));
        }
    }

    @PostMapping("/bulk-create")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> bulkCreate(@RequestBody Map<String, String> body) {
        String collectionName = body.get("collectionName");
        if (collectionName == null || collectionName.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("collectionName is required")));
        }
        return userAccountService.createAccountsFromCollection(collectionName)
                .map(summary -> ResponseEntity.ok(ApiResponse.ok(summary)))
                .onErrorResume(ex -> Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error(ex.getMessage()))));
    }

    @PutMapping("/{userId}/tier")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> updateUserTier(@PathVariable String userId,
                                                                                 @RequestBody Map<String, String> body) {
        String tierStr = body.get("tier");
        if (tierStr == null) return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("tier is required")));
        
        try {
            UserTier newTier = UserTier.valueOf(tierStr.toUpperCase());
            return userAccountService.updateUserTier(userId, newTier)
                    .map(user -> ResponseEntity.ok(ApiResponse.ok(Map.of(
                            "message", "User tier updated",
                            "user", facadeService.toUserMap(user)
                    ))))
                    .onErrorResume(e -> Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage()))));
        } catch (IllegalArgumentException e) {
            return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("Invalid tier: " + tierStr)));
        }
    }

    @PutMapping("/{userId}/deactivate")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> deactivateUser(@PathVariable String userId) {
        return userAccountService.deactivateUser(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.ok(facadeService.toUserMap(user))))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()))));
    }

    @PutMapping("/{userId}/reactivate")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> reactivateUser(@PathVariable String userId) {
        return userAccountService.reactivateUser(userId)
                .map(user -> ResponseEntity.ok(ApiResponse.ok(facadeService.toUserMap(user))))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()))));
    }

    @DeleteMapping("/{userId}")
    public Mono<ResponseEntity<ApiResponse<String>>> deleteUser(@PathVariable String userId) {
        return userAccountService.deleteUser(userId)
                .thenReturn(ResponseEntity.ok(ApiResponse.ok("User deleted successfully")))
                .onErrorResume(e -> Mono.just(ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()))));
    }

    @GetMapping("/tiers")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getAvailableTiers() {
        return Mono.fromCallable(() -> {
            List<Map<String, Object>> tiers = java.util.stream.Stream.of(UserTier.values())
                    .map(tier -> {
                        Map<String, Object> tierMap = new HashMap<>();
                        tierMap.put("name", tier.name());
                        tierMap.put("displayName", tier.name().charAt(0) + tier.name().substring(1).toLowerCase());
                        tierMap.put("monthlyQuota", tier.getDefaultMonthlyQuota());
                        tierMap.put("description", tier.getDescription());
                        tierMap.put("hasUnlimitedQuota", tier.hasUnlimitedQuota());
                        return tierMap;
                    }).toList();
            return ResponseEntity.ok(ApiResponse.ok(Map.<String, Object>of("tiers", tiers)));
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic());
    }
}
