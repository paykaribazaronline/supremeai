package com.supremeai.controller.browser;

import com.supremeai.model.browser.*;
import com.supremeai.service.browser.BrowserService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/browser")
@PreAuthorize("hasRole('ADMIN')")
public class BrowserController {

  private final BrowserService browserService;

  public BrowserController(BrowserService browserService) {
    this.browserService = browserService;
  }

  @GetMapping("/surf/status")
  public Mono<Map<String, Object>> getStatus() {
    return browserService.getStatus();
  }

  @PostMapping("/surf/start")
  public Mono<ResponseEntity<Void>> startSurf() {
    return browserService.startBrowsing().then(Mono.just(ResponseEntity.ok().build()));
  }

  @PostMapping("/surf/stop")
  public Mono<ResponseEntity<Void>> stopSurf() {
    return browserService.stopBrowsing().then(Mono.just(ResponseEntity.ok().build()));
  }

  @GetMapping("/activity/recent")
  public Mono<Map<String, Object>> getRecentActivity() {
    return browserService
        .getRecentActivity()
        .collectList()
        .map(activities -> Map.of("activities", activities));
  }

  @GetMapping("/credentials")
  public Mono<Map<String, Object>> getCredentials(
      @RequestParam(defaultValue = "default") String userId) {
    return browserService
        .getAllCredentials(userId)
        .collectList()
        .map(credentials -> Map.of("credentials", credentials));
  }

  @PostMapping("/credentials")
  public Mono<StoredCredential> saveCredential(@RequestBody StoredCredential credential) {
    return browserService.saveCredential(credential);
  }

  @PostMapping("/surf/resume")
  public Mono<ResponseEntity<Void>> resume(@RequestBody Map<String, String> body) {
    return browserService
        .resumeActivity(body.get("activityId"))
        .then(Mono.just(ResponseEntity.ok().build()));
  }

  @PostMapping("/surf/skip-auth")
  public Mono<ResponseEntity<Void>> skipAuth(@RequestBody Map<String, String> body) {
    return browserService
        .skipAuth(body.get("activityId"))
        .then(Mono.just(ResponseEntity.ok().build()));
  }

  @PostMapping("/surf/pause-manual")
  public Mono<ResponseEntity<Void>> pauseForManual(@RequestBody Map<String, String> body) {
    return browserService
        .pauseForManualCredential(body.get("activityId"))
        .then(Mono.just(ResponseEntity.ok().build()));
  }

  @GetMapping("/surf/paused-state")
  public Mono<Map<String, Object>> getPausedState() {
    return browserService.getPausedState();
  }

  @GetMapping("/urls/allowed")
  public Mono<Map<String, Object>> getAllowedUrls(
      @RequestParam(defaultValue = "default") String userId) {
    return browserService.getAllowedUrls(userId).collectList().map(urls -> Map.of("urls", urls));
  }

  @GetMapping("/urls/denied")
  public Mono<Map<String, Object>> getDeniedUrls(
      @RequestParam(defaultValue = "default") String userId) {
    return browserService.getDeniedUrls(userId).collectList().map(urls -> Map.of("urls", urls));
  }

  @PostMapping("/urls/allowed")
  public Mono<UrlPermission> addAllowedUrl(@RequestBody UrlPermission permission) {
    permission.setType("allowed");
    return browserService.addUrlPermission(permission);
  }

  @PostMapping("/urls/denied")
  public Mono<UrlPermission> addDeniedUrl(@RequestBody UrlPermission permission) {
    permission.setType("denied");
    return browserService.addUrlPermission(permission);
  }

  @PostMapping("/urls/allowAll")
  public Mono<UrlPermission> enableAllowAll(@RequestParam(defaultValue = "default") String userId) {
    UrlPermission p = new UrlPermission("*", "*", "allowAll");
    p.setUserId(userId);
    p.setReason("Allow all URLs for user");
    return browserService.addUrlPermission(p);
  }

  @PutMapping("/urls/{id}")
  public Mono<UrlPermission> updateUrl(
      @PathVariable String id, @RequestBody UrlPermission permission) {
    return browserService.updateUrlPermission(id, permission);
  }

  @DeleteMapping("/urls/{id}")
  public Mono<ResponseEntity<Void>> deleteUrl(@PathVariable String id) {
    return browserService.deleteUrlPermission(id).then(Mono.just(ResponseEntity.ok().build()));
  }

