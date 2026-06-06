package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * KnowledgeVerificationScheduler - A scheduled task to periodically verify the integrity of the
 * system's foundational knowledge and alert administrators if issues are found.
 */
@Service
public class KnowledgeVerificationScheduler {

  private static final Logger log = LoggerFactory.getLogger(KnowledgeVerificationScheduler.class);

  @Autowired private KnowledgeVerificationService verificationService;

  @Value("${foundation.knowledge.min-confidence:0.90}")
  private double minConfidenceThreshold;

  @Value("${learning.purge.threshold:0.70}") // নতুন প্রপার্টি: লার্নিং মুছে ফেলার জন্য সর্বনিম্ন
  // কনফিডেন্স থ্রেশহোল্ড
  private double purgeConfidenceThreshold;

  /**
   * Scheduled task to run foundation knowledge verification. Runs every hour at minute 0 (e.g.,
   * 01:00, 02:00, etc.). The cron expression "0 0 * * * ?" means: - Second: 0 - Minute: 0 - Hour:
   * Every hour (*) - Day of Month: Every day of the month (*) - Month: Every month (*) - Day of
   * Week: Every day of the week (?)
   */
  @Scheduled(cron = "${foundation.knowledge.verification.cron:0 0 * * * ?}")
  public void scheduledFoundationVerification() {
    log.info("Starting scheduled foundation knowledge verification...");

    verificationService
        .verifyFoundationKnowledge()
        .subscribe(
            results -> {
              String overallStatus = (String) results.get("overall_status");
              if ("FAIL".equals(overallStatus)) {
                log.error(
                    "🚨 [ADMIN_ALERT] Foundation knowledge verification FAILED! Details: {}",
                    results);
                // In a real system, this would trigger an actual alert (e.g., PagerDuty, email,
                // Slack)
              } else {
                log.info(
                    "✅ Scheduled foundation knowledge verification PASSED. Details: {}", results);
              }
            },
            error -> {
              log.error(
                  "❌ Error during scheduled foundation knowledge verification: {}",
                  error.getMessage(),
                  error);
            });
  }

  /**
   * শিডিউলড টাস্ক: কনফিডেন্স স্কোর থ্রেশহোল্ডের নিচে থাকা লার্নিংগুলো মুছে ফেলা। প্রতিদিন ভোর ৩টায়
   * (ডিফল্ট) এটি রান করবে।
   */
  @Scheduled(cron = "${learning.purge.cron:0 0 3 * * ?}")
  public void scheduledPurgeInvalidLearnings() {
    log.info(
        "শিডিউলড টাস্ক: ইনভ্যালিড লার্নিং মুছে ফেলা হচ্ছে (থ্রেশহোল্ড: {})",
        purgeConfidenceThreshold);
    verificationService.purgeInvalidLearnings(purgeConfidenceThreshold);
  }
}
