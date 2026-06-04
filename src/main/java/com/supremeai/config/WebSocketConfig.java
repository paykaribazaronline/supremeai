package com.supremeai.config;

import com.supremeai.security.JwtUtil;
import com.supremeai.websocket.SimulatorWebSocketHandler;
import com.supremeai.websocket.AdminWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    @Value("${cors.allowed-origins:*}")
    private String allowedOriginsCsv;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private AdminWebSocketHandler adminWebSocketHandler;

    @Autowired
    @org.springframework.context.annotation.Lazy
    private SimulatorWebSocketHandler simulatorWebSocketHandler;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{10000, 10000})
                .setTaskScheduler(messageBrokerTaskScheduler());
        config.setApplicationDestinationPrefixes("/app");
    }

    @Bean
    public TaskScheduler messageBrokerTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("stomp-heartbeat-thread-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns(origins.toArray(new String[0]))
                .setHandshakeHandler(new JwtHandshakeHandler(jwtUtil))
                .withSockJS();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        List<String> origins = Arrays.stream(allowedOriginsCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        registry.addHandler(simulatorWebSocketHandler, "/ws/simulator/{sessionId}")
                .setAllowedOriginPatterns(origins.toArray(new String[0]));

        registry.addHandler(adminWebSocketHandler, "/ws/visualization")
                .setAllowedOriginPatterns(origins.toArray(new String[0]));
    }
}