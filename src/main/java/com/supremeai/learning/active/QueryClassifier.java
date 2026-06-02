package com.supremeai.learning.active;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class QueryClassifier {

    public QueryClassification classify(String domainName, List<String> keywords) {
        String fullQuery = (domainName + " " + String.join(" ", keywords)).toLowerCase();
        
        boolean isCode = fullQuery.contains("error") || fullQuery.contains("exception") || 
                         fullQuery.contains("bug") || fullQuery.contains("code") ||
                         fullQuery.contains("java") || fullQuery.contains("python");
                         
        boolean isResearch = fullQuery.contains("paper") || fullQuery.contains("research") || 
                             fullQuery.contains("algorithm") || fullQuery.contains("arxiv") ||
                             fullQuery.contains("machine learning");
                             
        boolean isWebDev = fullQuery.contains("html") || fullQuery.contains("css") || 
                           fullQuery.contains("javascript") || fullQuery.contains("mdn") ||
                           fullQuery.contains("react") || fullQuery.contains("frontend");
                           
        return new QueryClassification(isCode, isResearch, isWebDev);
    }

    public static class QueryClassification {
        private final boolean codeRelated;
        private final boolean researchRelated;
        private final boolean webDevRelated;

        public QueryClassifier(boolean codeRelated, boolean researchRelated, boolean webDevRelated) {
            this.codeRelated = codeRelated;
            this.researchRelated = researchRelated;
            this.webDevRelated = webDevRelated;
        }

        public boolean isCodeRelated() { return codeRelated; }
        public boolean isResearchRelated() { return researchRelated; }
        public boolean isWebDevRelated() { return webDevRelated; }
    }
}
