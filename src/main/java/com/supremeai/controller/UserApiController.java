package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserApi;
import com.supremeai.model.UserTier;
import com.supremeai.repository.UserRepository;
import com.supremeai.repository.UserApiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserApiRepository userApiRepository;

    @PostMapping("/apis")
    public ResponseEntity<?> createApi(@RequestBody Map<String, String> request,
                                      Authentication authentication) {
        try {
            String userId = getCurrentUserId(authentication);
            String apiName = request.get("apiName");
            String description = request.get("description");

            if (apiName == null || apiName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "API name is required"));
            }

            if (description == null || description.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "Description is required"));
            }

            Optional<User> userOpt = userRepository.findByFirebaseUid(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
            }

            User user = userOpt.get();

            // Check if user has reached max APIs for their tier
            List<UserApi> userApis = userApiRepository.findByUserIdAndIsActive(userId, true);
            int maxApis = getMaxApisForTier(user.getTier());
            if (userApis.size() >= maxApis) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Maximum API limit reached for your tier"));
            }

            // Generate unique API key
            String apiKey;
            do {
                apiKey = "sk-" + UUID.randomUUID().toString().replace("-", "");
            } while (userApiRepository.findByApiKey(apiKey).isPresent());

            UserApi userApi = new UserApi(userId, apiName, apiKey, description,
                                        user.getTier(), user.getMonthlyQuota());
            userApiRepository.save(userApi);

            Map<String, Object> response = new HashMap<>();
            response.put("id", userApi.getId());
            response.put("apiName", userApi.getApiName());
            response.put("apiKey", userApi.getApiKey());
            response.put("description", userApi.getDescription());
            response.put("tier", userApi.getUserTier().toString());
            response.put("monthlyQuota", userApi.getMonthlyQuota());
            response.put("createdAt", userApi.getCreatedAt());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create API: " + e.getMessage()));
        }
    }

    @GetMapping("/apis")
    public ResponseEntity<?> getUserApis(Authentication authentication) {
        try {
            String userId = getCurrentUserId(authentication);
            List<UserApi> userApis = userApiRepository.findByUserIdAndIsActive(userId, true);

            List<Map<String, Object>> response = userApis.stream().map(api -> {
                Map<String, Object> apiMap = new HashMap<>();
                apiMap.put("id", api.getId());
                apiMap.put("apiName", api.getApiName());
                apiMap.put("apiKey", maskApiKey(api.getApiKey()));
                apiMap.put("description", api.getDescription());
                apiMap.put("tier", api.getUserTier().toString());
                apiMap.put("monthlyQuota", api.getMonthlyQuota());
                apiMap.put("currentUsage", api.getCurrentUsage());
                apiMap.put("usagePercent", calculateUsagePercent(api));
                apiMap.put("createdAt", api.getCreatedAt());
                apiMap.put("lastUsedAt", api.getLastUsedAt());
                return apiMap;
            }).toList();

            return ResponseEntity.ok(Map.of("apis", response));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to fetch APIs: " + e.getMessage()));
        }
    }

    @DeleteMapping("/apis/{apiId}")
    public ResponseEntity<?> deleteApi(@PathVariable Long apiId, Authentication authentication) {
        try {
            String userId = getCurrentUserId(authentication);
            Optional<UserApi> apiOpt = userApiRepository.findById(apiId);

            if (apiOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "API not found"));
            }

            UserApi api = apiOpt.get();
            if (!api.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            api.setIsActive(false);
            api.setUpdatedAt(LocalDateTime.now());
            userApiRepository.save(api);

            return ResponseEntity.ok(Map.of("message", "API deleted successfully"));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete API: " + e.getMessage()));
        }
    }

    @PostMapping("/apis/{apiId}/regenerate-key")
    public ResponseEntity<?> regenerateApiKey(@PathVariable Long apiId, Authentication authentication) {
        try {
            String userId = getCurrentUserId(authentication);
            Optional<UserApi> apiOpt = userApiRepository.findById(apiId);

            if (apiOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "API not found"));
            }

            UserApi api = apiOpt.get();
            if (!api.getUserId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Access denied"));
            }

            // Generate new unique API key
            String newApiKey;
            do {
                newApiKey = "sk-" + UUID.randomUUID().toString().replace("-", "");
            } while (userApiRepository.findByApiKey(newApiKey).isPresent());

            api.setApiKey(newApiKey);
            api.setUpdatedAt(LocalDateTime.now());
            userApiRepository.save(api);

            return ResponseEntity.ok(Map.of(
                "apiKey", api.getApiKey(),
                "message", "API key regenerated successfully"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to regenerate API key: " + e.getMessage()));
        }
    }

    private String getCurrentUserId(Authentication authentication) {
        // This would need to be implemented based on your authentication setup
        // For now, assuming Firebase UID is stored in authentication principal
        return authentication.getName();
    }

    private int getMaxApisForTier(UserTier tier) {
        return switch (tier) {
            case FREE -> 1;
            case BASIC -> 3;
            case PRO -> 10;
            case ENTERPRISE -> 50;
            case ADMIN -> 100;
        };
    }

    private String maskApiKey(String apiKey) {
        if (apiKey.length() <= 8) return apiKey;
        return apiKey.substring(0, 8) + "..." + apiKey.substring(apiKey.length() - 4);
    }

    private double calculateUsagePercent(UserApi api) {
        if (api.getMonthlyQuota() == 0) return 0.0;
        return (double) api.getCurrentUsage() / api.getMonthlyQuota() * 100.0;
    }
}