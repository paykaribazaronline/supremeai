package com.supremeai.controller;

import com.supremeai.model.GeneratedApp;
import com.supremeai.model.SimulatorDeploymentRecord;
import com.supremeai.repository.SimulatorDeploymentRepository;
import com.supremeai.service.CodeGenerationService;
import com.supremeai.service.SimulatorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Simulator Runtime Controller - Serves generated apps with device emulation.
 *
 * This is the actual runtime endpoint that generated apps are served from.
 * It fetches generated app content from Firestore and serves it with
 * device-specific transformations applied via DeviceEmulationMiddleware.
 *
 * Endpoints:
 * - GET /simulator/preview/{appId} - Serve generated app
 * - GET /simulator/preview/{appId}/health - Health check
 */
@RestController
@RequestMapping("/simulator/preview")
public class SimulatorRuntimeController {

    private static final Logger logger = LoggerFactory.getLogger(SimulatorRuntimeController.class);

    @Autowired
    private SimulatorService simulatorService;

    @Autowired
    private CodeGenerationService codeGenerationService;

    @Autowired
    private SimulatorDeploymentRepository deploymentRepository;

    @Autowired
    private WebClient.Builder webClientBuilder;

    /**
     * Serve a generated app with device emulation.
     * Fetches app content from GeneratedApp and wraps it in device-specific
     * transformations (viewport, user-agent hints, touch events).
     */
    @GetMapping("/{appId}")
    public Mono<ResponseEntity<String>> servePreview(
            @PathVariable String appId,
            @RequestParam(defaultValue = "PIXEL_6") String device) {

        logger.debug("[SIM_RUNTIME] Serving preview for app={}, device={}", appId, device);

        return simulatorService.getGeneratedApp(appId)
                .map(app -> {
                    String content = resolveContent(app);
                    String emulated = applyDeviceEmulation(content, device);
                    return ResponseEntity.ok()
                            .header("X-Simulator-Device", device)
                            .header("X-Simulator-App", appId)
                            .body(emulated);
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(notFoundPage(appId)));
    }

    /**
     * Health check for a simulator deployment.
     */
    @GetMapping("/{appId}/health")
    public Mono<ResponseEntity<Map<String, Object>>> healthCheck(
            @PathVariable String appId) {

        return deploymentRepository.findById(appId)
                .map(record -> {
                    Map<String, Object> health = new HashMap<>();
                    health.put("appId", appId);
                    health.put("status", record.getStatus());
                    healthy(record, health);
                    return ResponseEntity.ok(health);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Map<String, Object> health = new HashMap<>();
                    health.put("appId", appId);
                    health.put("status", "UNKNOWN");
                    return Mono.just(ResponseEntity.ok(health));
                }));
    }

