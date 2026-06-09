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
      MultiAIVotingService multiAIVotingService) {
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
  }

  public Mono<Map<String, Object>> processMessage(String userId, String message, boolean isAdmin) {
    String sanitizedMessage = Jsoup.clean(
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
                        if (validation.getIntentType() == AutonomousQuestioningEngine.IntentType.GREETING) {
                          return configService
                              .getEffectiveString(
                                  "chat.greeting.message",
                                  "Hello! I'm SupremeAI. How can I assist you with your project or coding today?")
                              .flatMap(
                                  greetingMsg -> saveAiResponse(
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
        String skillExtractionPrompt = "Analyze this interaction.\nUser: "
            + originalMessage
            + "\nResponse: "
            + responseContent
            + "\n\nCRITICAL INSTRUCTION: Extract ONLY the core technical SKILL (the 'Why' and 'How') and the best web SOURCE/URL pattern to find this type of info. Do NOT memorize the exact factual answer. Format as a reusable principle/routing rule for future queries.";
        fallbackOrchestrator
            .executeWithSupremeIntelligence(
                "chat", "skill_extraction", skillExtractionPrompt, "system")
            .flatMap(
                skillPattern -> enhancedLearningService.learnFromInteraction(
                    userId, originalMessage, skillPattern))
            .subscribeOn(Schedulers.parallel())
            .subscribe(
                v -> log.info("💡 Meta-Skill / Routing Pattern learned for user: {}", userId),
                e -> log.error("⚠️ Failed to learn meta-skill: {}", e.getMessage()));
      }
    }

    return chatHistoryRepository.save(aiMsg).thenReturn(baseResult);
  }

  private Mono<String> determineSearchUrl(String message) {
    // DYNAMIC DISCOVERY: Fetching targets from ConfigService instead of hardcoding
    return configService.getEffectiveString("agentic.web.targets", "")
        .map(targets -> {
          String query = message.toLowerCase();
          // Simple heuristic to find the best URL from the targets string
          String[] lines = targets.split("\n");
          for (String line : lines) {
            if (line.contains("(") && line.contains(")")) {
              String keywords = line.substring(line.indexOf("(") + 1, line.indexOf(")")).toLowerCase();
              if (Arrays.stream(keywords.split(" ")).anyMatch(query::contains)) {
                return line.split(" ")[1].replace("%s", "");
              }
            }
          }
          return "https://html.duckduckgo.com/html/?q=";
        })
        .map(baseUrl -> {
          try {
            String encoded = java.net.URLEncoder.encode(message, "UTF-8");
            return baseUrl + encoded;
          } catch (Exception e) {
            return "https://html.duckduckgo.com/html/?q=" + message;
          }
        });
  }

  private Mono<Map<String, Object>> processWithWebFallback(
      String userId,
      String message,
      Map<String, Object> baseResult,
      AutonomousQuestioningEngine.IntentType intentType) {
    Mono<String> webDataMono = determineSearchUrl(message)
        .flatMap(
            dynamicUrl -> {
              if (dynamicUrl != null && !dynamicUrl.isEmpty()) {
                log.info("🎯 [Level 3] AI dynamically generated target URL: {}", dynamicUrl);
                return browserService.searchAndScrape("", "ai_directed", dynamicUrl);
              }
              return configService
                  .getEffectiveString("fallback.search.engine", "duckduckgo")
                  .flatMap(
                      engine -> configService
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
                return saveAiResponse(
                    userId, // Changed to English
                    "Sorry, I couldn't find any information on this topic from the internet or local database.",
                    baseResult,
                    "fallback_failed",
                    null,
                    message,
                    intentType);
              }

              String prompt = "You are SupremeAI, an advanced agentic system.\n\n"
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
                                fallbackErr -> Mono.just(
                                    "Sorry, internet search was successful, but there was an issue with AI processing. (All AIs Down)")); // Changed
                                                                                                                                          // to
                                                                                                                                          // English
                      })
                  .flatMap(
                      aiResponse -> saveAiResponse(
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
              return saveAiResponse(
                  userId,
                  "An issue occurred while performing the internet search: " + e.getMessage(), // Changed to English
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
    if (messages == null)
      return list;
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
