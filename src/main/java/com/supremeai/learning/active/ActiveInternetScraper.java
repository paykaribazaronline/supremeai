package com.supremeai.learning.active;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.learning.knowledge.SolutionMemory;
import com.supremeai.learning.service.EnhancedContentSanitizerService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Active Internet Scraper - Proactively learns solutions from the internet before users encounter
 * errors. Uses free public APIs and includes content sanitization and source authority weighting.
 */
@Service
public class ActiveInternetScraper {

  private static final Logger log = LoggerFactory.getLogger(ActiveInternetScraper.class);
  private final WebClient webClient;
  private final ObjectMapper objectMapper;
  private final EnhancedContentSanitizerService sanitizer;
  private final QueryClassifier queryClassifier;

  @Value("${learning.scraper.wikipedia.limit:5}")
  private int wikiLimit;

  @Value("${learning.scraper.stackoverflow.limit:3}")
  private int soLimit;

  public ActiveInternetScraper(
      WebClient.Builder webClientBuilder,
      EnhancedContentSanitizerService sanitizer,
      QueryClassifier queryClassifier) {
    this.webClient = webClientBuilder.build();
    this.objectMapper = new ObjectMapper();
    this.sanitizer = sanitizer;
    this.queryClassifier = queryClassifier;
  }

  /** Targeted knowledge scraping for a specific domain. */
  public Flux<ScrapedIssue> scrapeKnowledge(String domainName, List<String> keywords) {
    log.info(
        "[Active Learning] Starting targeted scrape for domain: {} with keywords: {}",
        domainName,
        keywords);

    List<Flux<ScrapedIssue>> sources = new ArrayList<>();
    QueryClassifier.QueryClassification classification =
        queryClassifier.classify(domainName, keywords);

    // 1. General Wikipedia Search
    sources.add(scrapeWikipediaTargeted(domainName, keywords));

    // 2. DuckDuckGo Web Search Fallback for highly specific general questions
    String cleanQuery = buildCleanQuery(domainName, keywords);
    sources.add(scrapeDuckDuckGoFallback(cleanQuery));

    // 3. Code Search
    if (classification.isCodeRelated()) {
      sources.add(scrapeStackOverflowTargeted(domainName, keywords));
      sources.add(scrapeGitHubIssuesTargeted(domainName, keywords));
    }

    // 4. Web Dev Search
    if (classification.isWebDevRelated()) {
      sources.add(scrapeMDNTargeted(domainName, keywords));
    }

    // 5. Research Search
    if (classification.isResearchRelated()) {
      sources.add(scrapeArxivTargeted(domainName, keywords));
    }

    return Flux.merge(sources);
  }

  /**
   * Scrape trending issues from the internet (e.g. from general tech news or Wikipedia trending)
   */
  public List<ScrapedIssue> scrapeTrendingIssues() {
    log.info("[Active Learning] Scraping trending tech issues...");
    List<ScrapedIssue> issues = new ArrayList<>();

    // Basic static trending topics for now - in production this would call actual news APIs
    issues.add(
        new ScrapedIssue(
            "Spring Boot 3.4 Virtual Threads",
            "Virtual threads in Spring Boot 3.4 provide better performance for I/O bound tasks.",
            "InternalDiscovery"));
    issues.add(
        new ScrapedIssue(
            "React 19 Server Components",
            "React 19 introduces stabilized server components and actions.",
            "InternalDiscovery"));
    issues.add(
        new ScrapedIssue(
            "JDK 23 Features",
            "JDK 23 introduces new pattern matching features and performance improvements.",
            "InternalDiscovery"));

    return issues;
  }

  /** Clean query builder to prevent keyword duplication and improve relevance score */
  private String buildCleanQuery(String domainName, List<String> keywords) {
    java.util.Set<String> uniqueWords = new java.util.LinkedHashSet<>();
    if (domainName != null) {
      for (String part : domainName.toLowerCase().split("\\s+")) {
        if (part.length() > 2) uniqueWords.add(part);
      }
    }
    if (keywords != null) {
      for (String kw : keywords) {
        if (kw.length() > 2) uniqueWords.add(kw.toLowerCase());
      }
    }
    return uniqueWords.isEmpty() ? "technology" : String.join(" ", uniqueWords);
  }

