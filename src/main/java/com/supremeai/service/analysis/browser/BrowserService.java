package com.supremeai.service.browser;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.supremeai.service.SelfHealingService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class BrowserService {
  private static final Logger log = LoggerFactory.getLogger(BrowserService.class);

  private final SelfHealingService selfHealingService;

  public BrowserService(@Lazy SelfHealingService selfHealingService) {
    this.selfHealingService = selfHealingService;
  }

  public Mono<String> searchAndScrape(String query, String engine, String baseUrl) {
    return Mono.fromCallable(
            () -> {
              try {
                String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
                // URL format handling for database configurations
                String targetUrl =
                    baseUrl.contains("%s")
                        ? baseUrl.replace("%s", encodedQuery)
                        : baseUrl + encodedQuery;

                log.info(
                    "🌐 [Level-3 Web Fallback] Initiating Fast-Path (Jsoup) connection via {}: {}",
                    engine,
                    targetUrl);

                String scrapedData = performJsoupScrape(targetUrl, engine);

                String domain = extractDomain(targetUrl);

                if (selfHealingService.isDomainQuarantined(domain)) {
                  log.warn(
                      "🚫 [QUARANTINE] Domain {} is quarantined. Skipping Playwright, returning Jsoup fallback data.",
                      domain);
                  return scrapedData;
                }

                // Hybrid Check: Jsoup ফেইল করলে বা JS/Bot Protection থাকলে Playwright ফায়ার করবে
                if (needsPlaywrightFallback(scrapedData)) {
                  log.warn(
                      "⚠️ [Level-3] JS-rendering or Bot Protection detected! Switching to Playwright (Hybrid Engine)...");
                  try {
                    scrapedData = performPlaywrightScrape(targetUrl, engine);
                  } catch (Exception e) {
                    log.error("⚠️ Playwright crashed/blocked for {}: {}", domain, e.getMessage());
                    selfHealingService.recordDomainFailureAndMaybeQuarantine(domain);
                  }
                }

                return scrapedData;
              } catch (Exception e) {
                log.error("⚠️ Failed to scrape web data: {}", e.getMessage());
                return "";
              }
            })
        .subscribeOn(Schedulers.boundedElastic());
  }

  private String performJsoupScrape(String targetUrl, String engine) throws Exception {
    if ("duckduckgo".equalsIgnoreCase(engine) || targetUrl.contains("duckduckgo")) {
      Document doc =
          Jsoup.connect(targetUrl)
              .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
              .timeout(10000)
              .get();

      StringBuilder results = new StringBuilder();
      for (Element result : doc.select("a.result__snippet")) {
        results.append("- ").append(result.text()).append("\n");
      }
      log.info("✅ Scraped {} characters from search engine.", results.length());
      return results.toString();
    } else {
      Document doc = Jsoup.connect(targetUrl).userAgent("Mozilla/5.0").timeout(10000).get();
      String text = doc.body().text();
      return text.length() > 5000 ? text.substring(0, 5000) : text;
    }
  }

  private String extractDomain(String url) {
    try {
      java.net.URI uri = new java.net.URI(url);
      String domain = uri.getHost();
      return domain != null ? domain.startsWith("www.") ? domain.substring(4) : domain : "unknown";
    } catch (Exception e) {
      return "unknown";
    }
  }

  private boolean needsPlaywrightFallback(String text) {
    if (text == null || text.trim().isEmpty()) return true;
    String lower = text.toLowerCase();
    // যদি লেখা খুব ছোট হয় বা কোনো সাধারণ বট প্রোটেকশনের কিওয়ার্ড থাকে
    return lower.length() < 200
        || lower.contains("enable javascript")
        || lower.contains("checking your browser")
        || lower.contains("cloudflare")
        || lower.contains("please wait...")
        || lower.contains("captcha");
  }

  private String performPlaywrightScrape(String targetUrl, String engine) throws Exception {
    try (Playwright playwright = Playwright.create()) {
      // Headless মোডে রিয়েল ব্রাউজার (Chromium) রান হবে
      Browser browser =
          playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
      Page page = browser.newPage();
      page.navigate(targetUrl, new Page.NavigateOptions().setTimeout(15000));
      page.waitForLoadState(com.microsoft.playwright.options.LoadState.DOMCONTENTLOADED);

      String html = page.content();
      browser.close();

      // Playwright এর আনা HTML কে আবার Jsoup দিয়ে ক্লিন করা হচ্ছে
      Document doc = org.jsoup.Jsoup.parse(html);
      String text = doc.body().text();
      return text.length() > 5000 ? text.substring(0, 5000) : text;
    }
  }

  // --- STATEFUL BROWSER IMPLEMENTATION ---
  private Playwright activePlaywright;
  private Browser activeBrowser;
  private Page activePage;
  private String currentStatus = "inactive";
  private final Object browserLock = new Object();
  private final java.util.concurrent.ExecutorService browserExecutor =
      java.util.concurrent.Executors.newSingleThreadExecutor();

  public reactor.core.publisher.Mono<java.util.Map<String, Object>> getStatus() {
    return reactor.core.publisher.Mono.just(java.util.Map.of("status", currentStatus));
  }

  public reactor.core.publisher.Mono<Void> startBrowsing() {
    return reactor.core.publisher.Mono.fromRunnable(
            () -> {
              synchronized (browserLock) {
                if (activePlaywright == null) {
                  currentStatus = "starting";
                  activePlaywright = Playwright.create();
                  activeBrowser =
                      activePlaywright
                          .chromium()
                          .launch(new BrowserType.LaunchOptions().setHeadless(true));
                  activePage = activeBrowser.newPage();
                  currentStatus = "active";
                  log.info("🚀 Stateful Playwright Session Started");
                }
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor))
        .then();
  }

  public reactor.core.publisher.Mono<Void> stopBrowsing() {
    return reactor.core.publisher.Mono.fromRunnable(
            () -> {
              synchronized (browserLock) {
                if (activePlaywright != null) {
                  currentStatus = "stopping";
                  if (activePage != null) activePage.close();
                  if (activeBrowser != null) activeBrowser.close();
                  activePlaywright.close();
                  activePlaywright = null;
                  activeBrowser = null;
                  activePage = null;
                  currentStatus = "inactive";
                  log.info("🛑 Stateful Playwright Session Stopped");
                }
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor))
        .then();
  }

  public reactor.core.publisher.Mono<String> getScreenshot() {
    return reactor.core.publisher.Mono.fromCallable(
            () -> {
              synchronized (browserLock) {
                if (activePage != null) {
                  byte[] bytes =
                      activePage.screenshot(new Page.ScreenshotOptions().setFullPage(false));
                  return "data:image/png;base64,"
                      + java.util.Base64.getEncoder().encodeToString(bytes);
                }
                return "";
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor));
  }

  public reactor.core.publisher.Mono<Void> navigateTo(String url) {
    return reactor.core.publisher.Mono.fromRunnable(
            () -> {
              synchronized (browserLock) {
                if (activePage != null) {
                  activePage.navigate(url);
                  log.info("📍 Navigated to: {}", url);
                }
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor))
        .then();
  }

  public reactor.core.publisher.Mono<Void> click(String selector) {
    return reactor.core.publisher.Mono.fromRunnable(
            () -> {
              synchronized (browserLock) {
                if (activePage != null) {
                  activePage.click(selector);
                  log.info("🖱️ Clicked selector: {}", selector);
                }
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor))
        .then();
  }

  public reactor.core.publisher.Mono<Void> fill(String selector, String value) {
    return reactor.core.publisher.Mono.fromRunnable(
            () -> {
              synchronized (browserLock) {
                if (activePage != null) {
                  activePage.fill(selector, value);
                  log.info("⌨️ Filled selector: {} with value", selector);
                }
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor))
        .then();
  }

  public reactor.core.publisher.Mono<Void> clickAt(Integer x, Integer y) {
    return reactor.core.publisher.Mono.fromRunnable(
            () -> {
              synchronized (browserLock) {
                if (activePage != null) {
                  activePage.mouse().click(x, y);
                  log.info("🖱️ Clicked at: {}, {}", x, y);
                }
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor))
        .then();
  }

  public reactor.core.publisher.Mono<Void> typeKey(String key) {
    return reactor.core.publisher.Mono.fromRunnable(
            () -> {
              synchronized (browserLock) {
                if (activePage != null) {
                  activePage.keyboard().press(key);
                }
              }
            })
        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(browserExecutor))
        .then();
  }

  public reactor.core.publisher.Mono<java.util.Map<String, Object>> getAccessibilityTree() {
    return reactor.core.publisher.Mono.just(java.util.Collections.emptyMap());
  }

  public reactor.core.publisher.Mono<com.supremeai.model.browser.BrowserActivity> recordActivity(
      String url, String action, String title, String reasoning) {
    return reactor.core.publisher.Mono.just(new com.supremeai.model.browser.BrowserActivity());
  }

  public reactor.core.publisher.Mono<Void> executeAutonomousStep(String id) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Flux<com.supremeai.model.browser.BrowserActivity>
      getRecentActivity() {
    return reactor.core.publisher.Flux.empty();
  }

  public reactor.core.publisher.Flux<com.supremeai.model.browser.StoredCredential>
      getAllCredentials(String userId) {
    return reactor.core.publisher.Flux.empty();
  }

  public reactor.core.publisher.Mono<com.supremeai.model.browser.StoredCredential> saveCredential(
      com.supremeai.model.browser.StoredCredential credential) {
    return reactor.core.publisher.Mono.just(credential);
  }

  public reactor.core.publisher.Mono<Void> resumeActivity(String activityId) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Mono<Void> skipAuth(String activityId) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Mono<Void> pauseForManualCredential(String activityId) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Mono<java.util.Map<String, Object>> getPausedState() {
    return reactor.core.publisher.Mono.just(java.util.Collections.emptyMap());
  }

  public reactor.core.publisher.Flux<com.supremeai.model.browser.UrlPermission> getAllowedUrls(
      String userId) {
    return reactor.core.publisher.Flux.empty();
  }

  public reactor.core.publisher.Flux<com.supremeai.model.browser.UrlPermission> getDeniedUrls(
      String userId) {
    return reactor.core.publisher.Flux.empty();
  }

  public reactor.core.publisher.Mono<com.supremeai.model.browser.UrlPermission> addUrlPermission(
      com.supremeai.model.browser.UrlPermission permission) {
    return reactor.core.publisher.Mono.just(permission);
  }

  public reactor.core.publisher.Mono<com.supremeai.model.browser.UrlPermission> updateUrlPermission(
      String id, com.supremeai.model.browser.UrlPermission permission) {
    return reactor.core.publisher.Mono.just(permission);
  }

  public reactor.core.publisher.Mono<Void> deleteUrlPermission(String id) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Flux<com.supremeai.model.browser.UrlPermissionRequest>
      getPermissionRequests() {
    return reactor.core.publisher.Flux.empty();
  }

  public reactor.core.publisher.Mono<Void> processPermissionDecision(String id, Boolean approved) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Mono<java.util.Map<String, Object>> getSystemLearningStatus() {
    return reactor.core.publisher.Mono.just(java.util.Map.of("enabled", true));
  }

  public reactor.core.publisher.Mono<Void> toggleAutoLearn(Boolean enabled) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Flux<com.supremeai.model.browser.BrowserTask> getActiveTasks() {
    return reactor.core.publisher.Flux.empty();
  }

  public reactor.core.publisher.Mono<com.supremeai.model.browser.BrowserTask> createActivityTask(
      String goal) {
    return reactor.core.publisher.Mono.just(new com.supremeai.model.browser.BrowserTask());
  }

  public reactor.core.publisher.Mono<Void> deleteTask(String id) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Mono<Void> deleteCredential(String id) {
    return reactor.core.publisher.Mono.empty();
  }

  public reactor.core.publisher.Flux<com.supremeai.model.browser.BrowserFinding> getFindingsForTask(
      String id) {
    return reactor.core.publisher.Flux.empty();
  }

  public reactor.core.publisher.Mono<com.supremeai.model.browser.BrowserFinding> createFinding(
      com.supremeai.model.browser.BrowserFinding finding) {
    return reactor.core.publisher.Mono.just(finding);
  }
}
