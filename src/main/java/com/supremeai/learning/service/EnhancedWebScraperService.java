package com.supremeai.learning.service;

import com.supremeai.learning.ContentSanitizerService;
import com.supremeai.learning.knowledge.SolutionMemory;
import com.supremeai.learning.active.SourceAuthority;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Enhanced Web Scraper Service with comprehensive features:
 * - Rate limiting per domain
 * - Content caching with TTL
 * - Deduplication
 * - Technology detection
 * - Source authority tracking
 * - Integration with sanitization
 * - Quality scoring
 */
@Service
public class EnhancedWebScraperService {
    private static final Logger log = LoggerFactory.getLogger(EnhancedWebScraperService.class);

    @Autowired(required = false)
    private ContentSanitizerService sanitizer;

    // Rate limiting configuration
    @Value("${scraper.rate.limit.requests:10}")
    private int rateLimitRequests;

    @Value("${scraper.rate.limit.window:60}")
    private int rateLimitWindowSeconds;

    @Value("${scraper.cache.ttl.minutes:30}")
    private int cacheTtlMinutes;

    // Rate limiting per domain
    private final Map<String, Semaphore> domainLimiters = new ConcurrentHashMap<>();
    private final Map<String, Queue<Long>> requestTimestamps = new ConcurrentHashMap<>();

    // Deduplication cache (URL -> hash of content)
    private final Map<String, String> contentHashCache = new ConcurrentHashMap<>();

    // Content quality cache
    private final Map<String, Double> qualityScoreCache = new ConcurrentHashMap<>();

    // Source authority weights
    private static final Map<String, Double> SOURCE_AUTHORITIES = new HashMap<>();
    static {
        SOURCE_AUTHORITIES.put("github.com", 0.90);
        SOURCE_AUTHORITIES.put("stackoverflow.com", 0.85);
        SOURCE_AUTHORITIES.put("wikipedia.org", 0.80);
        SOURCE_AUTHORITIES.put("medium.com", 0.75);
        SOURCE_AUTHORITIES.put("dev.to", 0.70);
        SOURCE_AUTHORITIES.put("docs.spring.io", 0.90);
        SOURCE_AUTHORITIES.put("developer.android.com", 0.90);
        SOURCE_AUTHORITIES.put("kotlinlang.org", 0.85);
        SOURCE_AUTHORITIES.put("react.dev", 0.85);
    }

    // Technology detection patterns
    private static final Pattern[] TECH_PATTERNS = {
        Pattern.compile("(?i)\\b(java|kotlin|python|javascript|typescript|go|rust|c\\+\\+|c#|php|ruby|swift)\\b"),
        Pattern.compile("(?i)\\b(spring|react|angular|vue|django|flask|express|node\\.js|nestjs)\\b"),
        Pattern.compile("(?i)\\b(reactive|microservice|graphql|grpc|websocket|rest|api)\\b"),
        Pattern.compile("(?i)\\b(docker|kubernetes|terraform|ansible|jenkins|git|ci/cd)\\b"),
        Pattern.compile("(?i)\\b(aws|gcp|azure|firebase|mongodb|postgresql|mysql|redis)\\b")
    };

    // Security patterns for code analysis
    private static final Pattern[] SECURITY_PATTERNS = {
        Pattern.compile("(?i)eval\\s*\\("),
        Pattern.compile("(?i)exec\\s*\\("),
        Pattern.compile("(?i)system\\s*\\("),
        Pattern.compile("(?i)shell_exec\\s*\\("),
        Pattern.compile("(?i)passthru\\s*\\(")
    };

