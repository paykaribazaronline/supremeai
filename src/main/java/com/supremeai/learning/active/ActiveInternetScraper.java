package com.supremeai.learning.active;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulates scraping StackOverflow, GitHub Issues, etc. in the background
 * to proactively learn solutions before users even encounter the errors.
 */
@Service
public class ActiveInternetScraper {

    // In reality, this would connect to external APIs/web scrapers
    public List<ScrapedIssue> scrapeTrendingIssues() {
        List<ScrapedIssue> trending = new ArrayList<>();
        
        // Mocked trending issues from the internet
        trending.add(new ScrapedIssue(
            "java.lang.OutOfMemoryError: Java heap space in Spring Boot 3", 
            "Increase -Xmx flag or check for memory leaks in new Spring Boot 3 autoconfigurations",
            "StackOverflow"
        ));
        
        trending.add(new ScrapedIssue(
            "React useEffect infinite loop warning", 
            "Ensure dependency array is correctly populated and avoid passing objects directly",
            "GitHub Issues"
        ));

        return trending;
    }
}

class ScrapedIssue {
    public String titleOrError;
    public String potentialSolution;
    public String source;

    public ScrapedIssue(String title, String solution, String source) {
        this.titleOrError = title;
        this.potentialSolution = solution;
        this.source = source;
    }
}