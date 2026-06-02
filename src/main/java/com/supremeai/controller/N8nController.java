package com.supremeai.controller;

import com.supremeai.service.N8nIntegrationService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * n8n অটোমেশন কন্ট্রোলার।
 * এটি ড্যাশবোর্ড বা এক্সটার্নাল এপিআই থেকে n8n ইন্টিগ্রেশন এবং ফ্লো ট্রিগার করার সুবিধা দেয়।
 */
@RestController
@RequestMapping("/api/n8n")
@CrossOrigin(origins = "*")
public class N8nController {
    public N8nController(N8nIntegrationService n8nService) {
        this.n8nService = n8nService;
    }



    /**
     * n8n সার্ভিসের সাথে সংযোগের স্থিতি চেক করে।
     */
    @GetMapping("/status")
    public Mono<Map<String, Object>> getStatus() {
        return n8nService.testConnection()
                .map(connected -> Map.<String, Object>of(
                        "success", true,
                        "connected", connected,
                        "url", n8nService.getN8nUrl(),
                        "message", connected ? "n8n সার্ভিসের সাথে সফলভাবে সংযোগ স্থাপিত হয়েছে।" : "n8n সার্ভিসের সাথে সংযোগ করা যায়নি।"
                ));
    }

    /**
     * কাস্টম পে-লোড দিয়ে n8n এর একটি নির্দিষ্ট ওয়েবহুক ট্রিগার করে।
     */
    @PostMapping("/trigger")
    @SuppressWarnings("unchecked")
    public Mono<Map<String, Object>> triggerWebhook(@RequestBody Map<String, Object> request) {
        String path = (String) request.getOrDefault("path", "webhook/supremeai-trigger");
        Map<String, Object> payload = (Map<String, Object>) request.getOrDefault("payload", Map.of());

        return n8nService.triggerWebhook(path, payload)
                .map(response -> Map.<String, Object>of(
                        "success", true,
                        "message", "ওয়েবহুক সফলভাবে ট্রিগার হয়েছে।",
                        "response", response
                ))
                .onErrorResume(e -> Mono.just(Map.<String, Object>of(
                        "success", false,
                        "error", e.getMessage()
                )));
    }

    /**
     * n8n-এ তৈরি করা সকল ওয়ার্কফ্লোর তালিকা রিট্রিভ করে।
     */
    @GetMapping("/workflows")
    public Mono<String> getWorkflows() {
        return n8nService.callApi("/api/v1/workflows", "GET", null);
    }
}
