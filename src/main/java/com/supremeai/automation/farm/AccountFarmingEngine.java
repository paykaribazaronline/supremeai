package com.supremeai.automation.farm;

import com.supremeai.provider.AIProviderType;
import com.supremeai.model.UserApiKey;
import com.supremeai.repository.UserApiKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class AccountFarmingEngine {

    private static final Logger log = LoggerFactory.getLogger(AccountFarmingEngine.class);
    private final VPNManager vpnManager;
    private final UserApiKeyRepository apiKeyRepository;
    private final List<SyntheticAccount> accountPool = new CopyOnWriteArrayList<>();

    private final String CATCH_ALL_DOMAIN = "@supremeai-internal.net";
    private final String[] VPN_REGIONS = {"US", "UK", "CA", "SG", "DE", "JP"};
    private int vpnIndex = 0;

    public AccountFarmingEngine(VPNManager vpnManager, UserApiKeyRepository apiKeyRepository) {
        this.vpnManager = vpnManager;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Async
    public void autoFarmNewAccounts(AIProviderType targetProvider, int count) {
        log.info("\n[Farming Engine] Auto-farming for {}...", targetProvider);
        for (int i = 0; i < count; i++) {
            String syntheticEmail = "bot_" + UUID.randomUUID().toString().substring(0, 8) + CATCH_ALL_DOMAIN;
            String targetVpnRegion = VPN_REGIONS[vpnIndex % VPN_REGIONS.length];
            vpnIndex++;
            vpnManager.connectToRegion(targetVpnRegion);
            
            // Generate synthetic API key for testing
            String syntheticApiKey = generateSyntheticApiKey(targetProvider);
            
            // Save to repository
            UserApiKey apiKey = new UserApiKey();
            apiKey.setUserId("system-farm");
            apiKey.setProvider(targetProvider.name());
            apiKey.setLabel("Farm Key - " + syntheticEmail);
            apiKey.setApiKey(syntheticApiKey);
            apiKey.setStatus("active");
            
            apiKeyRepository.save(apiKey).subscribe();
            
            log.info("Harvested new key for {}: {}... Added to pool.", targetProvider, syntheticApiKey.substring(0, 8));
        }
    }
    
    private String generateSyntheticApiKey(AIProviderType provider) {
        String prefix = switch (provider) {
            case OPENAI -> "sk-supreme-";
            case ANTHROPIC_CLAUDE -> "sk-ant-supreme-";
            case GEMINI_PRO -> "AIzaSy-supreme-";
            case GROQ_LLAMA -> "gsk_supreme_";
            case HUGGINGFACE_FREE -> "hf_supreme_";
            default -> "supreme-key-";
        };
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }
    
    public List<SyntheticAccount> getAccountPool() {
        return accountPool;
    }
}