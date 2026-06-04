package com.supremeai.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class SimulatorWebSocketConfig implements WebSocketConfigurer {

  private final SimulatorWebSocketHandler simulatorWebSocketHandler;

  public SimulatorWebSocketConfig(SimulatorWebSocketHandler simulatorWebSocketHandler) {
    this.simulatorWebSocketHandler = simulatorWebSocketHandler;
  }

  @Override
  public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    // Path pattern: /ws/simulator/{type}/{sessionId}
    // type: runtime | dashboard
    registry
        .addHandler(
            simulatorWebSocketHandler, "/ws/simulator/runtime/*", "/ws/simulator/dashboard/*")
        .setAllowedOrigins("*");
  }
}
