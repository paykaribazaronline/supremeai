package com.supremeai.learning;

import org.springframework.stereotype.Service;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Smart Focus Detection - Analyzes a website URL to auto-detect
 * what topic the admin wants the system to learn from it.
 *
 * Detection is rule-based (keyword matching on domain + URL path)
 * so it works without any external API call.
 */
@Service
public class FocusDetectorService {

    private static final Map<String, List<String>> FOCUS_KEYWORDS = Map.of(
        "marketing", List.of(
            "marketing", "growth", "seo", "ads", "advert", "promo", "brand",
            "social-media", "influencer", "copywriting", "content-writ", "hubspot",
            "mailchimp", "moz", "semrush", "ahrefs"
        ),
        "security_hacking", List.of(
            "hack", "security", "exploit", "vulnerab", "pentest", "cve", "malware",
            "ransomware", "payload", "reverse-engine", "cybersec", "offensive",
            "defensive", "threat", "ctf", "kali", "metasploit", "burp"
        ),
        "ai_research", List.of(
            "ai", "ml", "llm", "gpt", "deep-learning", "neural", "nlp",
            "openai", "anthropic", "huggingface", "model", "transformer",
            "machine-learning", "artificial-intelligence", "generative-ai"
        ),
        "programming", List.of(
            "dev", "code", "program", "software", "engineer", "stackoverflow",
            "github", "gitlab", "docker", "kubernetes", "linux", "server",
            "api", "database", "sql", "nosql", "backend", "frontend",
            "java", "python", "javascript", " Typescript", "go", "rust"
        ),
        "business_startup", List.of(
            "startup", "business", "entrepreneur", "vc", "funding", "saas",
            "product-hunt", "pitch", "investor", "revenue", "biz"
        ),
        "data_science", List.of(
            "data", "analytics", "statistics", "visuali", "pandas", "tableau",
            "powerbi", "bigdata", "etl", "warehouse", "lake"
        ),
        "design_ux", List.of(
            "design", "figma", "ui", "ux", "user-experience", "photoshop",
            "illustrator", "adobe", "sketch", "prototyp"
        ),
        "cloud_devops", List.of(
            "cloud", "aws", "gcp", "azure", "terraform", "ansible", "cicd",
            "jenkins", "github-actions", "devops", "sre", "infra"
        ),
        "blockchain_crypto", List.of(
            "blockchain", "crypto", "bitcoin", "ethereum", "smart-contract",
            "web3", "defi", "nft", "solidity"
        ),
        "mobile_dev", List.of(
            "android", "ios", "flutter", "react-native", "swift", "kotlin",
            "mobile", "app-dev", "xcode", "play-store"
        )
    );

    /**
     * Auto-detect focus area from a URL.
     * Returns "general" if no match found.
     */
    public String detectFocus(String url) {
        if (url == null || url.isEmpty()) return "general";

        String lowerUrl;
        String host;

        try {
            URL parsed = new URL(url);
            host = parsed.getHost() != null ? parsed.getHost().toLowerCase() : "";
            lowerUrl = url.toLowerCase();
        } catch (Exception e) {
            lowerUrl = url.toLowerCase();
            host = "";
        }

        // 1) Score each focus area
        String bestFocus = "general";
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : FOCUS_KEYWORDS.entrySet()) {
            String focus = entry.getKey();
            int score = 0;

            for (String keyword : entry.getValue()) {
                if (host.contains(keyword))         score += 10;       // domain match = highest weight
                if (lowerUrl.contains(keyword))      score += 5;        // path match
            }

            if (score > bestScore) {
                bestScore = score;
                bestFocus = focus;
            }
        }

        // Need minimum score to override "general"
        return bestScore >= 5 ? bestFocus : "general";
    }

    /**
     * Extract domain from URL.
     */
    public String extractDomain(String url) {
        try {
            return new URL(url).getHost();
        } catch (Exception e) {
            // Fallback: strip protocol
            return url.replaceFirst("^https?://", "").replaceFirst("/.*", "");
        }
    }

    /**
     * All available focus areas (for dropdown in admin UI).
     */
    public List<String> getAllFocusAreas() {
        return new ArrayList<>(List.of("general")) {
            {
                addAll(FOCUS_KEYWORDS.keySet().stream().sorted().toList());
            }
        };
    }
}
