package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Map;

/**
 * n8n অটোমেশন হাব ইন্টিগ্রেশন সার্ভিস।
 * এটি SupremeAI ব্যাকএন্ড থেকে n8n ফ্লো এবং এপিআই ট্রিগার করতে ব্যবহৃত হয়।
 */
@Service
public class N8nIntegrationService {

    private static final Logger logger = LoggerFactory.getLogger(N8nIntegrationService.class);

    private final WebClient webClient;
    private final String n8nUrl;
    private final String apiKey;

    public N8nIntegrationService(
            @Value("${n8n.url:}") String n8nUrl,
            @Value("${n8n.api.key:}") String apiKey) {
        this.n8nUrl = n8nUrl;
        this.apiKey = apiKey;
        
        logger.info("[n8n] N8nIntegrationService ইনিশিয়ালাইজ করা হচ্ছে। URL: {}", n8nUrl);
        
        this.webClient = WebClient.builder()
                .baseUrl(n8nUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-N8N-API-KEY", apiKey)
                .build();
    }

    /**
     * n8n-এর নির্দিষ্ট ওয়েবহুক ট্রিগার করে।
     * 
     * @param webhookPath ওয়েবহুকের পাথ (যেমন: "webhook/supremeai-trigger")
     * @param payload ফ্লোতে পাঠানোর ডাটা পে-লোড
     * @return প্রতিক্রিয়ার স্ট্রিং
     */
    public Mono<String> triggerWebhook(String webhookPath, Map<String, Object> payload) {
        String fullPath = webhookPath.startsWith("/") ? webhookPath : "/" + webhookPath;
        logger.info("[n8n] ওয়েবহুক ট্রিগার করা হচ্ছে: {}{}", n8nUrl, fullPath);
        
        return webClient.post()
                .uri(fullPath)
                .bodyValue(payload != null ? payload : Collections.emptyMap())
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> logger.info("[n8n] ওয়েবহুক সফলভাবে রেসপন্স করেছে।"))
                .doOnError(error -> logger.error("[n8n] ওয়েবহুক ট্রিগার ব্যর্থ হয়েছে: {}", error.getMessage()));
    }

    /**
     * n8n API ব্যবহার করে কোনো অ্যাকশন সম্পন্ন করে।
     * 
     * @param endpoint এপিআই এন্ডপয়েন্ট (যেমন: "/api/v1/workflows")
     * @param method HTTP মেথড (GET, POST, ইত্যাদি)
     * @param body রিকোয়েস্ট বডি
     * @return এপিআই রেসপন্স
     */
    public Mono<String> callApi(String endpoint, String method, Object body) {
        logger.info("[n8n] এপিআই কল করা হচ্ছে: {} {}", method, endpoint);
        
        WebClient.RequestBodySpec requestSpec;
        if ("POST".equalsIgnoreCase(method)) {
            requestSpec = (WebClient.RequestBodySpec) webClient.post().uri(endpoint);
        } else if ("PUT".equalsIgnoreCase(method)) {
            requestSpec = (WebClient.RequestBodySpec) webClient.put().uri(endpoint);
        } else if ("DELETE".equalsIgnoreCase(method)) {
            return webClient.delete().uri(endpoint).retrieve().bodyToMono(String.class);
        } else {
            return webClient.get().uri(endpoint).retrieve().bodyToMono(String.class);
        }

        if (body != null) {
            requestSpec.bodyValue(body);
        }

        return requestSpec.retrieve()
                .bodyToMono(String.class)
                .doOnError(error -> logger.error("[n8n] এপিআই কল ব্যর্থ হয়েছে: {}", error.getMessage()));
    }

    /**
     * n8n সার্ভিসের সাথে সংযোগ পরীক্ষা করে।
     * 
     * @return সংযোগ সফল হলে true, অন্যথায় false
     */
    public Mono<Boolean> testConnection() {
        logger.info("[n8n] সংযোগ পরীক্ষা করা হচ্ছে...");
        return webClient.get()
                .uri("/")
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        logger.info("[n8n] সংযোগ সফল!");
                        return Mono.just(true);
                    } else {
                        logger.warn("[n8n] সংযোগ ব্যর্থ। স্ট্যাটাস কোড: {}", response.statusCode());
                        return Mono.just(false);
                    }
                })
                .onErrorResume(e -> {
                    logger.error("[n8n] সংযোগ পরীক্ষা করার সময় ত্রুটি: {}", e.getMessage());
                    return Mono.just(false);
                });
    }

    public String getN8nUrl() {
        return n8nUrl;
    }
}
