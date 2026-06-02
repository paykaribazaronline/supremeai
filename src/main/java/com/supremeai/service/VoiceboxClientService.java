package com.supremeai.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Map;


@Service
public class VoiceboxClientService {
    public VoiceboxClientService(ConfigService configService) {
        this.configService = configService;
    }



    private final WebClient webClient;

    public VoiceboxClientService() {
        this.webClient = WebClient.builder().build();
    }

    private Mono<String> getBaseUrl() {
        return configService.getEffectiveString("voicebox.base-url", "http://localhost:17493");
    }

    public Mono<Map> generateSpeech(String text, String profileId) {
        return getBaseUrl().flatMap(baseUrl -> 
            webClient.post()
                .uri(baseUrl + "/generate")
                .bodyValue(Map.of("text", text, "profile_id", profileId))
                .retrieve()
                .bodyToMono(Map.class)
        );
    }

    public Mono<Map> speak(String text, String profile) {
        return getBaseUrl().flatMap(baseUrl -> 
            webClient.post()
                .uri(baseUrl + "/speak")
                .bodyValue(Map.of("text", text, "profile", profile))
                .retrieve()
                .bodyToMono(Map.class)
        );
    }

    public Mono<Map> transcribe(byte[] audioData, String language) {
        org.springframework.http.client.MultipartBodyBuilder builder = new org.springframework.http.client.MultipartBodyBuilder();
        builder.part("file", new org.springframework.core.io.ByteArrayResource(audioData))
            .filename("audio.wav");
        if (language != null) {
            builder.part("language", language);
        }

        return getBaseUrl().flatMap(baseUrl -> 
            webClient.post()
                .uri(baseUrl + "/transcribe")
                .body(org.springframework.web.reactive.function.BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(Map.class)
        );
    }
}
