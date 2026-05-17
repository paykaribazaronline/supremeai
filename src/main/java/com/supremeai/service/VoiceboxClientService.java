package com.supremeai.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;


@Service
public class VoiceboxClientService {

    private final WebClient webClient;

    public VoiceboxClientService(@Value("${voicebox.base-url:http://localhost:17493}") String baseUrl) {
        this.webClient = WebClient.builder()
            .baseUrl(baseUrl)
            .build();
    }

    public Mono<Map> generateSpeech(String text, String profileId) {
        return webClient.post()
            .uri("/generate")
            .bodyValue(Map.of("text", text, "profile_id", profileId))
            .retrieve()
            .bodyToMono(Map.class);
    }

    public Mono<Map> speak(String text, String profile) {
        return webClient.post()
            .uri("/speak")
            .bodyValue(Map.of("text", text, "profile", profile))
            .retrieve()
            .bodyToMono(Map.class);
    }

    public Mono<Map> transcribe(byte[] audioData, String language) {
        org.springframework.http.client.MultipartBodyBuilder builder = new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("file", new org.springframework.core.io.ByteArrayResource(audioData))
            .filename("audio.wav");
        if (language != null) {
            builder.part("language", language);
        }

        return webClient.post()
            .uri("/transcribe")
            .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(builder.build()))
            .retrieve()
            .bodyToMono(Map.class);
    }
}
