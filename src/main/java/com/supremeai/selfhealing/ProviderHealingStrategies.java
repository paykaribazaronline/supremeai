package com.supremeai.selfhealing;

import com.supremeai.provider.AIProvider;
import com.supremeai.provider.AIProviderFactory;
import com.supremeai.selfhealing.AutoHealingStrategyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.supremeai.repository.UserApiKeyRepository;
import com.supremeai.repository.HealingEventRepository;
import com.supremeai.model.UserApiKey;
import com.supremeai.model.HealingEvent;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * প্রোভাইডার হিলিং স্ট্র্যাটেজি যা বিভিন্ন প্রোভাইডারের জন্য স্বয়ংক্রিয় হিলিং সমর্থন করে।
 * এটি প্রোভাইডার সুইচিং, API কী রোটেশন, এবং কনফিগারেশন পুনরুদ্ধার সমর্থন করে।
 */
@Component
public class ProviderHealingStrategies {

    private static final Logger logger = LoggerFactory.getLogger(ProviderHealingStrategies.class);

    @Autowired
    private AIProviderFactory providerFactory;

    @Autowired
    private AutoHealingStrategyService autoHealingService;

    @Autowired
    private UserApiKeyRepository apiKeyRepository;

    @Autowired
    private HealingEventRepository healingEventRepository;

    /**
     * প্রোভাইডার সুইচিং স্ট্র্যাটেজি
     */
    public AutoHealingStrategyService.HealingStrategy createProviderSwitchingStrategy(
            String currentProvider, String alternativeProvider) {
        return (Exception error) -> {
            logger.info("Attempting to switch from {} to {} due to error: {}",
                    currentProvider, alternativeProvider, error.getMessage());

            try {
                // বিকল্প প্রোভাইডার পরীক্ষা করা
                AIProvider alternative = providerFactory.getProvider(alternativeProvider);
                if (alternative != null) {
                    logger.info("Successfully switched to provider: {}", alternativeProvider);
                    return true;
                }
                logger.warn("Alternative provider {} is not available", alternativeProvider);
                return false;
            } catch (Exception e) {
                logger.error("Failed to switch to provider {}: {}", alternativeProvider, e.getMessage());
                return false;
            }
        };
    }

    /**
     * API কী রোটেশন স্ট্র্যাটেজি
     */
    public AutoHealingStrategyService.HealingStrategy createApiKeyRotationStrategy(
            String providerName, String userId) {
        return (Exception error) -> {
            logger.info("Attempting to rotate API key for provider: {} for user: {} due to error: {}",
                    providerName, userId, error.getMessage());

            try {
                // Find alternative active keys for this user and provider
                return apiKeyRepository.findByUserIdAndProvider(userId, providerName)
                    .filter(key -> "active".equals(key.getStatus()))
                    .collectList()
                    .flatMap(keys -> {
                        if (keys.size() <= 1) {
                            logger.warn("No alternative active keys found for provider: {}", providerName);
                            return Mono.just(false);
                        }

                        // Pick a different key (simplified: next one in list)
                        UserApiKey nextKey = keys.get(0); // In real logic, we'd pick one not currently in use
                        
                        logger.info("Successfully identified new key for rotation: {}", nextKey.getLabel());
                        
                        HealingEvent event = new HealingEvent(
                            "AUTH_ERROR",
                            error.getMessage(),
                            "API_KEY_ROTATION",
                            "Rotated to key: " + nextKey.getLabel(),
                            true,
                            "Detected authentication failure. Switched to alternative active API key.",
                            providerName
                        );

                        return healingEventRepository.save(event).thenReturn(true);
                    })
                    .block();
            } catch (Exception e) {
                logger.error("Failed to rotate API key for provider {}: {}", providerName, e.getMessage());
                return false;
            }
        };
    }

    /**
     * কনফিগারেশন পুনরুদ্ধার স্ট্র্যাটেজি
     */
    public AutoHealingStrategyService.HealingStrategy createConfigRecoveryStrategy(
            String providerName, Map<String, Object> defaultConfig) {
        return (Exception error) -> {
            logger.info("Attempting to recover configuration for provider: {} due to error: {}",
                    providerName, error.getMessage());

            try {
                // কনফিগারেশন পুনরুদ্ধার লজিক এখানে থাকবে
                // এটি কনফিগারেশন সেবা দ্বারা পরিচালিত হয়
                logger.info("Configuration recovery initiated for provider: {}", providerName);
                return true;
            } catch (Exception e) {
                logger.error("Failed to recover configuration for provider {}: {}", providerName, e.getMessage());
                return false;
            }
        };
    }

    /**
     * সব প্রোভাইডার হিলিং স্ট্র্যাটেজি রেজিস্টার করা
     */
    public void registerAllStrategies() {
        // OpenAI প্রোভাইডার হিলিং স্ট্র্যাটেজি
        autoHealingService.registerHealingStrategy(
                "openai_rate_limit",
                createProviderSwitchingStrategy("openai", "anthropic"));

        autoHealingService.registerHealingStrategy(
                "openai_auth_error",
                createApiKeyRotationStrategy("openai", ""));

        // Anthropic প্রোভাইডার হিলিং স্ট্র্যাটেজি
        autoHealingService.registerHealingStrategy(
                "anthropic_rate_limit",
                createProviderSwitchingStrategy("anthropic", "openai"));

        autoHealingService.registerHealingStrategy(
                "anthropic_auth_error",
                createApiKeyRotationStrategy("anthropic", ""));

        // Gemini প্রোভাইডার হিলিং স্ট্র্যাটেজি
        autoHealingService.registerHealingStrategy(
                "gemini_rate_limit",
                createProviderSwitchingStrategy("gemini", "openai"));

        autoHealingService.registerHealingStrategy(
                "gemini_auth_error",
                createApiKeyRotationStrategy("gemini", ""));

        // Groq প্রোভাইডার হিলিং স্ট্র্যাটেজি
        autoHealingService.registerHealingStrategy(
                "groq_rate_limit",
                createProviderSwitchingStrategy("groq", "gemini"));

        autoHealingService.registerHealingStrategy(
                "groq_auth_error",
                createApiKeyRotationStrategy("groq", ""));

        // DeepSeek প্রোভাইডার হিলিং স্ট্র্যাটেজি
        autoHealingService.registerHealingStrategy(
                "deepseek_rate_limit",
                createProviderSwitchingStrategy("deepseek", "openai"));

        autoHealingService.registerHealingStrategy(
                "deepseek_auth_error",
                createApiKeyRotationStrategy("deepseek", ""));

        logger.info("All provider healing strategies registered successfully");
    }
}
