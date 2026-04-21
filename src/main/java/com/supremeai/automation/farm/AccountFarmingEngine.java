package com.supremeai.automation.farm;

import com.supremeai.fallback.APIKeyManager;
import com.supremeai.fallback.AIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

/**
 * The "Quota Multiplier" Engine.
 * Automatically creates accounts using Catch-All emails, rotates VPNs,
 * extracts API keys, and feeds them back into our AIFallbackOrchestrator.
 */
@Service
public class AccountFarmingEngine {

    private static final Logger log = LoggerFactory.getLogger(AccountFarmingEngine.class);
    private final VPNController vpnController;
    private final APIKeyManager apiKeyManager;
    private final List<SyntheticAccount> accountPool = new CopyOnWriteArrayList<>();

    // A catch-all domain setup (e.g., anything@mycompany.com goes to one inbox)
    private final String CATCH_ALL_DOMAIN = "@supremeai-internal.net";
    
    // Rotating VPN regions to avoid triggering anti-bot protections
    private final String[] VPN_REGIONS = {"US", "UK", "CA", "SG", "DE", "JP"};
    private int vpnIndex = 0;

    public AccountFarmingEngine(VPNController vpnController, APIKeyManager apiKeyManager) {
        this.vpnController = vpnController;
        this.apiKeyManager = apiKeyManager;
    }

    /**
     * Triggers when the system realizes it's running out of free quota.
     * System auto-farms X new accounts to replenish the pool.
     */
    public void autoFarmNewAccounts(AIProvider targetProvider, int count) {
        log.info("\n[Farming Engine] CRITICAL QUOTA WARNING. Initiating auto-farm for {} new {} accounts...", count, targetProvider);

        for (int i = 0; i < count; i++) {
            // 1. Generate unique synthetic email
            String syntheticEmail = "bot_" + UUID.randomUUID().toString().substring(0, 8) + CATCH_ALL_DOMAIN;
            String generatedPassword = "SUpR3m3_" + UUID.randomUUID().toString().substring(0, 8) + "!";
            
            // 2. Rotate VPN to avoid IP Ban (Rate Limit by IP during signup)
            String targetVpnRegion = VPN_REGIONS[vpnIndex % VPN_REGIONS.length];
            vpnIndex++;
            vpnController.connectToRegion(targetVpnRegion);
            
            SyntheticAccount newAccount = new SyntheticAccount(syntheticEmail, generatedPassword, targetVpnRegion);
            
            try {
                // 3. Simulate Selenium/Puppeteer automation to sign up on Groq/HuggingFace website
                log.debug("   -> [Web Automator] Navigating to {} signup page...", targetProvider);
                log.debug("   -> [Web Automator] Bypassing Captcha...");
                log.debug("   -> [Web Automator] Registering with: {}", syntheticEmail);

                // 4. Simulate verifying the email via IMAP/Catch-All
                log.debug("   -> [Email Automator] Reading Catch-All inbox... Found verification link! Clicking...");

                // 5. Simulate extracting the free API Key from the dashboard
                log.debug("   -> [Web Automator] Navigating to API settings... Extracting new Key...");
                String newlyHarvestedKey = "farmed_" + targetProvider.name().toLowerCase() + "_" + UUID.randomUUID().toString().substring(0, 8);

                // Save state
                newAccount.setAssociatedApiKey(newlyHarvestedKey);
                accountPool.add(newAccount);

                // 6. FEED IT BACK INTO OUR SYSTEM!
                // The AI Fallback Orchestrator now has a fresh key to use!
                apiKeyManager.addKey(targetProvider, newlyHarvestedKey);

                log.info("   [SUCCESS] Harvested new Key! Added to Fallback Pool. Total Farmed Accounts: {}", accountPool.size());

            } catch (Exception e) {
                log.error("   [FAILED] Could not farm account for {}. Anti-bot protection triggered?", syntheticEmail, e);
                newAccount.markAsBanned();
            }
        }
    }
}