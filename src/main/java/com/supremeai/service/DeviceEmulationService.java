package com.supremeai.service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

/**
 * Device Emulation Service - Transforms web content for device-specific simulation.
 *
 * <p>Handles: - User-Agent rewriting - Viewport meta tag injection/modification - CSS viewport unit
 * conversions (px → dp) - Touch event injection - Device API mocking (geolocation, battery,
 * orientation) - DPR (Device Pixel Ratio) header injection
 */
@Service
public class DeviceEmulationService {

  private static final Logger logger = LoggerFactory.getLogger(DeviceEmulationService.class);

  // User-Agent strings per device
  private static final Map<String, String> USER_AGENTS =
      Map.ofEntries(
          Map.entry(
              "PIXEL_6",
              "Mozilla/5.0 (Linux; Android 14; Pixel 6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"),
          Map.entry(
              "PIXEL_7",
              "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"),
          Map.entry(
              "SAMSUNG_S24",
              "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"),
          Map.entry(
              "IPHONE_15",
              "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"),
          Map.entry(
              "IPHONE_15_PRO",
              "Mozilla/5.0 (iPhone; CPU iPhone OS 17_4 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"),
          Map.entry(
              "TABLET_10",
              "Mozilla/5.0 (Linux; Android 13; Tablet 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"));

  // Device-specific viewport dimensions (CSS pixels)
  private static final Map<String, Integer> VIEWPORT_WIDTH =
      Map.of(
          "PIXEL_6", 412,
          "PIXEL_7", 412,
          "SAMSUNG_S24", 360,
          "IPHONE_15", 393,
          "IPHONE_15_PRO", 393,
          "TABLET_10", 1200);

  private static final Map<String, Integer> VIEWPORT_HEIGHT =
      Map.of(
          "PIXEL_6", 915,
          "PIXEL_7", 951,
          "SAMSUNG_S24", 904,
          "IPHONE_15", 852,
          "IPHONE_15_PRO", 852,
          "TABLET_10", 800);

  /** Create emulation context for a specific device profile. */
  public EmulationContext createContext(String deviceType) {
    EmulationContext ctx = new EmulationContext();
    ctx.setDeviceType(deviceType);
    ctx.setUserAgent(USER_AGENTS.getOrDefault(deviceType, USER_AGENTS.get("PIXEL_6")));
    ctx.setViewportWidth(VIEWPORT_WIDTH.getOrDefault(deviceType, 412));
    ctx.setViewportHeight(VIEWPORT_HEIGHT.getOrDefault(deviceType, 915));
    ctx.setDevicePixelRatio(getDprForDevice(deviceType));
    ctx.setPlatform(getPlatformForDevice(deviceType));
    return ctx;
  }

  /**
   * Transform HTML content by applying device emulation rules. Accepts String directly to avoid
   * byte[] round-trip conversion.
   */
  public String transformHtml(String html, EmulationContext context) {
    // 1. Inject/modify viewport meta tag
    html = injectViewportMeta(html, context);

    // 2. Increase click area for touch targets (44px minimum)
    html = enhanceTouchTargets(html);

    // 3. Inject device API polyfills/mocks
    html = injectDeviceApis(html, context);

    // 4. Inject simulator bootstrap script
    html = injectSimulatorBootstrap(html, context);

    // 5. Fix viewport-relative units if needed
    html = convertViewportUnits(html, context);

    return html;
  }

  /** Legacy byte[] version - delegates to String version. Kept for backwards compatibility. */
  public byte[] transformHtml(byte[] originalHtml, EmulationContext context) {
    String html = new String(originalHtml, StandardCharsets.UTF_8);
    String transformed = transformHtml(html, context);
    return transformed.getBytes(StandardCharsets.UTF_8);
  }

  /** Produce HTTP headers specific to device emulation. */
  public HttpHeaders createEmulationHeaders(EmulationContext context) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("X-Device-Emulation", context.getDeviceType());
    headers.add("X-Device-Pixel-Ratio", String.valueOf(context.getDevicePixelRatio()));
    headers.add("X-Viewport-Width", String.valueOf(context.getViewportWidth()));
    headers.add("X-Viewport-Height", String.valueOf(context.getViewportHeight()));
    return headers;
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Private transformation methods
  // ─────────────────────────────────────────────────────────────────────────────

