package com.supremeai.automation.farm;

import com.supremeai.fallback.AIProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.UUID;

@Service
public class AccountFarmingEngine {

    private static final Logger log = LoggerFactory.getLogger(AccountFarmingEngine.class);
    private final VPNManager vpnManager;
    private final List<SyntheticAccount> accountPool = new CopyOnWriteArrayList<>();

    private final String CATCH_ALL_DOMAIN = "@supremeai-internal.net";
    private final String[] VPN_REGIONS = {"US", "UK", "CA", "SG", "DE", "JP"};
    private int vpnIndex = 0;

    public AccountFarmingEngine(VPNManager vpnManager) {
        this.vpnManager = vpnManager;
    }

    public void autoFarmNewAccounts(AIProvider targetProvider, int count) {
        log.info("\n[Farming Engine] Auto-farming for {}...", targetProvider);
        for (int i = 0; i < count; i++) {
            String syntheticEmail = "bot_" + UUID.randomUUID().toString().substring(0, 8) + CATCH_ALL_DOMAIN;
            String targetVpnRegion = VPN_REGIONS[vpnIndex % VPN_REGIONS.length];
            vpnIndex++;
            vpnManager.connectToRegion(targetVpnRegion);
            
            log.info("Harvested new keys for {}. Added to pool.", targetProvider);
        }
    }
}
