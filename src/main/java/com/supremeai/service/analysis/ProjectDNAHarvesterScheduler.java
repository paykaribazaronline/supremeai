package com.supremeai.service.analysis;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task for autonomous project DNA harvesting. Ensures the system stays up-to-date with
 * its own codebase changes.
 */
@Component
public class ProjectDNAHarvesterScheduler {

  private static final Logger log = LoggerFactory.getLogger(ProjectDNAHarvesterScheduler.class);

  @Autowired private ProjectDNAHarvesterService harvesterService;

  /** Run harvesting on startup. */
  @PostConstruct
  public void onStartup() {
    log.info("🚀 Triggering initial Project DNA Harvest on startup...");
    harvesterService.harvestDNA().subscribe();
  }

  /** Run harvesting daily at 4 AM to capture recent architectural changes. */
  @Scheduled(cron = "0 0 4 * * *")
  public void scheduledHarvest() {
    log.info("⏰ Running scheduled Project DNA Harvest...");
    harvesterService.harvestDNA().subscribe();
  }
}