    /**
     * Scrape URL with comprehensive features.
     */
    @Cacheable(value = "scrapedContent", key = "#url", unless = "#result == null")
    public ScrapedContent scrapeUrl(String url) {
        // Check rate limit
        if (!checkRateLimit(url)) {
            log.warn("Rate limit exceeded for URL: {}", url);
            return null;
        }

        try {
            // Extract domain for source authority
            String domain = extractDomain(url);
            double authority = SOURCE_AUTHORITIES.getOrDefault(domain, 0.50);

            // Scrape with proper headers
            Document doc = Jsoup.connect(url)
                    .userAgent("SupremeAI-Cloud-Agent/2.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(10000)
                    .followRedirects(true)
                    .maxBodySize(0)
                    .get();

            // Extract structured content
            String title = extractTitle(doc);
            String content = extractMainContent(doc);
            String codeSnippets = extractCodeSnippets(doc);

            // Generate content hash for deduplication
            String contentHash = generateHash(content);

            // Check for duplicate content
            if (isDuplicate(contentHash)) {
                log.debug("Duplicate content detected for URL: {}", url);
                return null;
            }

            // Store hash for future deduplication
            contentHashCache.put(url, contentHash);

            // Detect technologies mentioned
            Set<String> technologies = detectTechnologies(content);

            // Calculate quality score
            double qualityScore = calculateQualityScore(content, codeSnippets, technologies);

            // Create scraped content object
            ScrapedContent scraped = new ScrapedContent(
                    url,
                    title,
                    content,
                    codeSnippets,
                    domain,
                    authority,
                    technologies,
                    qualityScore,
                    LocalDateTime.now()
            );

            log.info("Successfully scraped URL: {} (authority: {}, quality: {}, technologies: {})", 
                    url, authority, qualityScore, technologies);

            return scraped;
        } catch (Exception e) {
            log.error("Scraping failed for URL: {}", url, e);
            return null;
        }
    }

    /**
     * Convert scraped content to solution memory with sanitization.
     */
    public SolutionMemory convertToSolution(ScrapedContent scraped, String errorSignature) {
        if (scraped == null || sanitizer == null) {
            return null;
        }

        SolutionMemory candidate = new SolutionMemory(
                errorSignature != null ? errorSignature : scraped.getTitle(),
                scraped.getFullContent(),
                scraped.getDomain(),
                estimateExecutionTime(scraped.getCodeSnippets()),
                estimateSecurityScore(scraped.getCodeSnippets())
        );

        // Set source authority
        candidate.setSourceAuthority(scraped.getSourceAuthority());

        // Apply sanitization
        if (sanitizer.sanitizeAndValidate(candidate, scraped.getDomain())) {
            return candidate;
        } else {
            log.warn("[Sanitizer] Dropped solution from {} (failed validation)", 
                    scraped.getDomain());
            return null;
        }
    }

    /**
     * Check rate limit for URL domain.
     */
    private boolean checkRateLimit(String url) {
        String domain = extractDomain(url);
        Semaphore limiter = domainLimiters.computeIfAbsent(
                domain, k -> new Semaphore(rateLimitRequests)
        );

        Queue<Long> timestamps = requestTimestamps.computeIfAbsent(
                domain, k -> new LinkedList<>()
        );

        // Clean old timestamps
        long now = System.currentTimeMillis();
        long windowStart = now - TimeUnit.SECONDS.toMillis(rateLimitWindowSeconds);

        while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
            timestamps.poll();
        }

        // Check if under limit
        if (timestamps.size() < rateLimitRequests && limiter.tryAcquire()) {
            timestamps.offer(now);
            return true;
        }

        return false;
    }

