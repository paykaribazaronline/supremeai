package org.example.service;

import org.example.model.AIAccount;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages all AI Accounts
 * - Automatically loads keys from Environment Variables (Render/Cloud)
 * - Supports multi-account rotation
 */
public class AIAccountManager {
    private final Map<String, List<AIAccount>> accountsByProvider;
    private final FirebaseService firebaseService;
    
    public AIAccountManager(FirebaseService firebase) {
        this.accountsByProvider = new HashMap<>();
        this.firebaseService = firebase;
        loadAccountsFromEnvironment(); // 🚀 NEW: Load from Render Env
        loadAccountsFromFirebase();
    }
    
    /**
     * 🌐 NEW: Load accounts directly from Environment Variables
     * This ensures the system works immediately on Render/Cloud.
     */
    private void loadAccountsFromEnvironment() {
        System.out.println("🌐 Scanning Environment Variables for AI Keys...");
        
        Map<String, String> env = System.getenv();
        
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            if (value == null || value.isEmpty() || value.equals("..........")) continue;

            String provider = null;
            if (key.startsWith("GEMINI_API_KEY")) provider = "GEMINI";
            else if (key.startsWith("GROQ_API_KEY")) provider = "GROQ";
            else if (key.startsWith("DEEPSEEK_API_KEY")) provider = "DEEPSEEK";
            else if (key.startsWith("NVIDIA_API_KEY")) provider = "NVIDIA";
            else if (key.startsWith("HUGGINGFACE_API_KEY")) provider = "HUGGINGFACE";
            else if (key.startsWith("RECRAFT_API_KEY")) provider = "RECRAFT";

            if (provider != null) {
                AIAccount account = new AIAccount(
                    key.toLowerCase(),           // accountId
                    provider,                    // provider
                    provider + " Account",       // accountName
                    value,                       // apiKey
                    "system",                    // createdBy
                    10.0                         // budgetLimit
                );
                account.setStatus(AIAccount.AccountStatus.ACTIVE);
                account.setPriority(1);
                
                addAccountToMemory(account);
                System.out.println("  ✅ Loaded " + provider + " account: " + key);
            }
        }
    }
    
    private void loadAccountsFromFirebase() {
        System.out.println("📦 Loading additional accounts from Firebase...");
        // This will append to accounts already loaded from Env
    }
    
    private void addAccountToMemory(AIAccount account) {
        String provider = account.getProvider();
        accountsByProvider.computeIfAbsent(provider, k -> new ArrayList<>()).add(account);
        accountsByProvider.get(provider).sort(Comparator.comparingInt(AIAccount::getPriority));
    }
    
    // ... rest of the existing methods (getAccount, selectBestAccount, recordSuccess, etc.)

    public AIAccount getAccount(String accountId) {
        for (List<AIAccount> accounts : accountsByProvider.values()) {
            for (AIAccount account : accounts) {
                if (account.getAccountId().equalsIgnoreCase(accountId)) {
                    return account;
                }
            }
        }
        return null;
    }
    
    public List<AIAccount> getAccountsForProvider(String provider) {
        return accountsByProvider.getOrDefault(provider, new ArrayList<>());
    }
    
    public List<AIAccount> getActiveAccountsForProvider(String provider) {
        return getAccountsForProvider(provider).stream()
            .filter(AIAccount::isActive)
            .filter(acc -> acc.getStatus() == AIAccount.AccountStatus.ACTIVE)
            .collect(Collectors.toList());
    }
    
    public List<AIAccount> getAllAccounts() {
        return accountsByProvider.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    public AIAccount selectBestAccount(String provider) {
        List<AIAccount> candidates = getActiveAccountsForProvider(provider);
        if (candidates.isEmpty()) return null;
        
        // Simple rotation/priority logic
        return candidates.get(0); 
    }

    public void recordSuccess(String accountId, double cost, int time) {
        AIAccount acc = getAccount(accountId);
        if (acc != null) acc.recordSuccess(cost, time);
    }

    public void recordFailure(String accountId, String reason) {
        AIAccount acc = getAccount(accountId);
        if (acc != null) acc.recordFailure();
    }
    
    public void banAccount(String accountId, String reason) {
        AIAccount acc = getAccount(accountId);
        if (acc != null) acc.ban(reason);
    }

    public AIAccount getNextAvailableAccount(String provider, String currentId) {
        List<AIAccount> accounts = getActiveAccountsForProvider(provider);
        for (AIAccount acc : accounts) {
            if (!acc.getAccountId().equalsIgnoreCase(currentId)) return acc;
        }
        return null;
    }
}
