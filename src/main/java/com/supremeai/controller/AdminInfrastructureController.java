package com.supremeai.controller;

import com.supremeai.model.InfrastructureAdvice;
import com.supremeai.service.InfrastructureConciergeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/admin/infrastructure")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInfrastructureController {

    private final InfrastructureConciergeService infraService;

    @GetMapping("/advice")
    public Mono<com.supremeai.response.ApiResponse<java.util.List<InfrastructureAdvice>>> getAllAdvice() {
        // Return all generated advice for the system
        return infraService.getAllAdvice()
                .collectList()
                .map(com.supremeai.response.ApiResponse::ok);
    }

    @PostMapping("/generate-advice")
    public Mono<com.supremeai.response.ApiResponse<InfrastructureAdvice>> generateGlobalAdvice() {
        return infraService.getOrGenerateAdvice("global-system")
                .map(com.supremeai.response.ApiResponse::ok);
    }

    @GetMapping("/advice/{appId}")
    public Mono<com.supremeai.response.ApiResponse<InfrastructureAdvice>> getAdvice(@PathVariable String appId) {
        return infraService.getOrGenerateAdvice(appId)
                .map(com.supremeai.response.ApiResponse::ok);
    }
}
