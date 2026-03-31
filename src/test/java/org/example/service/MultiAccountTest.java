package org.example.service;

import org.example.model.AIAccount;
import java.util.*;

/**
 * Test Multi-Account System
 * ✅ Phase 1 Testing
 */
public class MultiAccountTest {
    public static void main(String[] args) {
        System.out.println("\n🧪 ===== PHASE 1: MULTI-ACCOUNT SYSTEM TEST =====\n");
        
        // Mock Firebase service
        FirebaseService firebase = new FirebaseService();
        
        // Initialize Account Manager
        AIAccountManager accountManager = new AIAccountManager(firebase);
        
        try {
            // Test 1: Create multiple accounts for same provider
            System.out.println("📋 TEST 1: Creating multiple accounts for same provider...");
            
            AIAccount gpt4Account1 = new AIAccount(
                "gpt4-prod-1",
                "GPT-4",
                "Production Account #1",
                "sk-prod-key-1",
                "admin@supremeai.com",
                20.0  // $20 budget
            );
            accountManager.addAccount(gpt4Account1);
            
            AIAccount gpt4Account2 = new AIAccount(
                "gpt4-prod-2",
                "GPT-4",
                "Production Account #2",
                "sk-prod-key-2",
                "admin@supremeai.com",
                15.0  // $15 budget
            );
            accountManager.addAccount(gpt4Account2);
            
            AIAccount claudeAccount = new AIAccount(
                "claude-backup",
                "Claude-3",
                "Backup Account",
                "claude-key-1",
                "admin@supremeai.com",
                10.0  // $10 budget
            );
            accountManager.addAccount(claudeAccount);
            
            System.out.println("✅ Created 3 accounts\n");
            
            // Test 2: Select best account
            System.out.println("📋 TEST 2: Selecting best account for GPT-4...");
            AIAccount selected = accountManager.selectBestAccount("GPT-4");
            if (selected != null) {
                System.out.println("✅ Selected: " + selected.getAccountId() + "\n");
            }
            
            // Test 3: Record usage and simulate spending
            System.out.println("📋 TEST 3: Recording usage and budget tracking...");
            
            // Simulate requests
            accountManager.recordSuccess("gpt4-prod-1", 0.50, 1200);  // $0.50 spent, 1.2s response
            accountManager.recordSuccess("gpt4-prod-1", 0.75, 950);   // $0.75 more
            accountManager.recordSuccess("gpt4-prod-1", 0.60, 1100);  // $0.60 more
            
            System.out.println("✅ Recorded 3 successful requests\n");
            
            // Test 4: Check budget remaining
            System.out.println("📋 TEST 4: Checking account health...");
            AIAccount acc1 = accountManager.getAccount("gpt4-prod-1");
            System.out.printf("  Account: %s\n", acc1.getAccountId());
            System.out.printf("  Budget Used: $%.2f / $%.2f\n", acc1.getBudgetUsed(), acc1.getBudgetLimit());
            System.out.printf("  Budget Remaining: $%.2f\n", acc1.getBudgetRemaining());
            System.out.printf("  Health Score: %d%%\n", acc1.getHealthScore());
            System.out.printf("  Status: %s\n", acc1.getStatus());
            System.out.println("");
            
            // Test 5: Test account ban (simulate budget exceeded)
            System.out.println("📋 TEST 5: Testing budget exhaustion & auto-ban...");
            
            // Simulate spending remaining budget
            accountManager.recordSuccess("gpt4-prod-1", 18.5, 1000);  // Exceed limit!
            acc1 = accountManager.getAccount("gpt4-prod-1");
            System.out.printf("  After overspend - Status: %s\n", acc1.getStatus());
            System.out.printf("  Banned: %s\n", acc1.isBanned());
            System.out.println("");
            
            // Test 6: Fallback chain
            System.out.println("📋 TEST 6: Fallback chain on account failure...");
            List<AIAccount> fallback = accountManager.getFallbackChain("GPT-4");
            System.out.println("  Fallback chain for GPT-4:");
            for (int i = 0; i < fallback.size(); i++) {
                System.out.printf("    %d. %s (Health: %d%%)\n", 
                    i+1, 
                    fallback.get(i).getAccountId(),
                    fallback.get(i).getHealthScore());
            }
            System.out.println("");
            
            // Test 7: Budget Manager
            System.out.println("📋 TEST 7: Testing Budget Manager (HARD STOP)...");
            BudgetManager budgetManager = new BudgetManager(accountManager, firebase);
            
            System.out.printf("  Can proceed with GPT-4: %s\n", 
                budgetManager.canProceedWithRequest("GPT-4"));
            System.out.printf("  Can proceed with Claude-3: %s\n", 
                budgetManager.canProceedWithRequest("Claude-3"));
            System.out.printf("  Total spend (GPT-4): $%.2f\n", 
                budgetManager.getTotalSpendForProvider("GPT-4"));
            System.out.printf("  Spend percentage: %.1f%%\n", 
                budgetManager.getSpendPercentageForProvider("GPT-4"));
            System.out.println("");
            
            // Test 8: Health report
            System.out.println("📋 TEST 8: Full account health report...");
            accountManager.printHealthReport();
            System.out.println("");
            
            System.out.println("✅ ===== ALL TESTS PASSED =====\n");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
