package org.example.service;

import org.example.model.AIAccount;
import java.util.*;

/**
 * Budget Manager - Enforces $0 Hard Stop
 * 
 * When budget is reached:
 * 1. Ban the account
 * 2. Rotate to next available account
 * 3. If no accounts available → STOP processing
 * 4. Alert admin via email/dashboard
 */
public class BudgetManager {
    private final AIAccountManager accountManager;
    private final FirebaseService firebaseService;
    
    // Alert thresholds
    private static final double WARNING_THRESHOLD = 0.80;  // Alert at 80% spend
    private static final double CRITICAL_THRESHOLD = 0.95; // Alert at 95% spend
    
    public BudgetManager(AIAccountManager accountManager, FirebaseService firebase) {
        this.accountManager = accountManager;
        this.firebaseService = firebase;
    }
    
    /**
     * ⚡ CORE METHOD: Check if request can proceed
     * Also handles quota + rate limiting
     */
    public boolean canProceedWithRequest(String provider) {
        // 1. Check if any active accounts exist
        List<AIAccount> activeAccounts = accountManager.getActiveAccountsForProvider(provider);
        if (activeAccounts.isEmpty()) {
            System.out.println("🛑 HARD STOP: No active accounts for " + provider);
            alertAdminBudgetCritical(provider, "No accounts available");
            return false;
        }
        
        // 2. Filter accounts that have budget
        List<AIAccount> accountsWithBudget = new ArrayList<>();
        for (AIAccount acc : activeAccounts) {
            if (acc.isBudgetAvailable() && acc.isQuotaAvailable() && acc.canMakeRequest()) {
                accountsWithBudget.add(acc);
            }
        }
        
        if (accountsWithBudget.isEmpty()) {
            System.out.println("🛑 HARD STOP: All " + provider + " accounts exceeded budget/quota");
            
            // Auto-ban all accounts
            for (AIAccount acc : activeAccounts) {
                if (!acc.isBanned()) {
                    try {
                        accountManager.banAccount(acc.getAccountId(), "Budget/Quota exceeded - Auto-ban");
                    } catch (Exception e) {
                        System.err.println("Error banning account: " + e.getMessage());
                    }
                }
            }
            
            alertAdminBudgetCritical(provider, "All accounts exhausted");
            return false;
        }
        
        return true;
    }
    
    /**
     * Get total spend across all accounts for a provider
     */
    public double getTotalSpendForProvider(String provider) {
        return accountManager.getAccountsForProvider(provider).stream()
            .mapToDouble(AIAccount::getBudgetUsed)
            .sum();
    }
    
    /**
     * Get total budget limit across all accounts
     */
    public double getTotalBudgetForProvider(String provider) {
        return accountManager.getAccountsForProvider(provider).stream()
            .mapToDouble(AIAccount::getBudgetLimit)
            .sum();
    }
    
    /**
     * Get available budget remaining
     */
    public double getAvailableBudgetForProvider(String provider) {
        return accountManager.getActiveAccountsForProvider(provider).stream()
            .mapToDouble(AIAccount::getBudgetRemaining)
            .sum();
    }
    
    /**
     * Check spend percentage (0-100%)
     */
    public double getSpendPercentageForProvider(String provider) {
        double total = getTotalBudgetForProvider(provider);
        if (total == 0) return 0;
        
        double spent = getTotalSpendForProvider(provider);
        return (spent / total) * 100;
    }
    
    /**
     * Check account health and send alerts if needed
     */
    public void checkAndAlertBudgetStatus(String provider) {
        double percentage = getSpendPercentageForProvider(provider);
        List<AIAccount> accounts = accountManager.getAccountsForProvider(provider);
        
        System.out.println("\n💰 Budget Status for " + provider + ": " + String.format("%.1f%%", percentage));
        
        // Critical alert (95%+)
        if (percentage >= CRITICAL_THRESHOLD) {
            alertAdminBudgetCritical(provider, String.format("%.0f%% spent", percentage));
            
            // Ban accounts approaching limit
            for (AIAccount acc : accounts) {
                double accPercentage = (acc.getBudgetUsed() / acc.getBudgetLimit()) * 100;
                if (accPercentage >= 90) {
                    try {
                        accountManager.banAccount(acc.getAccountId(), "Approaching budget limit - " + String.format("%.0f%%", accPercentage));
                    } catch (Exception e) {
                        System.err.println("Error banning account: " + e.getMessage());
                    }
                }
            }
        }
        // Warning alert (80%+)
        else if (percentage >= WARNING_THRESHOLD) {
            alertAdminBudgetWarning(provider, String.format("%.0f%% spent", percentage));
        }
    }
    
