package com.supremeai.learning.active;

import com.supremeai.learning.knowledge.GlobalKnowledgeBase;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActiveLearnerCron {

    private final ActiveInternetScraper scraper;
    private final GlobalKnowledgeBase knowledgeBase;

    public ActiveLearnerCron(ActiveInternetScraper scraper, GlobalKnowledgeBase knowledgeBase) {
        this.scraper = scraper;
        this.knowledgeBase = knowledgeBase;
    }

    /**
     * Runs passively in the background every night at 2:00 AM.
     * It goes to the internet, finds new trending problems, and pre-learns them!
     */
    @Scheduled(cron = "0 0 2 * * *") 
    public void nightlyInternetLearning() {
        System.out.println("[Active Learning] Waking up at 2 AM to learn from the internet...");
        
        List<ScrapedIssue> trendingIssues = scraper.scrapeTrendingIssues();
        
        for (ScrapedIssue issue : trendingIssues) {
            // Store the scraped knowledge into our memory proactively
            knowledgeBase.recordSuccess(
                issue.titleOrError, 
                issue.potentialSolution, 
                "InternetScraper(" + issue.source + ")"
            );
        }
        
        System.out.println("[Active Learning] Finished learning " + trendingIssues.size() + " new trending issues while users were sleeping.");
    }
}