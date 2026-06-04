package com.supremeai.service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Simulator Screenshot Service - Captures screenshots from simulator sessions.
 *
 * <p>In production, uses Playwright or Puppeteer to render the preview page and capture
 * device-specific screenshots.
 */
@Service
public class SimulatorScreenshotService {

  private static final Logger logger = LoggerFactory.getLogger(SimulatorScreenshotService.class);

  /** Capture a screenshot of a simulator session. */
  public Mono<ScreenshotResult> captureScreenshot(
      String sessionId, String appId, String deviceType) {
    logger.info(
        "[SIM_SCREENSHOT] Capturing screenshot for session={}, app={}, device={}",
        sessionId,
        appId,
        deviceType);

    return Mono.fromCallable(
        () -> {
          // In production: Use Playwright to navigate to preview URL,
          // set viewport to device profile dimensions, and capture screenshot
          ScreenshotResult result = new ScreenshotResult();
          result.sessionId = sessionId;
          result.appId = appId;
          result.deviceType = deviceType;
          result.format = "png";
          result.capturedAt = System.currentTimeMillis();

          // Placeholder: return empty base64 PNG
          result.base64Data = Base64.getEncoder().encodeToString(new byte[0]);
          result.width = getDeviceWidth(deviceType);
          result.height = getDeviceHeight(deviceType);

          return result;
        });
  }

  /** Capture screenshot at a specific test step. */
  public Mono<String> captureStepScreenshot(String sessionId, String stepName) {
    logger.debug(
        "[SIM_SCREENSHOT] Capturing step screenshot: {} for session={}", stepName, sessionId);

    return Mono.fromCallable(
        () -> {
          // In production: capture and upload to Cloud Storage
          return "gs://supremeai-simulator-screenshots/"
              + sessionId
              + "/"
              + stepName
              + "_"
              + System.currentTimeMillis()
              + ".png";
        });
  }

  /** Get screenshot metadata for a session. */
  public Map<String, Object> getSessionScreenshots(String sessionId) {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("sessionId", sessionId);
    metadata.put("screenshotCount", 0);
    metadata.put("screenshots", java.util.List.of());
    return metadata;
  }

  private int getDeviceWidth(String deviceType) {
    return switch (deviceType.toUpperCase()) {
      case "PIXEL_6", "SAMSUNG_S24", "PIXEL_7" -> 1080;
      case "IPHONE_15", "IPHONE_15_PRO" -> 1179;
      case "TABLET_10" -> 1920;
      default -> 1080;
    };
  }

  private int getDeviceHeight(String deviceType) {
    return switch (deviceType.toUpperCase()) {
      case "PIXEL_6", "SAMSUNG_S24" -> 2340;
      case "PIXEL_7" -> 2400;
      case "IPHONE_15", "IPHONE_15_PRO" -> 2556;
      case "TABLET_10" -> 1200;
      default -> 2340;
    };
  }

  // ──────────────────────────────────────────────────────────────────────
  // Inner models
  // ──────────────────────────────────────────────────────────────────────

  public static class ScreenshotResult {
    public String sessionId;
    public String appId;
    public String deviceType;
    public String format;
    public String base64Data;
    public int width;
    public int height;
    public long capturedAt;
    public String storageUrl;
  }
}
