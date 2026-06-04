package com.supremeai.controller;

import com.supremeai.dto.valid.UserCreateDTO;
import com.supremeai.model.UserTier;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.UserAccountService;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

/**
 * REST API for user account management.
 *
 * <p>Endpoints: POST /api/accounts/create - Create a single account (ADMIN only) POST
 * /api/accounts/bulk-create - Create accounts from pre-saved Firestore credentials (ADMIN only) GET
 * /api/accounts - List all users (admin only) GET /api/accounts/{uid} - Get user details (ADMIN or
 * self) PUT /api/accounts/{uid}/tier - Update user tier (admin only) PUT
 * /api/accounts/{uid}/deactivate - Deactivate a user (admin only)
 */
@RestController
@RequestMapping("/api/accounts")
@Validated
public class UserAccountController {

  @Autowired private UserAccountService userAccountService;

  /**
   * POST /api/accounts/create - Create a single user account. Body: { "email": "...", "password":
   * "...", "displayName": "...", "tier": "FREE" }
   */
  @PostMapping("/create")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> createAccount(
      @Valid @RequestBody UserCreateDTO body) {
    String email = body.getEmail();
    String password = body.getPassword();
    String displayName = body.getDisplayName();
    String tierStr = body.getTier();

    try {
      UserTier tier = UserTier.valueOf(tierStr.toUpperCase());
      return userAccountService
          .createAccount(email, password, displayName, tier)
          .map(
              user -> {
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("uid", user.getFirebaseUid());
                response.put("email", user.getEmail());
                response.put("displayName", user.getDisplayName());
                response.put("tier", user.getTier().toString());
                return ResponseEntity.ok(ApiResponse.ok(response));
              })
          .onErrorResume(
              e ->
                  Mono.just(
                      ResponseEntity.internalServerError()
                          .body(ApiResponse.error(e.getMessage()))));
    } catch (IllegalArgumentException e) {
      return Mono.just(
          ResponseEntity.badRequest().body(ApiResponse.error("Invalid tier: " + tierStr)));
    }
  }

  /**
   * POST /api/accounts/bulk-create - Create accounts from a Firestore collection containing
   * pre-saved email/password credentials.
   */
  @PostMapping("/bulk-create")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> bulkCreate(
      @RequestBody Map<String, String> body) {
    String collectionName = body.get("collectionName");

    if (collectionName == null || collectionName.isBlank()) {
      return Mono.just(
          ResponseEntity.badRequest().body(ApiResponse.error("collectionName is required")));
    }

    return userAccountService
        .createAccountsFromCollection(collectionName)
        .map(summary -> ResponseEntity.ok(ApiResponse.ok(summary)))
        .onErrorResume(
            ex -> {
              if (ex instanceof IllegalStateException) {
                return Mono.just(
                    ResponseEntity.status(503).body(ApiResponse.error(ex.getMessage())));
              }
              return Mono.just(
                  ResponseEntity.internalServerError().body(ApiResponse.error(ex.getMessage())));
            });
  }

  /** GET /api/accounts - List all registered users (admin only). */
  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<ApiResponse<List<Map<String, Object>>>>> listUsers() {
    return userAccountService
        .listAllUsers()
        .map(
            u -> {
              Map<String, Object> map = new LinkedHashMap<>();
              map.put("uid", u.getFirebaseUid());
              map.put("email", u.getEmail());
              map.put("displayName", u.getDisplayName());
              map.put("tier", u.getTier().toString());
              map.put("isActive", u.getIsActive());
              map.put("currentUsage", u.getCurrentUsage());
              map.put("monthlyQuota", u.fetchMonthlyQuota());
              map.put("createdAt", u.getCreatedAt());
              map.put("lastLoginAt", u.getLastLoginAt());
              return map;
            })
        .collectList()
        .map(list -> ResponseEntity.ok(ApiResponse.ok(list)));
  }