  @GetMapping("/urls/requests")
  public Mono<Map<String, Object>> getRequests() {
    return browserService
        .getPermissionRequests()
        .collectList()
        .map(requests -> Map.of("requests", requests));
  }

  @PostMapping("/urls/requests/{id}/decision")
  public Mono<ResponseEntity<Void>> decision(
      @PathVariable String id, @RequestBody Map<String, Boolean> body) {
    return browserService
        .processPermissionDecision(id, body.get("approved"))
        .then(Mono.just(ResponseEntity.ok().build()));
  }

  @GetMapping("/system-learning")
  public Mono<Map<String, Object>> getSystemLearning() {
    return browserService.getSystemLearningStatus();
  }

  @PostMapping("/system-learning/toggle")
  public Mono<ResponseEntity<Void>> toggleLearning(@RequestBody Map<String, Boolean> body) {
    return browserService
        .toggleAutoLearn(body.get("enabled"))
        .then(Mono.just(ResponseEntity.ok().build()));
  }

  @GetMapping("/tasks")
  public Mono<Map<String, Object>> getTasks() {
    return browserService.getActiveTasks().collectList().map(tasks -> Map.of("tasks", tasks));
  }

  @PostMapping("/tasks")
  public Mono<BrowserTask> createTask(@RequestBody Map<String, String> body) {
    return browserService.createActivityTask(body.get("goal"));
  }

  @DeleteMapping("/tasks/{id}")
  public Mono<ResponseEntity<Void>> deleteTask(@PathVariable String id) {
    return browserService.deleteTask(id).then(Mono.just(ResponseEntity.ok().build()));
  }

  @DeleteMapping("/credentials/{id}")
  public Mono<ResponseEntity<Void>> deleteCredential(@PathVariable String id) {
    return browserService.deleteCredential(id).then(Mono.just(ResponseEntity.ok().build()));
  }

  @GetMapping("/tasks/{id}/findings")
  public Mono<Map<String, Object>> getFindings(@PathVariable String id) {
    return browserService
        .getFindingsForTask(id)
        .collectList()
        .map(findings -> Map.of("findings", findings));
  }

  @PostMapping("/findings")
  public Mono<BrowserFinding> addFinding(@RequestBody BrowserFinding finding) {
    return browserService.createFinding(finding);
  }

  @GetMapping("/surf/screenshot")
  public Mono<Map<String, String>> getScreenshot() {
    return browserService.getScreenshot().map(s -> Map.of("screenshot", s));
  }

  @PostMapping("/surf/navigate")
  public Mono<ResponseEntity<Void>> navigate(@RequestBody Map<String, String> body) {
    return browserService.navigateTo(body.get("url")).then(Mono.just(ResponseEntity.ok().build()));
  }

  @PostMapping("/surf/click")
  public Mono<ResponseEntity<Void>> click(@RequestBody Map<String, String> body) {
    return browserService.click(body.get("selector")).then(Mono.just(ResponseEntity.ok().build()));
  }

  @PostMapping("/surf/fill")
  public Mono<ResponseEntity<Void>> fill(@RequestBody Map<String, String> body) {
    return browserService
        .fill(body.get("selector"), body.get("value"))
        .then(Mono.just(ResponseEntity.ok().build()));
  }

  @PostMapping("/surf/click-at")
  public Mono<ResponseEntity<Void>> clickAt(@RequestBody Map<String, Integer> body) {
    return browserService
        .clickAt(body.get("x"), body.get("y"))
        .then(Mono.just(ResponseEntity.ok().build()));
  }

  @PostMapping("/surf/type-key")
  public Mono<ResponseEntity<Void>> typeKey(@RequestBody Map<String, String> body) {
    return browserService.typeKey(body.get("key")).then(Mono.just(ResponseEntity.ok().build()));
  }

  @GetMapping("/surf/accessibility")
  public Mono<Map<String, Object>> getAccessibilityTree() {
    return browserService.getAccessibilityTree();
  }

  /** Test endpoint to simulate activity */
  @PostMapping("/simulate-activity")
  public Mono<BrowserActivity> simulateActivity(@RequestBody Map<String, String> body) {
    return browserService.recordActivity(
        body.get("url"),
        body.getOrDefault("action", "surf"),
        body.getOrDefault("title", "Unknown Page"),
        body.get("reasoning"));
  }

  @PostMapping("/tasks/{id}/step")
  public Mono<ResponseEntity<Void>> executeStep(@PathVariable String id) {
    return browserService.executeAutonomousStep(id).then(Mono.just(ResponseEntity.ok().build()));
  }
}
