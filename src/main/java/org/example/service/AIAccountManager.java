package org.example.service;

import org.example.model.AIAccount;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all AI Accounts
 * - CRUD operations for accounts
 * - Account selection based on budget/quota/rate limit
 * - Fallback chain management
 * - Auto-rotation on ban/suspension
 */
public class AIAccountManager {
    private final Map<String, List<AIAccount>> accountsByProvider;  // "GPT-4" -> [account1, account2]
    private final FirebaseService firebaseService;
    
    public AIAccountManager(FirebaseService firebase) {
        this.accountsByProvider = new HashMap<>();
        this.firebaseService = firebase;
        loadAccountsFromFirebase();
    }
    
    // ========== LOAD & INITIALIZATION ==========
    
    /**
     * Load all accounts from Firebase
     */
    private void loadAccountsFromFirebase() {
        // TODO: Implement Firebase loading
        System.out.println("📦 Loading accounts from Firebase...");
        // firebaseService.getCollection("ai_accounts")
        //     .addSnapshotListener((snapshot, error) -> {
        //         if (error != null || snapshot == null) return;
        //         
        //         for (DocumentSnapshot doc : snapshot.getDocuments()) {
        //             AIAccount account = doc.toObject(AIAccount.class);
        //             addAccountToMemory(account);
        //         }
        //         System.out.println("✅ Loaded " + getTotalAccountCount() + " accounts");
        //     });
    }
    
    private void addAccountToMemory(AIAccount account) {
        String provider = account.getProvider();
        accountsByProvider.computeIfAbsent(provider, k -> new ArrayList<>()).add(account);
        // Sort by priority (1 = highest priority)
        accountsByProvider.get(provider).sort(Comparator.comparingInt(AIAccount::getPriority));
    }
    
    // ========== CREATE / ADD ACCOUNT ==========
    
    /**
     * Add new AI account
     * For example: Add 2nd GPT-4 account
     */
    public void addAccount(AIAccount account) throws Exception {
        // Validate
        if (account.getAccountId() == null || account.getAccountId().isEmpty()) {
            throw new Exception("❌ Account ID cannot be empty");
        }
        
        if (getAccount(account.getAccountId()) != null) {
            throw new Exception("❌ Account already exists: " + account.getAccountId());
        }
        
        // Add to memory
        addAccountToMemory(account);
        
        // Save to Firebase
        saveAccountToFirebase(account);
        
        System.out.println("✅ Added account: " + account.getAccountId());
    }
    
    private void saveAccountToFirebase(AIAccount account) {
        // TODO: Implement Firebase save
        // firebaseService.getCollection("ai_accounts")
        //     .document(account.getAccountId())
        //     .set(account)
        //     .addOnSuccessListener(...)
        //     .addOnFailureListener(...);
    }
    
    // ========== GET / RETRIEVE ==========
    
    /**
     * Get specific account by ID
     */
    public AIAccount getAccount(String accountId) {
        for (List<AIAccount> accounts : accountsByProvider.values()) {
            for (AIAccount account : accounts) {
                if (account.getAccountId().equals(accountId)) {
                    return account;
                }
            }
        }
        return null;
    }
    
    /**
     * Get all accounts for a provider
     */
    public List<AIAccount> getAccountsForProvider(String provider) {
        return accountsByProvider.getOrDefault(provider, new ArrayList<>());
    }
    
