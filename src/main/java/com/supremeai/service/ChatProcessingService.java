package com.supremeai.service;

import com.supremeai.fallback.ThirdOpinionOrchestrator;
import com.supremeai.model.*;
import com.supremeai.repository.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatProcessingService {

  private static final Logger log = LoggerFactory.getLogger(ChatProcessingService.class);

  private final ChatHistoryRepository chatHistoryRepository;
  private final ThirdOpinionOrchestrator fallbackOrchestrator;

  private final AIProviderService aiProviderService;
  private final com.supremeai.service.browser.BrowserService browserService;
  private final AdminProviderValidationService validationService;
  private final CyberSecuritySkillService cyberSecuritySkillService;
  private final EnhancedLearningService enhancedLearningService;
  private final KnowledgeService knowledgeService;
  private final AutonomousQuestioningEngine autonomousEngine;
  private final ConfigService configService;
  private final MultiAIVotingService multiAIVotingService;
  private final DynamicRegistryService dynamicRegistryService;

  public ChatProcessingService(
      ChatHistoryRepository chatHistoryRepository,
      ThirdOpinionOrchestrator fallbackOrchestrator,
      AIProviderService aiProviderService,
      com.supremeai.service.browser.BrowserService browserService,
      AdminProviderValidationService validationService,
      CyberSecuritySkillService cyberSecuritySkillService,
      EnhancedLearningService enhancedLearningService,
      KnowledgeService knowledgeService,
      AutonomousQuestioningEngine autonomousEngine,
      ConfigService configService,
      MultiAIVotingService multiAIVotingService,
      DynamicRegistryService dynamicRegistryService) {
    this.chatHistoryRepository = chatHistoryRepository;
    this.fallbackOrchestrator = fallbackOrchestrator;
    this.aiProviderService = aiProviderService;
    this.browserService = browserService;
    this.validationService = validationService;
    this.cyberSecuritySkillService = cyberSecuritySkillService;
    this.enhancedLearningService = enhancedLearningService;
    this.knowledgeService = knowledgeService;
    this.autonomousEngine = autonomousEngine;
    this.configService = configService;
    this.multiAIVotingService = multiAIVotingService;
    this.dynamicRegistryService = dynamicRegistryService;
  }

  public Mono<Map<String, Object>> processMessage(String userId, String message, boolean isAdmin) {
    String sanitizedMessage =
        Jsoup.clean(
            message,
            Safelist.basic()
                .addTags("br", "p", "strong", "em", "code")
                .addProtocols("a", "href", "https"));

    ChatMessage chatMsg = new ChatMessage(userId, sanitizedMessage, isAdmin);
    chatMsg.setId(generateId("chat"));

    return chatHistoryRepository
        .save(chatMsg)
        .flatMap(
            savedMsg -> {
              String chatId = savedMsg.getId();

              Map<String, Object> result = new HashMap<>();
              result.put("chat_id", chatId);
              result.put("message", message);
              result.put("needs_confirmation", false);

              return autonomousEngine
                  .validateAndQuestion(message, AutonomousQuestioningEngine.RequestType.GENERAL_AI)
                  .flatMap(
                      validation -> {
                        if (validation.getIntentType()
                            == AutonomousQuestioningEngine.IntentType.GREETING) {
                          // বাংলা মন্তব্য: হার্ডকোডেড গ্রিটিং মেসেজের বদলে ডাইনামিক কনফিগ ব্যবহার
                          // করা হচ্ছে।
                          return configService
                              .getEffectiveString(
                                  "chat.greeting.message",
                                  dynamicRegistryService.getMessage(
                                      "chat.greeting", "Hello! How can I help?"))
                              .flatMap(
                                  greetingMsg ->
                                      saveAiResponse(
                                          userId,
                                          greetingMsg,
                                          result,
                                          "local_greeting",
                                          null,
                                          message,
                                          validation.getIntentType()));
                        }

                        log.info(
                            "🎯 [Smart Hybrid Router] Routing non-greeting query directly to live web fallback: {}",
                            message);
                        return processWithWebFallback(
                            userId, message, result, validation.getIntentType());
                      });
            });
  }

  private Mono<Map<String, Object>> saveAiResponse(
      String userId,
      String responseContent,
      Map<String, Object> baseResult,
      String responseType,
      List<String> options,
      String originalMessage,
      AutonomousQuestioningEngine.IntentType intentType) {
    ChatMessage aiMsg = new ChatMessage();
    aiMsg.setId(generateId("chat_ai"));
    aiMsg.setUserId(userId);
    aiMsg.setContent(responseContent);
    aiMsg.setRole("ai");
    aiMsg.setTimestamp(LocalDateTime.now());

    baseResult.put("response", responseContent);
    baseResult.put("response_type", responseType);
    if (options != null && !options.isEmpty()) {
      baseResult.put("options", options);
    }

    if (Arrays.asList("llm_response", "magic_loop_combined_response").contains(responseType)) {
      if (intentType != AutonomousQuestioningEngine.IntentType.GREETING) {
        // বাংলা মন্তব্য: স্কিল এক্সট্রাকশন প্রম্পট এখন ডাইনামিক।
        String skillExtractionPrompt =
            dynamicRegistryService.getPrompt(
                "skill_extraction",
                "Analyze this interaction.\nUser: {originalMessage}\nResponse: {responseContent}\n\nCRITICAL INSTRUCTION: Extract ONLY the core technical SKILL and URL patterns. Format as a principle.",
                Map.of("originalMessage", originalMessage, "responseContent", responseContent));

        fallbackOrchestrator
            .executeWithSupremeIntelligence(
                "chat", "skill_extraction", skillExtractionPrompt, "system")
            .flatMap(
                // বাংলা মন্তব্য: লার্নিং প্রসেসটি এখন সরাসরি চেইন করা হয়েছে যাতে এটি "not saved" না
                // হয়।
                skillPattern ->
                    enhancedLearningService.learnFromInteraction(
                        userId, originalMessage, skillPattern))
            .doOnSuccess(v -> log.info("💡 Meta-Skill learned and verified for user: {}", userId))
            .subscribeOn(Schedulers.parallel())
            .subscribe(
                v -> {}, e -> log.error("⚠️ Learning persistence failed: {}", e.getMessage()));
      }
    }

    return chatHistoryRepository.save(aiMsg).thenReturn(baseResult);
  }

  private Mono<String> determineSearchUrl(String message) {
    // বাংলা মন্তব্য: DynamicRegistryService ব্যবহার করে সার্চ URL নির্ধারণ করা হচ্ছে।
    // এটি Firestore থেকে লোড করা ডাইনামিক ম্যাপিং ব্যবহার করবে।
    return Mono.just(dynamicRegistryService.determineSearchUrl(message))
        .map(
            url -> {
              // URL এনকোডিং নিশ্চিত করা হচ্ছে
              return url.replace(
                  "{query}",
                  java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8));
            });
  }

  private Mono<Map<String, Object>> processWithWebFallback(
      String userId,
      String message,
      Map<String, Object> baseResult,
      AutonomousQuestioningEngine.IntentType intentType) {
    Mono<String> webDataMono =
        determineSearchUrl(message)
            .flatMap(
                dynamicUrl -> {
                  if (dynamicUrl != null && !dynamicUrl.isEmpty()) {
                    log.info("🎯 [Level 3] AI dynamically generated target URL: {}", dynamicUrl);
                    return browserService.searchAndScrape("", "ai_directed", dynamicUrl);
                  }
                  return configService
                      .getEffectiveString("fallback.search.engine", "duckduckgo")
                      .flatMap(
                          engine ->
                              configService
                                  .getEffectiveString(
                                      "fallback.search.url." + engine,
                                      "https://html.duckduckgo.com/html/?q=")
                                  .flatMap(
                                      url -> browserService.searchAndScrape(message, engine, url)));
                });

    Mono<String> localDataMono = knowledgeService.getRelevantContext(message);

    return Mono.zip(localDataMono, webDataMono)
        .flatMap(
            tuple -> {
              String localData = tuple.getT1();
              String scrapedData = tuple.getT2();

              if ((scrapedData == null || scrapedData.isEmpty())
                  && localData.contains("No relevant local context")) {
                // বাংলা মন্তব্য: হার্ডকোড করা এরর মেসেজ ডাইনামিক করা হলো।
                return saveAiResponse(
                    userId,
                    dynamicRegistryService.getMessage(
                        "error.no_info", "I couldn't find information on this topic."),
                    baseResult,
                    "fallback_failed",
                    null,
                    message,
                    intentType);
              }

              String prompt =
                  "You are SupremeAI, an advanced agentic system.\n\n"
                      + "[PAST LEARNED SKILLS & ROUTING PATTERNS]\n"
                      + localData
                      + "\n\n"
                      + "[LIVE EXTERNAL DATA]\n"
                      + scrapedData
                      + "\n\n"
                      + "User Question: "
                      + message
                      + "\n\n"
                      + "CRITICAL INSTRUCTION: Do not rely on past factual memory. Use the PAST LEARNED SKILLS and the LIVE EXTERNAL DATA to answer the user's question directly. DO NOT explain your work process, DO NOT provide an architecture plan, and DO NOT explain how you found the answer. Just provide the perfect, up-to-date REAL answer concisely in their preferred language.";

              return fallbackOrchestrator
                  .executeWithSupremeIntelligence("chat", message, prompt, userId)
                  .onErrorResume(
                      e -> {
                        log.warn(
                            "⚠️ [Phase 4] Primary AI failed in Web Fallback. Triggering Multi-AI Failover. Error: {}",
                            e.getMessage());
                        return multiAIVotingService
                            .askContextualAIs(prompt, 3, 15000)
                            .map(com.supremeai.model.ConsensusResult::getConsensusAnswer)
                            .onErrorResume(
                                // বাংলা মন্তব্য: এআই ডাউন থাকাকালীন মেসেজটিও এখন ডাইনামিক।
                                fallbackErr ->
                                    Mono.just(
                                        dynamicRegistryService.getMessage(
                                            "error.ai_down",
                                            "AI systems are currently unavailable.")));
                      })
                  .flatMap(
                      aiResponse ->
                          saveAiResponse(
                              userId,
                              aiResponse,
                              baseResult,
                              "magic_loop_combined_response",
                              null,
                              message,
                              intentType));
            })
        .onErrorResume(
            e -> {
              log.error("Web fallback error: ", e);
              // বাংলা মন্তব্য: সার্চ এরর মেসেজ ডাইনামিক করা হলো।
              return saveAiResponse(
                  userId,
                  dynamicRegistryService.getMessage("error.search_failed", "Search error: ")
                      + e.getMessage(),
                  baseResult,
                  "error",
                  null,
                  message,
                  intentType);
            });
  }

  public Mono<List<Map<String, Object>>> getChatHistory(String userId, int limit) {
    return (userId != null
            ? chatHistoryRepository.findByUserId(userId)
            : chatHistoryRepository.findAll())
        .take(limit)
        .collectList()
        .map(this::convertChatMessageList);
  }

  private List<Map<String, Object>> convertChatMessageList(List<ChatMessage> messages) {
    List<Map<String, Object>> list = new ArrayList<>();
    if (messages == null) return list;
    for (ChatMessage msg : messages) {
      Map<String, Object> map = new HashMap<>();
      map.put("id", msg.getId());
      map.put("user_id", msg.getUserId());
      map.put("content", msg.getContent());
      map.put("role", msg.getRole());
      map.put("created_at", msg.getTimestamp() != null ? msg.getTimestamp().toString() : null);
      list.add(map);
    }
    return list;
  }

  private String generateId(String prefix) {
    return prefix
        + "_"
        + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSSS"));
  }

  public Mono<List<Map<String, Object>>> getPendingConfirmations(String userId) {
    return Mono.just(new ArrayList<>());
  }

  public Mono<Map<String, Object>> confirmItem(String itemId, Boolean confirmed, String userId) {
    return Mono.just(Map.of("success", true));
  }

  public Mono<List<Map<String, Object>>> getRules(boolean activeOnly) {
    return Mono.just(new ArrayList<>());
  }

  public Mono<Map<String, Object>> getItemById(String type, String id) {
    return Mono.just(new HashMap<>());
  }

  public Mono<List<Map<String, Object>>> getPlans(boolean activeOnly) {
    return Mono.just(new ArrayList<>());
  }

  public Mono<List<Map<String, Object>>> getCommands(boolean activeOnly) {
    return Mono.just(new ArrayList<>());
  }

  public Mono<List<Map<String, Object>>> getConfirmationHistory(String itemId, String chatId) {
    return Mono.just(new ArrayList<>());
  }
}
