package com.supremeai.controller;

import com.supremeai.service.VoiceboxClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import java.util.Map;

@RestController
@RequestMapping("/api/voicebox")
public class VoiceboxController {

    @Autowired
    private VoiceboxClientService voiceboxClientService;

    @PostMapping("/speak")
    public Mono<ResponseEntity<Map>> speak(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String profile = request.getOrDefault("profile", "default");
        if (text == null || text.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "text is required")));
        }
        return voiceboxClientService.speak(text, profile)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }

    @PostMapping("/generate")
    public Mono<ResponseEntity<Map>> generate(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        String profileId = request.getOrDefault("profile_id", "default");
        if (text == null || text.trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "text is required")));
        }
        return voiceboxClientService.generateSpeech(text, profileId)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }

    @PostMapping("/transcribe")
    public Mono<ResponseEntity<Map>> transcribe(@RequestBody byte[] audioData, @RequestParam(required = false) String language) {
        if (audioData == null || audioData.length == 0) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "audio data is empty")));
        }
        return voiceboxClientService.transcribe(audioData, language)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))));
    }
}