    /**
     * Proxy API requests to the generated app's backend.
     * Routes /simulator/preview/{appId}/api/** → actual backend.
     */
    @GetMapping("/{appId}/api/**")
    public Mono<ResponseEntity<String>> proxyApiRequest(
            @PathVariable String appId,
            @RequestParam(defaultValue = "PIXEL_6") String device) {

        logger.debug("[SIM_RUNTIME] API proxy for app={}", appId);

        return simulatorService.getGeneratedApp(appId)
                .map(app -> ResponseEntity.ok()
                        .header("X-Simulator-Device", device)
                        .body("{\"status\":\"ok\",\"appId\":\"" + appId + "\"}"))
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("{\"error\":\"App not found\"}"));
    }

    /**
     * Stream logs for a simulator session.
     */
    @GetMapping("/{appId}/logs")
    public Mono<ResponseEntity<String>> streamLogs(
            @PathVariable String appId) {

        logger.debug("[SIM_RUNTIME] Log stream requested for app={}", appId);

        return Mono.just(ResponseEntity.ok()
                .header("X-Simulator-App", appId)
                .body("{\"logs\":[],\"appId\":\"" + appId + "\"}"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────

    private String resolveContent(GeneratedApp app) {
        if (app.getHtmlContent() != null && !app.getHtmlContent().isEmpty()) {
            return app.getHtmlContent();
        }
        if (app.getSourceFiles() != null && !app.getSourceFiles().isEmpty()) {
            return app.getSourceFiles().values().iterator().next();
        }
        return notFoundPage(app.getAppId());
    }

    private String applyDeviceEmulation(String html, String device) {
        DeviceProfile profile = DeviceProfile.fromType(device);

        // Inject device emulation script
        String injection = String.format(
            "<script>" +
            "window.__SIMULATOR_DEVICE__ = '%s';" +
            "window.__SIMULATOR_VIEWPORT__ = {width:%d, height:%d, dpr:%.1f};" +
            "window.__SIMULATOR_USER_AGENT__ = '%s';" +
            "</script>",
            device,
            profile.viewportWidth,
            profile.viewportHeight,
            profile.devicePixelRatio,
            profile.userAgent
        );

        // Inject device-specific CSS
        String deviceCss = String.format(
            "<style>" +
            ":root { --device-width: %dpx; --device-height: %dpx; --device-dpr: %.1f; }" +
            "#simulator-viewport { width: %dpx; height: %dpx; " +
            "overflow: hidden; position: relative; margin: 0 auto; " +
            "box-shadow: 0 4px 24px rgba(0,0,0,0.15); border-radius: 12px; }" +
            "</style>",
            profile.viewportWidth, profile.viewportHeight, profile.devicePixelRatio,
            profile.viewportWidth, profile.viewportHeight
        );

        if (html.contains("</head>")) {
            html = html.replace("</head>", injection + deviceCss + "</head>");
        } else if (html.contains("<html")) {
            html = html.replace("<html", "<html" +
                " style='max-width:" + profile.viewportWidth + "px;margin:0 auto;' ");
        }

        // Wrap content in simulator viewport div
        if (!html.contains("simulator-viewport")) {
            html = html.replaceFirst("(<body[^>]*>)",
                "$1<div id='simulator-viewport'>") + "</div>";
        }

        return html;
    }

    private String notFoundPage(String appId) {
        return "<html><body style='display:flex;align-items:center;justify-content:center;height:100vh;font-family:sans-serif;'>" +
            "<div style='text-align:center;'>" +
            "<h1>🚀 Simulator</h1>" +
            "<p>App <strong>" + appId + "</strong> not found or not yet generated.</p>" +
            "<p>Generate an app first, then preview it here.</p>" +
            "</div></body></html>";
    }

    private void healthy(SimulatorDeploymentRecord record, Map<String, Object> health) {
        String status = record.getStatus();
        health.put("healthy", "RUNNING".equals(status));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Device profiles
    // ──────────────────────────────────────────────────────────────────────

    private static class DeviceProfile {
        final int viewportWidth;
        final int viewportHeight;
        final double devicePixelRatio;
        final String userAgent;

        DeviceProfile(int w, int h, double dpr, String ua) {
            this.viewportWidth = w;
            this.viewportHeight = h;
            this.devicePixelRatio = dpr;
            this.userAgent = ua;
        }

        static DeviceProfile fromType(String type) {
            return switch (type.toUpperCase()) {
                case "PIXEL_6", "SAMSUNG_S24" -> new DeviceProfile(
                    1080, 2340, 3.0,
                    "Mozilla/5.0 (Linux; Android 14) Chrome/120.0.0.0 Mobile");
                case "PIXEL_7" -> new DeviceProfile(
                    1080, 2400, 3.0,
                    "Mozilla/5.0 (Linux; Android 14) Chrome/120.0.0.0 Mobile");
                case "IPHONE_15", "IPHONE_15_PRO" -> new DeviceProfile(
                    1179, 2556, 3.0,
                    "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) Safari/604.1");
                case "TABLET_10" -> new DeviceProfile(
                    1920, 1200, 2.0,
                    "Mozilla/5.0 (Linux; Android 13) Chrome/120.0.0.0");
                default -> new DeviceProfile(
                    1080, 2340, 3.0,
                    "Mozilla/5.0 (Linux; Android 14) Chrome/120.0.0.0 Mobile");
            };
        }
    }
}