    /**
     * Get all active accounts for a provider (sorted by priority)
     */
    public List<AIAccount> getActiveAccountsForProvider(String provider) {
        return getAccountsForProvider(provider).stream()
            .filter(AIAccount::isActive)
            .filter(acc -> acc.getStatus() == AIAccount.AccountStatus.ACTIVE)
            .sorted(Comparator.comparingInt(AIAccount::getPriority))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all accounts
     */
    public List<AIAccount> getAllAccounts() {
        return accountsByProvider.values().stream()
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }
    
    /**
     * Get account count by provider
     */
    public int getAccountCountForProvider(String provider) {
        return getAccountsForProvider(provider).size();
    }
    
    public int getTotalAccountCount() {
        return getAllAccounts().size();
    }
    
    // ========== BEST ACCOUNT SELECTION ==========
    
    /**
     * 🎯 SMART SELECTION: Find best available account for a request
     * 
     * Priority:
     * 1. Budget available + Quota available
     * 2. Rate limit not exceeded
     * 3. Not banned or suspended
     * 4. Highest health score
     * 
     * If none available → return null (or fallback)
     */
    public AIAccount selectBestAccount(String provider) {
        List<AIAccount> candidates = getActiveAccountsForProvider(provider);
        
        if (candidates.isEmpty()) {
            System.out.println("⚠️ No active accounts for provider: " + provider);
            return null;
        }
        
        // 1. Filter: Budget available
        List<AIAccount> budgetOK = candidates.stream()
            .filter(AIAccount::isBudgetAvailable)
            .collect(Collectors.toList());
        
        if (budgetOK.isEmpty()) {
            System.out.println("❌ No accounts with budget available for: " + provider);
            return null;
        }
        
        // 2. Filter: Quota available
        List<AIAccount> quotaOK = budgetOK.stream()
            .filter(AIAccount::isQuotaAvailable)
            .collect(Collectors.toList());
        
        if (quotaOK.isEmpty()) {
            System.out.println("❌ No accounts with quota available for: " + provider);
            return null;
        }
        
        // 3. Filter: Rate limit OK
        List<AIAccount> rateLimitOK = quotaOK.stream()
            .filter(AIAccount::canMakeRequest)
            .collect(Collectors.toList());
        
        if (rateLimitOK.isEmpty()) {
            System.out.println("⏱️ All accounts hit rate limit for: " + provider);
            return null;
        }
        
        // 4. Sort by health score (highest first) and priority
        AIAccount best = rateLimitOK.stream()
            .max(Comparator.comparingInt((AIAccount a) -> a.getHealthScore())
                          .thenComparingInt(AIAccount::getPriority))
            .orElse(null);
        
        if (best != null) {
            System.out.println("✅ Selected account: " + best.getAccountId() 
                             + " (health: " + best.getHealthScore() + "%)");
        }
        
        return best;
    }
    
    /**
     * Get fallback chain for a provider
     * For auto-rotation on failure
     */
    public List<AIAccount> getFallbackChain(String provider) {
        return getActiveAccountsForProvider(provider);
    }
    
    /**
     * Get next available account (for fallback on current account failure)
     */
    public AIAccount getNextAvailableAccount(String provider, String currentAccountId) {
        List<AIAccount> fallback = getFallbackChain(provider);
        
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
    
    // ========== USAGE TRACKING ==========
    
    /**
     * After successful API call
     */
    public void recordSuccess(String accountId, double costIncurred, int responseTimeMs) {
        AIAccount account = getAccount(accountId);
        if (account != null) {
            account.recordSuccess(costIncurred, responseTimeMs);
            updateAccountInFirebase(account);
            
            // Check if budget exceeded
            if (account.getBudgetUsed() >= account.getBudgetLimit() && account.getBudgetLimit() > 0) {
                account.ban("Budget limit reached: $" + account.getBudgetLimit());
                System.out.println("🚫 Account banned - budget exceeded: " + accountId);
            }
        }
    }
    
    /**
     * After failed API call
     */
    public void recordFailure(String accountId, String reason) {
        AIAccount account = getAccount(accountId);
        if (account != null) {
            account.recordFailure();
            
            // Special handling for quota exceeded
            if (reason.contains("quota") || reason.contains("exceeded")) {
                account.ban("Quota exceeded: " + reason);
                System.out.println("🚫 Account banned - quota exceeded: " + accountId);
            }
            
            updateAccountInFirebase(account);
        }
    }
    
    private void updateAccountInFirebase(AIAccount account) {
        // TODO: Update Firebase
        // firebaseService.getCollection("ai_accounts")
        //     .document(account.getAccountId())
        //     .update(...);
    }
    
    // ========== ACCOUNT MANAGEMENT ==========
    
    /**
     * Suspend account (manual)
     */
    public void suspendAccount(String accountId, String reason) throws Exception {
        AIAccount account = getAccount(accountId);
        if (account == null) throw new Exception("Account not found: " + accountId);
        
        account.suspend(reason);
        updateAccountInFirebase(account);
        System.out.println("⏸️ Suspended account: " + accountId);
    }
    
    /**
     * Ban account (usually automatic)
     */
    public void banAccount(String accountId, String reason) throws Exception {
        AIAccount account = getAccount(accountId);
        if (account == null) throw new Exception("Account not found: " + accountId);
        
        account.ban(reason);
        updateAccountInFirebase(account);
        System.out.println("🚫 Banned account: " + accountId + " (" + reason + ")");
    }
    
    /**
     * Reactivate account
     */
    public void reactivateAccount(String accountId) throws Exception {
        AIAccount account = getAccount(accountId);
        if (account == null) throw new Exception("Account not found: " + accountId);
        
        account.reactivate();
        updateAccountInFirebase(account);
        System.out.println("✅ Reactivated account: " + accountId);
    }
    
    /**
     * Delete account
     */
    public void deleteAccount(String accountId) throws Exception {
        AIAccount account = getAccount(accountId);
        if (account == null) throw new Exception("Account not found: " + accountId);
        
        String provider = account.getProvider();
        accountsByProvider.get(provider).remove(account);
        
        // Delete from Firebase
        // firebaseService.getCollection("ai_accounts")
        //     .document(accountId)
        //     .delete();
        
        System.out.println("🗑️ Deleted account: " + accountId);
    }
    
    /**
     * Auto-unblock accounts (run periodically)
     */
    public void checkAndUnblockAccounts() {
        int unblocked = 0;
        for (AIAccount account : getAllAccounts()) {
            if (account.shouldAutoUnblock()) {
                account.reactivate();
                updateAccountInFirebase(account);
                unblocked++;
                System.out.println("🔓 Auto-unblocked: " + account.getAccountId());
            }
        }
        if (unblocked > 0) {
            System.out.println("✅ Auto-unblocked " + unblocked + " accounts");
        }
    }
    
    // ========== REPORTING ==========
    
    /**
     * Get health report for all accounts
     */
    public void printHealthReport() {
        System.out.println("\n📊 ===== ACCOUNT HEALTH REPORT =====");
        
        for (Map.Entry<String, List<AIAccount>> entry : accountsByProvider.entrySet()) {
            String provider = entry.getKey();
            List<AIAccount> accounts = entry.getValue();
            
            System.out.println("\n🔹 " + provider + " (" + accounts.size() + " accounts)");
            
            for (AIAccount acc : accounts) {
                System.out.printf("  - %s: Health=%d%% | Budget=%.2f/%.2f | Quota=%d/%d | Status=%s\n",
                    acc.getAccountId(),
                    acc.getHealthScore(),
                    acc.getBudgetUsed(),
                    acc.getBudgetLimit(),
                    (int)acc.getMonthlyUsed(),
                    (int)acc.getMonthlyQuota(),
                    acc.getStatus()
                );
            }
        }
    }
}
