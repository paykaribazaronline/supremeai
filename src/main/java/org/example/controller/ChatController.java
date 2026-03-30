package org.example.controller;

import org.example.model.ChatMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Chat Controller
 * Manages chat communication with AI agents
 */
@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final List<ChatMessage> chatHistory = new ArrayList<>();

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory() {
        try {
            return ResponseEntity.ok(chatHistory);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessage message) {
        try {
            if (message.getId() == null) {
                message.setId(UUID.randomUUID().toString());
            }
            if (message.getTimestamp() == null) {
                message.setTimestamp(LocalDateTime.now());
            }
            
            chatHistory.add(message);
            
            // Simulate AI response
            if ("user".equals(message.getSender())) {
                ChatMessage aiResponse = new ChatMessage();
                aiResponse.setId(UUID.randomUUID().toString());
                aiResponse.setSender("ai");
                aiResponse.setAgent(message.getAgent());
                aiResponse.setContent("Processing: " + message.getContent());
                aiResponse.setTimestamp(LocalDateTime.now());
                aiResponse.setStatus("sent");
                chatHistory.add(aiResponse);
            }

            return ResponseEntity.ok(Map.of("success", true, "messageId", message.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
