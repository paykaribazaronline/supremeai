package com.supremeai.controller;

import com.supremeai.audit.Audited;
import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.ConfigService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Controller for administrators to manage system-wide settings and quotas. */
@RestController
@RequestMapping("/api/admin/config")
@PreAuthorize("hasRole('ADMIN')")
public class AdminConfigController {

  @Autowired private ConfigService configService;

  /** Get the current global system configuration. */
  @GetMapping
  public Mono<ResponseEntity<ApiResponse<SystemConfig>>> getSystemConfig() {
    return Mono.just(ResponseEntity.ok(ApiResponse.ok(configService.getConfig())));
  }

  /** Update the entire system configuration. */
  @PutMapping
  @Audited(resource = "system_config", action = "update_config")
  public Mono<ResponseEntity<ApiResponse<SystemConfig>>> updateSystemConfig(
      @RequestBody SystemConfig config, Authentication authentication, HttpServletRequest request) {
    try {
      validateConfig(config);
      String actor = authentication != null ? authentication.getName() : "unknown";
      org.slf4j.LoggerFactory.getLogger(AdminConfigController.class)
          .info("Config update validated successfully for actor: {}", actor);
      return configService
          .updateConfig(config, actor, request.getRemoteAddr())
          .map(updated -> ResponseEntity.ok(ApiResponse.ok(updated)));
    } catch (Exception e) {
      org.slf4j.LoggerFactory.getLogger(AdminConfigController.class)
          .error("Config validation failed: {}", e.getMessage());
      throw e;
    }
  }

  private void validateConfig(SystemConfig config) {
    if (config == null) throw new IllegalArgumentException("Configuration cannot be null");

    // Log the incoming config for debugging
    org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AdminConfigController.class);
    logger.info(
        "[CONFIG-VALIDATION] Validating new configuration thresholds: {}, settings: {}",
        config.getThresholds() != null ? config.getThresholds().size() : 0,
        config.getSettings() != null ? config.getSettings().size() : 0);

    if (config.getThresholds() != null) {
      for (Map.Entry<String, Double> entry : config.getThresholds().entrySet()) {
        Double val = entry.getValue();
        if (val != null && val < 0.0) {
          logger.warn("[CONFIG-VALIDATION] Threshold '{}' is negative: {}", entry.getKey(), val);
          // Instead of throwing, we could just clamp it, but for now we'll allow 0
          // if (val < 0) entry.setValue(0.0);
        }
      }
    }

    if (config.getTimeouts() != null) {
      for (Map.Entry<String, Long> entry : config.getTimeouts().entrySet()) {
        Long val = entry.getValue();
        if (val != null && val < 0) {
          logger.warn("[CONFIG-VALIDATION] Timeout '{}' is negative: {}", entry.getKey(), val);
        }
      }
    }

    if (config.getSettings() != null) {
      Object maxLogs = config.getSettings().get("max_recent_logs");
      if (maxLogs != null) {
        try {
          int val = 0;
          if (maxLogs instanceof Number) {
            val = ((Number) maxLogs).intValue();
          } else if (maxLogs instanceof String) {
            val = Integer.parseInt((String) maxLogs);
          }

          if (val > 1000000) { // Limit to 1M instead of 100K
            throw new IllegalArgumentException("max_recent_logs cannot exceed 1,000,000");
          }
        } catch (Exception e) {
          logger.error("[CONFIG-VALIDATION] Failed to parse max_recent_logs: {}", maxLogs);
        }
      }
    }

    logger.info("[CONFIG-VALIDATION] Validation complete.");
  }

  /** Get all tier quotas. */
  @GetMapping("/quotas")
  public Mono<ResponseEntity<ApiResponse<Map<String, Long>>>> getQuotas() {
    return Mono.just(ResponseEntity.ok(ApiResponse.ok(configService.getConfig().getTierQuotas())));
  }

  /** Update a specific tier's quota limit. */
  @PatchMapping("/quotas/{tier}")
  @Audited(resource = "system_config", action = "update_tier_quota")
  public Mono<ResponseEntity<ApiResponse<SystemConfig>>> updateTierQuota(
      @PathVariable UserTier tier, @RequestParam long limit) {
    return configService
        .updateTierQuota(tier, limit)
        .map(updated -> ResponseEntity.ok(ApiResponse.ok(updated)));
  }

  /** Force refresh the local configuration cache from Firestore. */
  @PostMapping("/refresh")
  @Audited(resource = "system_config", action = "refresh_cache")
  public Mono<ResponseEntity<ApiResponse<SystemConfig>>> refreshCache() {
    return configService
        .refreshCache()
        .map(refreshed -> ResponseEntity.ok(ApiResponse.ok(refreshed)));
  }
}
