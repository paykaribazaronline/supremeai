package com.supremeai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping({"/api/status", "/api/health"})
public class ServerStatusController {

    @GetMapping
    public Map<String, String> getStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "SupremeAI Backend is running");
        status.put("version", "6.0.0");
        return status;
    }
}
