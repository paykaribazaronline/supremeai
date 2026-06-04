package com.supremeai.middleware;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Device Emulation Middleware - Phase 4
 *
 * <p>Transforms HTTP responses for simulator preview requests to match device-specific
 * characteristics (viewport, user-agent, touch events).
 *
 * <p>Applied to all requests matching /simulator/preview/**
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class DeviceEmulationMiddleware implements WebFilter {

  private static final Logger logger = LoggerFactory.getLogger(DeviceEmulationMiddleware.class);

  private static final String SIMULATOR_PREFIX = "/simulator/preview";

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();

    if (!request.getPath().value().startsWith(SIMULATOR_PREFIX)) {
      return chain.filter(exchange);
    }

    String device = request.getQueryParams().getFirst("device");
    if (device == null || device.isEmpty()) {
      device = "PIXEL_6";
    }

    DeviceProfile profile = DeviceProfile.fromType(device);

    logger.trace("[DEVICE_EMU] Applying {} profile for {}", device, request.getPath());

    // Add device info to response headers
    exchange.getResponse().getHeaders().add("X-Device-Type", device);
    exchange
        .getResponse()
        .getHeaders()
        .add("X-Device-Width", String.valueOf(profile.viewportWidth));
    exchange
        .getResponse()
        .getHeaders()
        .add("X-Device-Height", String.valueOf(profile.viewportHeight));
    exchange
        .getResponse()
        .getHeaders()
        .add("X-Device-DPR", String.valueOf(profile.devicePixelRatio));

    // Override client hints for device emulation
    request
        .mutate()
        .header("Viewport-Width", String.valueOf(profile.viewportWidth))
        .header("Sec-CH-UA-Mobile", profile.isMobile ? "?1" : "?0")
        .header("DPR", String.valueOf(profile.devicePixelRatio));

    return chain.filter(exchange);
  }

  // ──────────────────────────────────────────────────────────────────────
  // Device profile data
  // ──────────────────────────────────────────────────────────────────────

  private static class DeviceProfile {
    final int viewportWidth;
    final int viewportHeight;
    final double devicePixelRatio;
    final boolean isMobile;

    DeviceProfile(int w, int h, double dpr, boolean mobile) {
      this.viewportWidth = w;
      this.viewportHeight = h;
      this.devicePixelRatio = dpr;
      this.isMobile = mobile;
    }

    static DeviceProfile fromType(String type) {
      return switch (type.toUpperCase()) {
        case "PIXEL_6", "SAMSUNG_S24" -> new DeviceProfile(1080, 2340, 3.0, true);
        case "PIXEL_7" -> new DeviceProfile(1080, 2400, 3.0, true);
        case "IPHONE_15", "IPHONE_15_PRO" -> new DeviceProfile(1179, 2556, 3.0, true);
        case "TABLET_10" -> new DeviceProfile(1920, 1200, 2.0, false);
        default -> new DeviceProfile(1080, 2340, 3.0, true);
      };
    }
  }
}