    /**
     * Rotate to next available account on current one's failure
     */
    public AIAccount getNextAvailableAccount(String provider, String currentAccountId) {
        List<AIAccount> fallback = accountManager.getFallbackChain(provider);
        
        for (AIAccount acc : fallback) {
            if (!acc.getAccountId().equals(currentAccountId) && 
                acc.isActive() && 
                acc.isBudgetAvailable() && 
                acc.isQuotaAvailable()) {
                
                System.out.println("🔄 Rotating to account: " + acc.getAccountId());
                return acc;
            }
        }
        
        System.out.println("❌ No fallback accounts available for rotation");
        return null;
    }
    
    /**
     * Monitor all providers periodically
     * Run via scheduled task every 5 minutes
     */
    public void monitorAllProviders() {
        Set<String> providers = new HashSet<>();
        for (AIAccount acc : accountManager.getAllAccounts()) {
            providers.add(acc.getProvider());
        }
        
        for (String provider : providers) {
            checkAndAlertBudgetStatus(provider);
            accountManager.checkAndUnblockAccounts();  // Auto-unblock after 30 min
        }
    }
    
    /**
     * System status check
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        Set<String> providers = new HashSet<>();
        for (AIAccount acc : accountManager.getAllAccounts()) {
            providers.add(acc.getProvider());
        }
        
        Map<String, Map<String, Object>> providerStatus = new HashMap<>();
        
        for (String provider : providers) {
            Map<String, Object> pStatus = new HashMap<>();
            pStatus.put("total_accounts", accountManager.getAccountCountForProvider(provider));
            pStatus.put("active_accounts", accountManager.getActiveAccountsForProvider(provider).size());
            pStatus.put("total_spent", getTotalSpendForProvider(provider));
            pStatus.put("total_budget", getTotalBudgetForProvider(provider));
            pStatus.put("spend_percentage", getSpendPercentageForProvider(provider));
            pStatus.put("can_proceed", canProceedWithRequest(provider));
            
            providerStatus.put(provider, pStatus);
        }
        
        status.put("providers", providerStatus);
        status.put("total_accounts", accountManager.getTotalAccountCount());
        status.put("timestamp", new Date());
        
        return status;
    }
    
    // ========== ALERTS ==========
    
    /**
     * Send warning alert (80-95%)
     */
    private void alertAdminBudgetWarning(String provider, String details) {
        System.out.println("⚠️ WARNING: " + provider + " budget warning - " + details);
        
        // TODO: Send email alert
        // emailService.sendAlert(
        //     "admin@supremeai.com",
        //     "Budget Warning: " + provider,
        //     "⚠️ " + provider + " has reached " + details + ". Please review account status."
        // );
        
        // TODO: Save to Firebase alerts
        // firebaseService.logAlert({
        //     type: "BUDGET_WARNING",
        //     provider: provider,
        //     details: details,
        //     timestamp: new Date()
        // });
    }
    
    /**
     * Send critical alert (95%+)
     */
    private void alertAdminBudgetCritical(String provider, String details) {
        System.out.println("🚨 CRITICAL: " + provider + " budget CRITICAL - " + details);
        
        // TODO: Send urgent email alert
        // emailService.sendUrgentAlert(
        //     "admin@supremeai.com",
        //     "🚨 CRITICAL: " + provider + " Budget Exhausted!",
        //     "CRITICAL: " + provider + " - " + details + ". Processing may be blocked. Immediate action required!"
        // );
        
        // TODO: Save critical alert to Firebase
        // firebaseService.logCriticalAlert({
        //     type: "BUDGET_CRITICAL",
        //     provider: provider,
        //     details: details,
        //     timestamp: new Date(),
        //     automatic_action: "Accounts banned, processing blocked"
        // });
    }
    
    /**
     * Print full budget report
     */
    public void printBudgetReport() {
        System.out.println("\n💵 ===== BUDGET REPORT =====");
        
        Map<String, Object> status = getSystemStatus();
        
        @SuppressWarnings("unchecked")
        Map<String, Map<String, Object>> providers = 
            (Map<String, Map<String, Object>>) status.get("providers");
        
        for (Map.Entry<String, Map<String, Object>> entry : providers.entrySet()) {
            String provider = entry.getKey();
            Map<String, Object> pStatus = entry.getValue();
            
            System.out.printf("\n%s\n", provider);
            System.out.printf("  Accounts: %d active / %d total\n", 
                pStatus.get("active_accounts"), 
                pStatus.get("total_accounts"));
            System.out.printf("  Budget: $%.2f / $%.2f (%.1f%%)\n", 
                pStatus.get("total_spent"),
                pStatus.get("total_budget"),
                pStatus.get("spend_percentage"));
            System.out.printf("  Can Proceed: %s\n", pStatus.get("can_proceed"));
        }
    }
}
