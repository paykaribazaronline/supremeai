package com.supremeai.controller;

import com.supremeai.model.SystemConfig;
import com.supremeai.model.UserTier;
import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.repository.UserRepository;
import com.supremeai.response.ApiResponse;
import com.supremeai.service.ConfigService;
import com.supremeai.service.QuotaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/quotas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminQuotaController {

    @Autowired
    private ConfigService configService;

    @Autowired
    private QuotaService quotaService;

    @Autowired
    private UserApiKeyRepository userApiKeyRepository;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/config")
    public Mono<ApiResponse<SystemConfig>> getQuotaConfig() {
        return Mono.just(ApiResponse.success("Quota configuration retrieved", configService.getConfig()));
    }

    @PostMapping("/config")
    public Mono<ApiResponse<SystemConfig>> updateQuotaConfig(@RequestBody SystemConfig newConfig) {
        SystemConfig current = configService.getConfig();
        if (newConfig.getTierQuotas() != null) current.setTierQuotas(newConfig.getTierQuotas());
        if (newConfig.getTierMaxApis() != null) current.setTierMaxApis(newConfig.getTierMaxApis());
        if (newConfig.getTierMaxSimulatorInstalls() != null) current.setTierMaxSimulatorInstalls(newConfig.getTierMaxSimulatorInstalls());
        
        return configService.updateConfig(current)
                .map(saved -> ApiResponse.success("Quota configuration updated successfully", saved));
    }

    @GetMapping("/usage")
    public Mono<ApiResponse<List<UserApiKey>>> getAllUsage() {
        return userApiKeyRepository.findAll()
                .collectList()
                .map(list -> ApiResponse.success("Usage data retrieved", list));
    }

    @PostMapping("/reset/{apiKey}")
    public Mono<ApiResponse<Boolean>> resetUsage(@PathVariable String apiKey) {
        return quotaService.resetApiUsage(apiKey)
                .map(success -> success 
                    ? ApiResponse.success("Usage reset successfully", true)
                    : ApiResponse.error("Failed to reset usage or API key not found"));
    }
}
