package com.supremeai.controller.browser;

import com.supremeai.model.browser.*;
import com.supremeai.service.browser.BrowserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

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
        return browserService.startBrowsing()
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/surf/stop")
    public Mono<ResponseEntity<Void>> stopSurf() {
        return browserService.stopBrowsing()
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/activity/recent")
    public Mono<Map<String, Object>> getRecentActivity() {
        return browserService.getRecentActivity()
            .collectList()
            .map(activities -> Map.of("activities", activities));
    }

    @GetMapping("/credentials")
    public Mono<Map<String, Object>> getCredentials() {
        return browserService.getAllCredentials()
            .collectList()
            .map(credentials -> Map.of("credentials", credentials));
    }

    @PostMapping("/credentials")
    public Mono<StoredCredential> saveCredential(@RequestBody StoredCredential credential) {
        return browserService.saveCredential(credential);
    }

    @PostMapping("/surf/resume")
    public Mono<ResponseEntity<Void>> resume(@RequestBody Map<String, String> body) {
        return browserService.resumeActivity(body.get("activityId"))
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @PostMapping("/surf/skip-auth")
    public Mono<ResponseEntity<Void>> skipAuth(@RequestBody Map<String, String> body) {
        return browserService.skipAuth(body.get("activityId"))
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/urls/allowed")
    public Mono<Map<String, Object>> getAllowedUrls() {
        return browserService.getAllowedUrls()
            .collectList()
            .map(urls -> Map.of("urls", urls));
    }

    @GetMapping("/urls/denied")
    public Mono<Map<String, Object>> getDeniedUrls() {
        return browserService.getDeniedUrls()
            .collectList()
            .map(urls -> Map.of("urls", urls));
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

    @PutMapping("/urls/{id}")
    public Mono<UrlPermission> updateUrl(@PathVariable String id, @RequestBody UrlPermission permission) {
        return browserService.updateUrlPermission(id, permission);
    }

    @DeleteMapping("/urls/{id}")
    public Mono<ResponseEntity<Void>> deleteUrl(@PathVariable String id) {
        return browserService.deleteUrlPermission(id)
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/urls/requests")
    public Mono<Map<String, Object>> getRequests() {
        return browserService.getPermissionRequests()
            .collectList()
            .map(requests -> Map.of("requests", requests));
    }

    @PostMapping("/urls/requests/{id}/decision")
    public Mono<ResponseEntity<Void>> decision(@PathVariable String id, @RequestBody Map<String, Boolean> body) {
        return browserService.processPermissionDecision(id, body.get("approved"))
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/system-learning")
    public Mono<Map<String, Object>> getSystemLearning() {
        return browserService.getSystemLearningStatus();
    }

    @PostMapping("/system-learning/toggle")
    public Mono<ResponseEntity<Void>> toggleLearning(@RequestBody Map<String, Boolean> body) {
        return browserService.toggleAutoLearn(body.get("enabled"))
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/tasks")
    public Mono<Map<String, Object>> getTasks() {
        return browserService.getActiveTasks()
            .collectList()
            .map(tasks -> Map.of("tasks", tasks));
    }

    @PostMapping("/tasks")
    public Mono<BrowserTask> createTask(@RequestBody Map<String, String> body) {
        return browserService.createActivityTask(body.get("goal"));
    }

    @DeleteMapping("/tasks/{id}")
    public Mono<ResponseEntity<Void>> deleteTask(@PathVariable String id) {
        return browserService.deleteTask(id)
            .then(Mono.just(ResponseEntity.ok().build()));
    }

    @GetMapping("/tasks/{id}/findings")
    public Mono<Map<String, Object>> getFindings(@PathVariable String id) {
        return browserService.getFindingsForTask(id)
            .collectList()
            .map(findings -> Map.of("findings", findings));
    }

    @PostMapping("/findings")
    public Mono<BrowserFinding> addFinding(@RequestBody BrowserFinding finding) {
        return browserService.createFinding(finding);
    }
    
    /**
     * Test endpoint to simulate activity
     */
    @PostMapping("/simulate-activity")
    public Mono<BrowserActivity> simulateActivity(@RequestBody Map<String, String> body) {
        return browserService.recordActivity(
            body.get("url"), 
            body.getOrDefault("action", "surf"), 
            body.getOrDefault("title", "Unknown Page"),
            body.get("reasoning")
        );
    }
}
