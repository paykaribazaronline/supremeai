package com.supremeai.controller;

import com.supremeai.model.User;
import com.supremeai.model.UserApi;
import com.supremeai.repository.UserRepository;
import com.supremeai.repository.UserApiRepository;
import com.supremeai.service.ConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserApiController {

    private static final Logger logger = LoggerFactory.getLogger(UserApiController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserApiRepository userApiRepository;

    @Autowired
    private ConfigService configService;

    @PostMapping("/apis")
    public ResponseEntity<?> createApi(@RequestBody Map<String, String> request,
                                      Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        try {
            String apiName = request.get("apiName");
            String description = request.get("description");

            if (apiName == null || description == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid request"));
            }

            User user = userRepository.findByFirebaseUid(userId).block();
            if (user == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

            String apiKey = "sk-" + UUID.randomUUID().toString().replace("-", "");
            UserApi userApi = new UserApi(userId, apiName, apiKey, description, user.getTier(), user.getMonthlyQuota());
            userApiRepository.save(userApi).block();

            return ResponseEntity.ok(Map.of("apiKey", apiKey));

        } catch (Exception e) {
            logger.error("Failed to create API for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/apis")
    public ResponseEntity<?> getUserApis(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        List<UserApi> userApis = userApiRepository.findByUserIdAndIsActive(userId, true).collectList().block();
        return ResponseEntity.ok(Map.of("apis", userApis != null ? userApis : List.of()));
    }

    @DeleteMapping("/apis/{apiId}")
    public ResponseEntity<?> deleteApi(@PathVariable String apiId, Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        UserApi api = userApiRepository.findById(apiId).block();
        if (api != null && api.getUserId().equals(userId)) {
            api.setIsActive(false);
            userApiRepository.save(api).block();
            return ResponseEntity.ok(Map.of("message", "API deleted"));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping("/apis/{apiId}/regenerate-key")
    public ResponseEntity<?> regenerateApiKey(@PathVariable String apiId, Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        UserApi api = userApiRepository.findById(apiId).block();
        if (api != null && api.getUserId().equals(userId)) {
            api.setApiKey("sk-" + UUID.randomUUID().toString().replace("-", ""));
            userApiRepository.save(api).block();
            return ResponseEntity.ok(Map.of("apiKey", api.getApiKey()));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private String getCurrentUserId(Authentication authentication) {
        return authentication != null ? authentication.getName() : "guest";
    }
}
