package com.supremeai.learning.active;

import com.supremeai.learning.active.ActiveInternetScraper.ScrapedIssue;
import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActiveLearnerCron {

    private static final Logger log = LoggerFactory.getLogger(ActiveLearnerCron.class);
    private final ActiveInternetScraper scraper;
    private final GlobalKnowledgeBase knowledgeBase;

    public ActiveLearnerCron(ActiveInternetScraper scraper, GlobalKnowledgeBase knowledgeBase) {
        this.scraper = scraper;
        this.knowledgeBase = knowledgeBase;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void nightlyInternetLearning() {
        log.info("[Active Learning] Waking up at 2 AM...");
        List<ScrapedIssue> trendingIssues = scraper.scrapeTrendingIssues();

        for (ScrapedIssue issue : trendingIssues) {
            knowledgeBase.recordSuccessWithPermission(
                issue.titleOrError,
                issue.potentialSolution,
                "InternetScraper(" + issue.source + ")",
                100, 
                0.9
            );
        }
        log.info("[Active Learning] Finished learning.");
    }
}
