package com.supremeai.config;

import com.supremeai.model.*;
import com.supremeai.repository.*;
import com.supremeai.repository.ProviderRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * SeedDataValidator — validates that real seed data exists in Firestore at startup.
 *
 * <p>Run with: --spring.profiles.active=seed-validation OR just start the app normally (runs on
 * every non-local/non-test profile by default).
 *
 * <p>What it checks: ✅ At least MIN_USERS users exist in Firestore ✅ All MIN_PROVIDERS AI providers
 * are registered ✅ User tier records (FREE/PRO/ADMIN) exist ✅ At least 5 knowledge domains
 * registered ✅ System-learning collection has records ✅ All 3 knowledge domains present for system
 * context (adding more for quality)
 *
 * <p>If a check fails → logs WARN, does NOT block startup. Real writes happen via scripts/seed/*.js
 * (Node.js, uses Firebase Admin SDK).
 */
@Component
@Profile("!local & !test & !sandbox")
public class SeedDataValidator implements CommandLineRunner {

  private final UserRepository userRepository;
  private final ProviderRepository apiProviderRepository;
  private final UserTierRepository userTierRepository;
  private final KnowledgeDomainRepository knowledgeDomainRepository;
  private final SystemLearningRepository systemLearningRepository;
  private final KnowledgeEntryRepository knowledgeEntryRepository;
  private final ProviderTaskPerformanceRepository perfRepository;
  private final WorkflowDefinitionRepository workflowRepository;

  public SeedDataValidator(
      UserRepository userRepository,
      ProviderRepository apiProviderRepository,
      UserTierRepository userTierRepository,
      KnowledgeDomainRepository knowledgeDomainRepository,
      SystemLearningRepository systemLearningRepository,
      KnowledgeEntryRepository knowledgeEntryRepository,
      ProviderTaskPerformanceRepository perfRepository,
      WorkflowDefinitionRepository workflowRepository) {
    this.userRepository = userRepository;
    this.apiProviderRepository = apiProviderRepository;
    this.userTierRepository = userTierRepository;
    this.knowledgeDomainRepository = knowledgeDomainRepository;
    this.systemLearningRepository = systemLearningRepository;
    this.knowledgeEntryRepository = knowledgeEntryRepository;
    this.perfRepository = perfRepository;
    this.workflowRepository = workflowRepository;
  }

  @Override
  public void run(String... args) throws Exception {
    System.out.println("\n[SeedDataValidator] 🔍 Checking seed data health...\n");

    int score = 0, maxScore = 7;
    boolean didWarn = false;

    // 1. Users
    long userCount = userRepository.count().block();
    if (userCount >= 3) {
      System.out.printf("[OK] Users: %d records (min 3)%n", userCount);
      score++;
    } else {
      System.out.printf(
          "[WARN] Users: %d found — run: node scripts/seed/seed-all-data.js --execute%n",
          userCount);
      didWarn = true;
    }

    // 2. AI Providers
    long providerCount = apiProviderRepository.count().block();
    if (providerCount >= 5) {
      System.out.printf("[OK] AI Providers: %d records (min 5)%n", providerCount);
      score++;
    } else {
      System.out.printf(
          "[WARN] AI Providers: %d found — run: node scripts/seed/seed-ai-providers.js --execute%n",
          providerCount);
      didWarn = true;
    }

    // 3. User Tiers
    long tierCount = userTierRepository.count().block();
    if (tierCount >= 3) {
      System.out.printf("[OK] User Tiers: %d records (FREE/PRO/ADMIN)%n", tierCount);
      score++;
    } else {
      System.out.printf(
          "[WARN] User Tiers: %d found — run: node scripts/seed/seed-all-data.js --execute%n",
          tierCount);
      didWarn = true;
    }

    // 4. Knowledge Domains
    long domainCount = knowledgeDomainRepository.count().block();
    if (domainCount >= 5) {
      System.out.printf("[OK] Knowledge Domains: %d records%n", domainCount);
      score++;
    } else {
      System.out.printf(
          "[WARN] Knowledge Domains: %d found — run: node scripts/seed/seed-all-data.js --execute%n",
          domainCount);
      didWarn = true;
    }

    // 5. System Learning
    long learningCount = systemLearningRepository.count().block();
    if (learningCount >= 5) {
      System.out.printf("[OK] System Learning: %d records%n", learningCount);
      score++;
    } else {
      System.out.printf(
          "[WARN] System Learning: %d found — run: node scripts/seed/seed-all-data.js --execute%n",
          learningCount);
      didWarn = true;
    }

    // 6. Knowledge Entries
    long entryCount = knowledgeEntryRepository.count().block();
    if (entryCount >= 5) {
      System.out.printf("[OK] Knowledge Entries: %d records%n", entryCount);
      score++;
    } else {
      System.out.printf(
          "[WARN] Knowledge Entries: %d found — run: node scripts/seed/seed-all-data.js --execute%n",
          entryCount);
      didWarn = true;
    }

    // 7. Provider Performance
    long perfCount = perfRepository.count().block();
    if (perfCount >= 4) {
      System.out.printf("[OK] Provider Performance: %d records%n", perfCount);
      score++;
    } else {
      System.out.printf(
          "[WARN] Provider Performance: %d found — run: node scripts/seed/seed-all-data.js --execute%n",
          perfCount);
      didWarn = true;
    }

    // 8. Workflow Definitions
    long wfCount = workflowRepository.count().block();
    if (wfCount >= 1) {
      System.out.printf("[OK] Workflow Definitions: %d records%n", wfCount);
      score++;
    } else {
      System.out.printf("[INFO] Workflow Definitions: %d found (optional)%n", wfCount);
      score++;
    }

    System.out.printf("%n[SeedDataValidator] Health score: %d/%d%n", score, maxScore);
    if (didWarn) {
      System.out.println("[SeedDataValidator] ⚠️  Some seed data is missing or below threshold.");
      System.out.println("   Run: npm run seed:all   (from scripts/ directory) to fix\n");
    } else {
      System.out.println("[SeedDataValidator] ✅ All seed data checks passed\n");
    }
  }
}