  /** Inject or modify viewport meta tag. Ensures proper scaling on mobile devices. */
  private String injectViewportMeta(String html, EmulationContext ctx) {
    String viewportContent =
        String.format(
            "width=%d, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover",
            ctx.getViewportWidth());

    String metaTag = String.format("<meta name=\"viewport\" content=\"%s\">", viewportContent);

    // If <head> exists, inject after <meta charset> or first element
    if (html.contains("<head>")) {
      if (html.contains("<meta name=\"viewport\"")) {
        // Replace existing viewport
        html =
            html.replaceAll(
                Pattern.quote("<meta name=\"viewport\"") + "[^>]*>",
                Matcher.quoteReplacement(metaTag));
      } else {
        // Insert after <head>
        html = html.replace("<head>", "<head>\n    " + metaTag);
      }
    } else if (html.contains("<html")) {
      // No head? Add head with viewport
      html = html.replace("<html", "<html>\n<head>" + metaTag + "</head>");
    } else {
      // Fallback: prepend
      html = metaTag + "\n" + html;
    }

    logger.debug("[EMULATION] Injected viewport meta for device={}", ctx.getDeviceType());
    return html;
  }

  /**
   * Ensure touch targets meet 44px minimum for finger taps. Adds CSS rule to increase hit area for
   * clickable elements.
   */
  private String enhanceTouchTargets(String html) {
    String css =
        """
                <style id="supremeai-touch-enhancement">
                  button, a, [role="button"], .btn, .button {
                    min-height: 44px !important;
                    min-width: 44px !important;
                    padding: 12px 16px !important;
                    touch-action: manipulation !important;
                  }
                  input, select, textarea {
                    min-height: 44px !important;
                    font-size: 16px !important; /* Prevents iOS zoom on focus */
                  }
                </style>
                """;

    if (html.contains("</head>")) {
      html = html.replace("</head>", css + "\n</head>");
    } else {
      html = css + html;
    }

    return html;
  }

  /** Inject JavaScript to mock device APIs. */
  private String injectDeviceApis(String html, EmulationContext ctx) {
    String script =
        String.format(
            """
                <script id="supremeai-device-mock">
                  // Device orientation (portrait)
                  (function() {
                    Object.defineProperty(window, 'orientation', {
                      get: () => 0,
                      enumerable: true
                    });
                    Object.defineProperty(screen, 'orientation', {
                      get: () => ({ type: 'portrait-primary', angle: 0 }),
                      enumerable: true
                    });

                    // Geolocation mock (configurable)
                    window.navigator.geolocation = {
                      getCurrentPosition: function(success) {
                        success({
                          coords: {
                            latitude: 37.7749,  // San Francisco
                            longitude: -122.4194,
                            accuracy: 10
                          }
                        });
                      },
                      watchPosition: function(success) {
                        success({
                          coords: {
                            latitude: 37.7749,
                            longitude: -122.4194,
                            accuracy: 10
                          }
                        });
                      }
                    };

                    // Battery API mock
                    if ('getBattery' in navigator) {
                      navigator.getBattery = function() {
                        return Promise.resolve({
                          level: 0.85,
                          charging: true,
                          chargingTime: 1800,
                          dischargingTime: 7200
                        });
                      };
                    }

                    // Device pixel ratio (match device)
                    Object.defineProperty(window, 'devicePixelRatio', {
                      get: () => %.2f,
                      enumerable: true
                    });

                    // Simulator runtime metadata
                    window.__SIMULATOR_DEVICE__ = {
                      type: '%s',
                      platform: '%s',
                      viewport: { width: %d, height: %d },
                      dpr: %.2f
                    };

                    console.log('[Simulator] Device emulation initialized:', window.__SIMULATOR_DEVICE__);
                  })();
                </script>
                """,
            ctx.getDevicePixelRatio(),
            ctx.getDeviceType(),
            ctx.getPlatform(),
            ctx.getViewportWidth(),
            ctx.getViewportHeight(),
            ctx.getDevicePixelRatio());

    if (html.contains("</body>")) {
      html = html.replace("</body>", script + "\n</body>");
    } else if (html.contains("</head>")) {
      html = html.replace("</head>", "\n</head>") + script;
    } else {
      html = html + script;
    }

    return html;
  }