  /** Detect if text contains Bengali characters for dual-language searches */
  private boolean containsBengali(String text) {
    if (text == null) return false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= '\u0980' && c <= '\u09FF') {
        return true;
      }
    }
    return false;
  }

  /** DuckDuckGo General Web Scraper - free, zero API keys, handles everything Wikipedia misses */
  private Flux<ScrapedIssue> scrapeDuckDuckGoFallback(String query) {
    String url =
        "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
    return webClient
        .get()
        .uri(url)
        .header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        .retrieve()
        .bodyToMono(String.class)
        .flatMapMany(
            html -> {
              List<ScrapedIssue> results = new ArrayList<>();
              try {
                int index = 0;
                while ((index = html.indexOf("class=\"result__snippet\"", index)) != -1) {
                  int snippetStart = html.indexOf(">", index) + 1;
                  int snippetEnd = html.indexOf("</a>", snippetStart);
                  if (snippetEnd == -1) break;
                  String snippet =
                      html.substring(snippetStart, snippetEnd)
                          .replaceAll("<[^>]*>", "")
                          .replaceAll("&amp;", "&")
                          .replaceAll("&quot;", "\"")
                          .trim();

                  int resultBodyStart = html.lastIndexOf("class=\"result__body\"", index);
                  String title = "Web Result";
                  if (resultBodyStart != -1) {
                    int titleLinkStart = html.indexOf("class=\"result__a\"", resultBodyStart);
                    if (titleLinkStart != -1 && titleLinkStart < index) {
                      int titleStart = html.indexOf(">", titleLinkStart) + 1;
                      int titleEnd = html.indexOf("</a>", titleStart);
                      if (titleEnd != -1) {
                        title =
                            html.substring(titleStart, titleEnd)
                                .replaceAll("<[^>]*>", "")
                                .replaceAll("&amp;", "&")
                                .replaceAll("&quot;", "\"")
                                .trim();
                      }
                    }
                  }

                  if (!snippet.isEmpty() && snippet.length() > 30) {
                    ScrapedIssue issue = new ScrapedIssue(title, snippet, "Web Search");
                    issue.setSolution(snippet);
                    issue.setSourceAuthority(SourceAuthority.FORUMS.getWeight());
                    issue.setRawConfidence(0.65);
                    results.add(issue);
                  }
                  index = snippetEnd;
                  if (results.size() >= 3) break;
                }
              } catch (Exception e) {
                log.error("Failed to parse DuckDuckGo HTML: {}", e.getMessage());
              }
              return Flux.fromIterable(results);
            })
        .onErrorResume(
            e -> {
              log.warn("DuckDuckGo HTML search failed: {}", e.getMessage());
              return Flux.empty();
            });
  }

  private Flux<ScrapedIssue> scrapeWikipediaTargeted(String domainName, List<String> keywords) {
    String cleanQuery = buildCleanQuery(domainName, keywords);
    String wikiSubdomain = containsBengali(cleanQuery) ? "bn" : "en";

    String url =
        String.format(
            "https://%s.wikipedia.org/w/api.php?action=query&list=search&srsearch=%s&srlimit=%d&format=json",
            wikiSubdomain, URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8), wikiLimit);

    return webClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .flatMapMany(
            response -> {
              try {
                JsonNode root = objectMapper.readTree(response);
                JsonNode search = root.path("query").path("search");
                List<ScrapedIssue> issues = new ArrayList<>();
                if (search.isArray()) {
                  for (JsonNode item : search) {
                    String title = item.path("title").textValue();
                    String snippet = item.path("snippet").textValue().replaceAll("<[^>]*>", "");

                    ScrapedIssue issue =
                        new ScrapedIssue(
                            title, snippet, "Wikipedia (" + wikiSubdomain.toUpperCase() + ")");
                    issue.setSourceAuthority(SourceAuthority.WIKIPEDIA.getWeight());
                    issue.setRawConfidence(0.7 + (new Random().nextDouble() * 0.2));
                    issues.add(issue);
                  }
                }
                return Flux.fromIterable(issues);
              } catch (Exception e) {
                log.error("Failed to parse Wikipedia search results: {}", e.getMessage());
                return Flux.empty();
              }
            })
        .flatMap(
            issue ->
                fetchWikipediaSummaryReactive(issue.getTitle(), wikiSubdomain)
                    .map(
                        summary -> {
                          if (summary != null && !summary.isEmpty()) {
                            issue.setSolution(summary);
                          }
                          return issue;
                        })
                    .defaultIfEmpty(issue))
        .onErrorResume(
            e -> {
              log.error("Wikipedia search failed gracefully: {}", e.getMessage());
              return Flux.empty();
            });
  }

  private Mono<String> fetchWikipediaSummaryReactive(String title, String wikiSubdomain) {
    String url =
        String.format(
            "https://%s.wikipedia.org/api/rest_v1/page/summary/%s",
            wikiSubdomain, URLEncoder.encode(title, StandardCharsets.UTF_8));
    return webClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .map(
            response -> {
              try {
                JsonNode root = objectMapper.readTree(response);
                return root.path("extract").textValue();
              } catch (Exception e) {
                return "";
              }
            })
        .onErrorResume(e -> Mono.just(""));
  }

  private Flux<ScrapedIssue> scrapeStackOverflowTargeted(String domainName, List<String> keywords) {
    String tagged =
        String.join(
            ";", keywords.stream().map(k -> k.toLowerCase().replace(" ", "-")).limit(3).toList());

    String url =
        String.format(
            "https://api.stackexchange.com/2.3/questions?order=desc&sort=relevance&intitle=%s&tagged=%s&site=stackoverflow&pagesize=%d&filter=withbody",
            URLEncoder.encode(domainName, StandardCharsets.UTF_8),
            URLEncoder.encode(tagged, StandardCharsets.UTF_8),
            soLimit);

    return webClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .flatMapMany(
            response -> {
              try {
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("items");
                List<ScrapedIssue> issues = new ArrayList<>();
                if (items.isArray()) {
                  for (JsonNode question : items) {
                    String title = question.path("title").textValue();
                    String link = question.path("link").textValue();
                    String body =
                        question
                            .path("body")
                            .textValue()
                            .replaceAll("<[^>]*>", "")
                            .substring(
                                0, Math.min(500, question.path("body").textValue().length()));

                    ScrapedIssue issue =
                        new ScrapedIssue(
                            title, "SO Context: " + body + "\nSource: " + link, "StackOverflow");
                    issue.setSourceAuthority(SourceAuthority.STACK_OVERFLOW.getWeight());
                    issue.setRawConfidence(0.8 + (new Random().nextDouble() * 0.15));
                    issues.add(issue);
                  }
                }
                return Flux.fromIterable(issues);
              } catch (Exception e) {
                log.error("Failed to parse StackOverflow search results: {}", e.getMessage());
                return Flux.empty();
              }
            })
        .onErrorResume(e -> Flux.empty());
  }

  private Flux<ScrapedIssue> scrapeGitHubIssuesTargeted(String domainName, List<String> keywords) {
    String query = domainName + " " + String.join(" ", keywords);
    String url =
        String.format(
            "https://api.github.com/search/issues?q=%s&per_page=3",
            URLEncoder.encode(query, StandardCharsets.UTF_8));

    return webClient
        .get()
        .uri(url)
        .header("Accept", "application/vnd.github.v3+json")
        .retrieve()
        .bodyToMono(String.class)
        .flatMapMany(
            response -> {
              try {
                JsonNode root = objectMapper.readTree(response);
                JsonNode items = root.path("items");
                List<ScrapedIssue> issues = new ArrayList<>();
                if (items.isArray()) {
                  for (JsonNode issueNode : items) {
                    String title = issueNode.path("title").textValue();
                    String htmlUrl = issueNode.path("html_url").textValue();
                    String body = issueNode.path("body").textValue();
                    if (body == null) body = "";
                    body =
                        body.replaceAll("<[^>]*>", "").substring(0, Math.min(500, body.length()));

                    ScrapedIssue issue =
                        new ScrapedIssue(
                            title, "GitHub Issue: " + body + "\nSource: " + htmlUrl, "GitHub");
                    issue.setSourceAuthority(0.85); // High authority for code issues
                    issue.setRawConfidence(0.75 + (new Random().nextDouble() * 0.15));
                    issues.add(issue);
                  }
                }
                return Flux.fromIterable(issues);
              } catch (Exception e) {
                return Flux.empty();
              }
            })
        .onErrorResume(e -> Flux.empty());
  }

  private Flux<ScrapedIssue> scrapeMDNTargeted(String domainName, List<String> keywords) {
    String query = domainName + " " + String.join(" ", keywords);
    String url =
        String.format(
            "https://developer.mozilla.org/api/v1/search?q=%s&size=3",
            URLEncoder.encode(query, StandardCharsets.UTF_8));

    return webClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .flatMapMany(
            response -> {
              try {
                JsonNode root = objectMapper.readTree(response);
                JsonNode documents = root.path("documents");
                List<ScrapedIssue> issues = new ArrayList<>();
                if (documents.isArray()) {
                  for (JsonNode doc : documents) {
                    String title = doc.path("title").textValue();
                    String summary = doc.path("summary").textValue();
                    String mdnUrl =
                        "https://developer.mozilla.org" + doc.path("mdn_url").textValue();

                    ScrapedIssue issue =
                        new ScrapedIssue(
                            title, "MDN Docs: " + summary + "\nSource: " + mdnUrl, "MDN");
                    issue.setSourceAuthority(0.95); // Very high authority for web docs
                    issue.setRawConfidence(0.9 + (new Random().nextDouble() * 0.1));
                    issues.add(issue);
                  }
                }
                return Flux.fromIterable(issues);
              } catch (Exception e) {
                return Flux.empty();
              }
            })
        .onErrorResume(e -> Flux.empty());
  }

  private Flux<ScrapedIssue> scrapeArxivTargeted(String domainName, List<String> keywords) {
    String query = domainName + " " + String.join(" ", keywords);
    String url =
        String.format(
            "http://export.arxiv.org/api/query?search_query=all:%s&max_results=3",
            URLEncoder.encode(query, StandardCharsets.UTF_8));

    return webClient
        .get()
        .uri(url)
        .retrieve()
        .bodyToMono(String.class)
        .flatMapMany(
            response -> {
              // Quick and dirty XML parsing for Arxiv
              try {
                List<ScrapedIssue> issues = new ArrayList<>();
                String[] entries = response.split("<entry>");
                for (int i = 1; i < entries.length; i++) {
                  String entry = entries[i];
                  String title = extractXmlTag(entry, "title");
                  String summary = extractXmlTag(entry, "summary");
                  String id = extractXmlTag(entry, "id");

                  ScrapedIssue issue =
                      new ScrapedIssue(
                          title, "arXiv Abstract: " + summary + "\nSource: " + id, "arXiv");
                  issue.setSourceAuthority(0.9); // High authority for research
                  issue.setRawConfidence(0.8 + (new Random().nextDouble() * 0.1));
                  issues.add(issue);
                }
                return Flux.fromIterable(issues);
              } catch (Exception e) {
                return Flux.empty();
              }
            })
        .onErrorResume(e -> Flux.empty());
  }

  private String extractXmlTag(String xml, String tag) {
    String startTag = "<" + tag + ">";
    String endTag = "</" + tag + ">";
    int start = xml.indexOf(startTag);
    int end = xml.indexOf(endTag);
    if (start != -1 && end != -1) {
      return xml.substring(start + startTag.length(), end).trim().replaceAll("\n", " ");
    }
    return "";
  }

  /** Convert ScrapedIssue to SolutionMemory, applying sanitization. */
  public SolutionMemory convertToSolution(ScrapedIssue issue, String errorSignature) {
    SolutionMemory candidate =
        new SolutionMemory(
            errorSignature != null ? errorSignature : issue.getTitle(),
            issue.getSolution(),
            issue.getSource(),
            estimateExecutionTime(issue.getSolution()),
            estimateSecurityScore(issue.getSolution()));
    candidate.setTimeless(isLikelyTimeless(issue));

    if (sanitizer.sanitizeAndValidate(candidate, issue.getSource()).isValid()) {
      return candidate;
    } else {
      log.warn("[Sanitizer] Dropped solution from {} (failed validation)", issue.getSource());
      return null;
    }
  }

  private long estimateExecutionTime(String code) {
    return Math.min(5000, Math.max(10, code.length() * 2));
  }

  private double estimateSecurityScore(String code) {
    String lower = code.toLowerCase();
    if (lower.contains("eval(") || lower.contains("exec(") || lower.contains("system(")) return 0.2;
    if (lower.contains("password") || lower.contains("secret") || lower.contains("token"))
      return 0.3;
    return 0.8;
  }

  private boolean isLikelyTimeless(ScrapedIssue issue) {
    String title = issue.getTitle().toLowerCase();
    String[] timelessKeywords = {
      "algorithm", "reverse", "sort", "search", "tree", "graph", "recursion"
    };
    for (String kw : timelessKeywords) {
      if (title.contains(kw)) return true;
    }
    return false;
  }

  public static class ScrapedIssue {
    private String title;
    private String solution;
    private String source;
    private double sourceAuthority;
    private double rawConfidence;

    public ScrapedIssue(String title, String solution, String source) {
      this.title = title;
      this.solution = solution;
      this.source = source;
      this.sourceAuthority = 0.5;
      this.rawConfidence = 0.5;
    }

    public String getTitle() {
      return title;
    }

    public String getSolution() {
      return solution;
    }

    public String getBody() {
      return solution;
    } // Compatibility with getBody()

    public void setSolution(String solution) {
      this.solution = solution;
    }

    public String getSource() {
      return source;
    }

    public double getSourceAuthority() {
      return sourceAuthority;
    }

    public void setSourceAuthority(double authority) {
      this.sourceAuthority = authority;
    }

    public double getRawConfidence() {
      return rawConfidence;
    }

    public void setRawConfidence(double rawConfidence) {
      this.rawConfidence = rawConfidence;
    }

    // Compatibility with getAuthority().getWeight()
    public AuthorityWrapper getAuthority() {
      return new AuthorityWrapper(sourceAuthority);
    }

    public static class AuthorityWrapper {
      private final double weight;

      public AuthorityWrapper(double weight) {
        this.weight = weight;
      }

      public double getWeight() {
        return weight;
      }
    }
  }
}