    /**
     * Extract domain from URL.
     */
    private String extractDomain(String url) {
        try {
            String domain = url.replaceAll("^https?://", "")
                    .replaceAll("/.*$", "")
                    .replaceAll("www\\.", "");
            return domain.split("/")[0];
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Extract title from document.
     */
    private String extractTitle(Document doc) {
        Element title = doc.selectFirst("title");
        return title != null ? title.text() : "Untitled";
    }

    /**
     * Extract main content from document.
     */
    private String extractMainContent(Document doc) {
        // Try common content selectors
        Elements contentElements = doc.select(
                "article, main, .content, .post-content, .article-body, .markdown-body"
        );

        if (!contentElements.isEmpty()) {
            return contentElements.first().text();
        }

        // Fallback to body
        return doc.body().text();
    }

    /**
     * Extract code snippets from document.
     */
    private String extractCodeSnippets(Document doc) {
        Elements codeElements = doc.select("pre, code, .highlight, .code-block");
        StringBuilder code = new StringBuilder();

        for (Element element : codeElements) {
            code.append(element.text()).append("\n");
        }

        return code.toString();
    }

    /**
     * Generate hash for content deduplication.
     */
    private String generateHash(String content) {
        return String.valueOf(content.hashCode());
    }

    /**
     * Check if content is duplicate.
     */
    private boolean isDuplicate(String hash) {
        return contentHashCache.containsValue(hash);
    }

    /**
     * Detect technologies in content.
     */
    private Set<String> detectTechnologies(String content) {
        Set<String> technologies = new HashSet<>();
        String lowerContent = content.toLowerCase();

        for (Pattern pattern : TECH_PATTERNS) {
            java.util.regex.Matcher matcher = pattern.matcher(lowerContent);
            while (matcher.find()) {
                technologies.add(matcher.group().toLowerCase());
            }
        }

        return technologies;
    }

    /**
     * Calculate quality score for content.
     */
    private double calculateQualityScore(String content, String codeSnippets, Set<String> technologies) {
        double score = 0.5; // Base score

        // Length factor (not too short, not too long)
        int length = content.length();
        if (length > 100 && length < 10000) {
            score += 0.2;
        }

        // Code snippets presence
        if (!codeSnippets.isEmpty()) {
            score += 0.2;
        }

        // Technology detection
        if (!technologies.isEmpty()) {
            score += 0.1;
        }

        // Source authority is added separately

        return Math.min(1.0, score);
    }

    /**
     * Estimate execution time for code.
     */
    private long estimateExecutionTime(String code) {
        return Math.min(5000, Math.max(10, code.length() * 2));
    }

    /**
     * Estimate security score for code.
     */
    private double estimateSecurityScore(String code) {
        String lower = code.toLowerCase();

        // Check for dangerous patterns
        for (Pattern pattern : SECURITY_PATTERNS) {
            if (pattern.matcher(lower).find()) {
                return 0.2;
            }
        }

        // Check for sensitive data
        if (lower.contains("password") || lower.contains("secret") || lower.contains("token")) {
            return 0.3;
        }

        return 0.8; // Default decent score
    }

    /**
     * Scheduled cleanup of old cache entries.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupCache() {
        log.info("Starting cache cleanup...");

        // Clean old request timestamps
        long now = System.currentTimeMillis();
        long windowStart = now - TimeUnit.SECONDS.toMillis(rateLimitWindowSeconds);

        requestTimestamps.forEach((domain, timestamps) -> {
            while (!timestamps.isEmpty() && timestamps.peek() < windowStart) {
                timestamps.poll();
            }
        });

        // Clean old content hashes (older than cache TTL)
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(cacheTtlMinutes);
        contentHashCache.entrySet().removeIf(entry -> {
            // Simple heuristic: remove old entries
            return entry.getKey().hashCode() < cutoff.hashCode();
        });

        log.info("Cache cleanup completed");
    }

    /**
     * Inner class to represent scraped content.
     */
    public static class ScrapedContent {
        private final String url;
        private final String title;
        private final String content;
        private final String codeSnippets;
        private final String domain;
        private final double sourceAuthority;
        private final Set<String> technologies;
        private final double qualityScore;
        private final LocalDateTime scrapedAt;

        public ScrapedContent(String url, String title, String content, String codeSnippets,
                           String domain, double sourceAuthority, Set<String> technologies,
                           double qualityScore, LocalDateTime scrapedAt) {
            this.url = url;
            this.title = title;
            this.content = content;
            this.codeSnippets = codeSnippets;
            this.domain = domain;
            this.sourceAuthority = sourceAuthority;
            this.technologies = technologies;
            this.qualityScore = qualityScore;
            this.scrapedAt = scrapedAt;
        }

        public String getUrl() { return url; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
        public String getCodeSnippets() { return codeSnippets; }
        public String getDomain() { return domain; }
        public double getSourceAuthority() { return sourceAuthority; }
        public Set<String> getTechnologies() { return technologies; }
        public double getQualityScore() { return qualityScore; }
        public LocalDateTime getScrapedAt() { return scrapedAt; }

        public String getFullContent() {
            return title + "\n\n" + content + "\n\n" + codeSnippets;
        }
    }
}