  /** Inject simulator bootstrap (WebSocket connection, command relay). */
  private String injectSimulatorBootstrap(String html, EmulationContext ctx) {
    // Actual WebSocket URL will be set by SimulatorRuntimeController
    String script =
        """
                <script>
                  // Simulator control channel (WebSocket set at runtime)
                  window.__SIMULATOR_CONTROL__ = null;

                  // Listen for commands from admin UI
                  window.addEventListener('message', function(event) {
                    if (event.data && event.data.type === 'SIMULATOR_COMMAND') {
                      const cmd = event.data.command;
                      if (window.__SIMULATOR_CONTROL__) {
                        window.__SIMULATOR_CONTROL__.send(JSON.stringify(cmd));
                      }
                    }
                  });

                  // Emit events back to admin
                  function emitSimulatorEvent(type, data) {
                    if (window.parent !== window) {
                      window.parent.postMessage({
                        source: 'supremeai-simulator',
                        type: type,
                        data: data
                      }, '*');
                    }
                  };

                  // Auto-emit page load complete
                  window.addEventListener('load', function() {
                    emitSimulatorEvent('page_load', {
                      url: window.location.href,
                      title: document.title
                    });
                  });
                </script>
                """;

    if (html.contains("</body>")) {
      html = html.replace("</body>", script + "\n</body>");
    } else {
      html = html + script;
    }

    return html;
  }

  /**
   * Convert viewport-relative units (vh, vw, vmin, vmax) to pixel values. In device emulation, we
   * want consistent physical sizing based on device resolution.
   */
  private String convertViewportUnits(String html, EmulationContext ctx) {
    // This is simple: we could inject CSS custom properties
    // --vh = viewport height in pixels
    // --vw = viewport width in pixels
    String cssVars =
        String.format(
            """
                <style id="supremeai-viewport-vars">
                  :root {
                    --sim-vw: %dpx;
                    --sim-vh: %dpx;
                    --sim-dpr: %.2f;
                  }
                </style>
                """,
            ctx.getViewportWidth(), ctx.getViewportHeight(), ctx.getDevicePixelRatio());

    if (html.contains("</head>")) {
      html = html.replace("</head>", cssVars + "\n</head>");
    } else {
      html = cssVars + html;
    }

    return html;
  }

  /** Get DPR (Device Pixel Ratio) for device. Retina displays: 2.0+, most Android: 2.5-3.5 */
  private double getDprForDevice(String deviceType) {
    return switch (deviceType) {
      case "IPHONE_15", "IPHONE_15_PRO" -> 3.0;
      case "PIXEL_6", "PIXEL_7", "SAMSUNG_S24" -> 3.0;
      case "TABLET_10" -> 2.0;
      default -> 1.0;
    };
  }

  /** Get platform string for device. */
  private String getPlatformForDevice(String deviceType) {
    return switch (deviceType) {
      case "IPHONE_15", "IPHONE_15_PRO" -> "iOS";
      default -> "Android";
    };
  }

  // ─────────────────────────────────────────────────────────────────────────────
  // Public Models
  // ─────────────────────────────────────────────────────────────────────────────

  /** Emulation context passed to transformers. */
  public static class EmulationContext {
    private String deviceType;
    private String userAgent;
    private int viewportWidth;
    private int viewportHeight;
    private double devicePixelRatio;
    private String platform;

    public String getDeviceType() {
      return deviceType;
    }

    public void setDeviceType(String deviceType) {
      this.deviceType = deviceType;
    }

    public String getUserAgent() {
      return userAgent;
    }

    public void setUserAgent(String userAgent) {
      this.userAgent = userAgent;
    }

    public int getViewportWidth() {
      return viewportWidth;
    }

    public void setViewportWidth(int viewportWidth) {
      this.viewportWidth = viewportWidth;
    }

    public int getViewportHeight() {
      return viewportHeight;
    }

    public void setViewportHeight(int viewportHeight) {
      this.viewportHeight = viewportHeight;
    }

    public double getDevicePixelRatio() {
      return devicePixelRatio;
    }

    public void setDevicePixelRatio(double devicePixelRatio) {
      this.devicePixelRatio = devicePixelRatio;
    }

    public String getPlatform() {
      return platform;
    }

    public void setPlatform(String platform) {
      this.platform = platform;
    }
  }
}