  /** GET /api/accounts/{uid} - Get details for a specific user. */
  @GetMapping("/{uid}")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> getUser(@PathVariable String uid) {
    return ReactiveSecurityContextHolder.getContext()
        .map(SecurityContext::getAuthentication)
        .flatMap(
            auth -> {
              if (auth == null || auth.getName() == null) {
                return Mono.error(
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated"));
              }

              String currentUid = auth.getName();
              boolean isAdmin =
                  auth.getAuthorities().stream()
                      .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

              if (!isAdmin && !currentUid.equals(uid)) {
                return Mono.error(
                    new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Access denied: You can only view your own profile"));
              }

              return userAccountService
                  .getUser(uid)
                  .map(
                      user -> {
                        Map<String, Object> response = new LinkedHashMap<>();
                        response.put("uid", user.getFirebaseUid());
                        response.put("email", user.getEmail());
                        response.put("displayName", user.getDisplayName());
                        response.put("tier", user.getTier().toString());
                        response.put("isActive", user.getIsActive());
                        response.put("currentUsage", user.getCurrentUsage());
                        response.put("monthlyQuota", user.fetchMonthlyQuota());
                        response.put("hasQuotaRemaining", user.checkQuotaRemaining());
                        response.put("createdAt", user.getCreatedAt());
                        response.put("lastLoginAt", user.getLastLoginAt());
                        return ResponseEntity.ok(ApiResponse.ok(response));
                      })
                  .switchIfEmpty(
                      Mono.just(
                          ResponseEntity.status(404).body(ApiResponse.error("User not found"))));
            });
  }

  /** PUT /api/accounts/{uid}/tier - Update a user's tier (admin only). */
  @PutMapping("/{uid}/tier")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> updateTier(
      @PathVariable String uid, @RequestBody Map<String, String> body) {
    String tierStr = body.get("tier");
    if (tierStr == null) {
      return Mono.just(ResponseEntity.badRequest().body(ApiResponse.error("tier is required")));
    }

    try {
      UserTier tier = UserTier.valueOf(tierStr.toUpperCase());
      return userAccountService
          .updateUserTier(uid, tier)
          .map(
              user ->
                  ResponseEntity.ok(
                      ApiResponse.ok(
                          Map.<String, Object>of(
                              "uid", user.getFirebaseUid(),
                              "tier", user.getTier().toString(),
                              "monthlyQuota", user.fetchMonthlyQuota()))))
          .onErrorResume(
              e ->
                  Mono.just(
                      ResponseEntity.internalServerError()
                          .body(ApiResponse.error(e.getMessage()))));
    } catch (IllegalArgumentException e) {
      return Mono.just(
          ResponseEntity.badRequest().body(ApiResponse.error("Invalid tier: " + tierStr)));
    }
  }

  /** PUT /api/accounts/{uid}/deactivate - Deactivate a user account (admin only). */
  @PutMapping("/{uid}/deactivate")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> deactivateUser(
      @PathVariable String uid) {
    return userAccountService
        .deactivateUser(uid)
        .map(
            user ->
                ResponseEntity.ok(
                    ApiResponse.ok(
                        Map.<String, Object>of(
                            "uid", user.getFirebaseUid(),
                            "isActive", user.getIsActive()))))
        .onErrorResume(
            e -> {
              if (e instanceof IllegalArgumentException) {
                return Mono.just(
                    ResponseEntity.status(404).body(ApiResponse.error(e.getMessage())));
              }
              return Mono.just(
                  ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage())));
            });
  }

  /** PUT /api/accounts/{uid}/reactivate - Reactivate a user account (admin only). */
  @PutMapping("/{uid}/reactivate")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> reactivateUser(
      @PathVariable String uid) {
    return userAccountService
        .reactivateUser(uid)
        .map(
            user ->
                ResponseEntity.ok(
                    ApiResponse.ok(
                        Map.<String, Object>of(
                            "uid", user.getFirebaseUid(),
                            "isActive", user.getIsActive()))))
        .onErrorResume(
            e -> {
              if (e instanceof IllegalArgumentException) {
                return Mono.just(
                    ResponseEntity.status(404).body(ApiResponse.error(e.getMessage())));
              }
              return Mono.just(
                  ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage())));
            });
  }

  /** DELETE /api/accounts/{uid} - Permanently delete a user account (admin only). */
  @DeleteMapping("/{uid}")
  @PreAuthorize("hasRole('ADMIN')")
  public Mono<ResponseEntity<ApiResponse<String>>> deleteUser(@PathVariable String uid) {
    return userAccountService
        .deleteUser(uid)
        .thenReturn(ResponseEntity.ok(ApiResponse.ok("User deleted successfully")))
        .onErrorResume(
            e -> {
              if (e instanceof IllegalArgumentException) {
                return Mono.just(
                    ResponseEntity.status(404).body(ApiResponse.error(e.getMessage())));
              }
              return Mono.just(
                  ResponseEntity.internalServerError().body(ApiResponse.error(e.getMessage())));
            });
  }
}
