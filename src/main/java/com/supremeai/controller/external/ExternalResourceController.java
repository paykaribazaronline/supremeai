package com.supremeai.controller.external;

import com.supremeai.provider.GeminiProvider;
import com.supremeai.provider.GroqProvider;
import com.supremeai.provider.HuggingFaceProvider;
import com.supremeai.provider.NvidiaNimProvider;
import com.supremeai.provider.TogetherAIProvider;
import com.supremeai.service.ClaudeMemoryService;
import com.supremeai.service.FirecrawlService;
import com.supremeai.service.FreeLLMService;
import com.supremeai.service.HiggsfieldVideoService;
import com.supremeai.service.ModalServerlessService;
import com.supremeai.service.N8nIntegrationService;
import com.supremeai.service.ReplicateModelService;
import com.supremeai.service.SupremeAIBrain;
import com.supremeai.service.agent.ProximaAgentService;
import com.supremeai.service.analytics.PostHogService;
import com.supremeai.service.avatar.OpenHumanService;
import com.supremeai.service.cli.AnusCliService;
import com.supremeai.service.github.GitHubSkillsService;
import com.supremeai.service.knowledge.LongCatAIService;
import com.supremeai.service.knowledge.UnslothTrainingService;
import com.supremeai.service.logic.FabricLogicEngineService;
import com.supremeai.service.logic.LangChainOrchestrationService;
import com.supremeai.service.memory.ClaudeMemAndMem0Service;
import com.supremeai.service.rag.LlamaIndexRagService;
import com.supremeai.service.redis.UpstashRedisService;
import com.supremeai.service.research.PerplexityResearchService;
import com.supremeai.service.schema.ManifestService;
import com.supremeai.service.scraper.FirecrawlScraperService;
import com.supremeai.service.security.ECCToolsService;
import com.supremeai.service.security.Godmod3OdysseusService;
import com.supremeai.service.security.UltraplinianSecurityService;
import com.supremeai.service.sync.SyncInService;
import com.supremeai.service.vdb.CohereEmbeddingService;
import com.supremeai.service.vdb.PineconeVectorService;
import com.supremeai.service.vdb.QdrantVectorService;
import com.supremeai.service.vdb.TurbovecVectorService;
import com.supremeai.service.vdb.WeaviateVectorService;
import com.supremeai.service.visionlocal.MiroFishOfflineService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/** Unified External Resource Controller exposing all 37+ integrations. */
@RestController
@RequestMapping("/api/external/v1")
public class ExternalResourceController {

  private static final Logger logger = LoggerFactory.getLogger(ExternalResourceController.class);

  @Autowired(required = false)
  private FirecrawlService firecrawlService;

  @Autowired(required = false)
  private FirecrawlScraperService firecrawlScraper;

  @Autowired(required = false)
  private ClaudeMemoryService claudeMemoryService;

  @Autowired(required = false)
  private ClaudeMemAndMem0Service mem0Service;

  @Autowired(required = false)
  private FabricLogicEngineService fabricService;

  @Autowired(required = false)
  private LlamaIndexRagService llamaIndexService;

  @Autowired(required = false)
  private ProximaAgentService proximaService;

  @Autowired(required = false)
  private UltraplinianSecurityService ultraplinianService;

  @Autowired(required = false)
  private PerplexityResearchService perplexityService;

  @Autowired(required = false)
  private TurbovecVectorService turbovecService;

  @Autowired(required = false)
  private PineconeVectorService pineconeService;

  @Autowired(required = false)
  private LongCatAIService longcatService;

  @Autowired(required = false)
  private Godmod3OdysseusService g0dm0d3Service;

  @Autowired(required = false)
  private N8nIntegrationService n8nService;

  @Autowired(required = false)
  private HuggingFaceProvider huggingFaceProvider;

  @Autowired(required = false)
  private GeminiProvider geminiProvider;

  @Autowired(required = false)
  private GroqProvider groqProvider;

  @Autowired(required = false)
  private SupremeAIBrain supremeAIBrain;

  @Autowired(required = false)
  private UnslothTrainingService unslothService;

  @Autowired(required = false)
  private HiggsfieldVideoService higgsfieldService;

  @Autowired(required = false)
  private FreeLLMService freeLLMService;

  @Autowired(required = false)
  private LangChainOrchestrationService langchainService;

  @Autowired(required = false)
  private CohereEmbeddingService cohereService;

  @Autowired(required = false)
  private QdrantVectorService qdrantService;

  @Autowired(required = false)
  private ReplicateModelService replicateService;

  @Autowired(required = false)
  private ModalServerlessService modalService;

  @Autowired(required = false)
  private WeaviateVectorService weaviateService;

  @Autowired(required = false)
  private UpstashRedisService upstashService;

  @Autowired(required = false)
  private OpenHumanService openHumanService;

  @Autowired(required = false)
  private ManifestService manifestService;

  @Autowired(required = false)
  private SyncInService syncInService;

  @Autowired(required = false)
  private AnusCliService anusCliService;

  @Autowired(required = false)
  private GitHubSkillsService githubSkillsService;

  @Autowired(required = false)
  private MiroFishOfflineService miroFishService;

  @Autowired(required = false)
  private PostHogService postHogService;

  @Autowired(required = false)
  private NvidiaNimProvider nvidiaNimProvider;

  @Autowired(required = false)
  private TogetherAIProvider togetherAIProvider;

  @Autowired(required = false)
  private ECCToolsService eccToolsService;

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    Map<String, Object> status =
        Map.ofEntries(
            Map.entry("firecrawl", firecrawlService != null),
            Map.entry("claude_mem", claudeMemoryService != null),
            Map.entry("mem0", mem0Service != null && mem0Service.isMem0Configured()),
            Map.entry("fabric", fabricService != null),
            Map.entry("llamaindex", llamaIndexService != null),
            Map.entry("proxima", proximaService != null && proximaService.isConfigured()),
            Map.entry("ultraplinian", ultraplinianService != null),
            Map.entry("perplexity", perplexityService != null && perplexityService.isConfigured()),
            Map.entry("turbovec", turbovecService != null && turbovecService.isConfigured()),
            Map.entry("pinecone", pineconeService != null && pineconeService.isConfigured()),
            Map.entry("longcat", longcatService != null && longcatService.isConfigured()),
            Map.entry("g0dm0d3_odysseus", g0dm0d3Service != null),
            Map.entry("n8n", n8nService != null),
            Map.entry("huggingface", huggingFaceProvider != null),
            Map.entry("gemini", geminiProvider != null),
            Map.entry("groq", groqProvider != null),
            Map.entry("unsloth", unslothService != null && unslothService.isConfigured()),
            Map.entry("higgsfield", higgsfieldService != null && higgsfieldService.isConfigured()),
            Map.entry("free_llm", freeLLMService != null && freeLLMService.isConfigured()),
            Map.entry("langchain", langchainService != null && langchainService.isConfigured()),
            Map.entry("cohere", cohereService != null && cohereService.isConfigured()),
            Map.entry("qdrant", qdrantService != null && qdrantService.isConfigured()),
            Map.entry("replicate", replicateService != null && replicateService.isConfigured()),
            Map.entry("modal", modalService != null && modalService.isConfigured()),
            Map.entry("weaviate", weaviateService != null && weaviateService.isConfigured()),
            Map.entry("upstash", upstashService != null && upstashService.isConfigured()),
            Map.entry("openhuman", openHumanService != null && openHumanService.isConfigured()),
            Map.entry("manifest", manifestService != null && manifestService.isConfigured()),
            Map.entry("syncin", syncInService != null && syncInService.isConfigured()),
            Map.entry("anus_cli", anusCliService != null && anusCliService.isConfigured()),
            Map.entry(
                "github_skills", githubSkillsService != null && githubSkillsService.isConfigured()),
            Map.entry("mirofish", miroFishService != null && miroFishService.isConfigured()),
            Map.entry("posthog", postHogService != null && postHogService.isConfigured()),
            Map.entry("nvidia_nim", nvidiaNimProvider != null),
            Map.entry("together_ai", togetherAIProvider != null),
            Map.entry("ecc_tools", eccToolsService != null && eccToolsService.isAvailable()));
    return ResponseEntity.ok(status);
  }

  @PostMapping("/firecrawl/scrape")
  public Mono<ResponseEntity<Map<String, Object>>> firecrawlScrape(
      @RequestBody Map<String, Object> payload) {
    if (firecrawlScraper == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    String url = (String) payload.get("url");
    try {
      var result = firecrawlScraper.scrape(url);
      return Mono.just(
          ResponseEntity.ok(
              Map.of(
                  "url",
                  result.url(),
                  "content",
                  result.content(),
                  "metadata",
                  result.metadata())));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/memory/claude/store")
  public Mono<ResponseEntity<Map<String, Object>>> storeClaudeMemory(
      @RequestBody Map<String, Object> payload) {
    if (mem0Service == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    String userId = (String) payload.get("user_id");
    String content = (String) payload.get("content");
    try {
      mem0Service.storeClaudeMemory(userId, content);
      return Mono.just(ResponseEntity.ok(Map.of("status", "stored")));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/memory/search")
  public Mono<ResponseEntity<Map<String, Object>>> searchMemory(
      @RequestBody Map<String, Object> payload) {
    if (mem0Service == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    String userId = (String) payload.get("user_id");
    String query = (String) payload.get("query");
    int limit = payload.containsKey("limit") ? ((Number) payload.get("limit")).intValue() : 5;
    try {
      var results = mem0Service.searchMem0(userId, query, limit);
      return Mono.just(ResponseEntity.ok(Map.of("results", results)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/fabric/apply")
  public Mono<ResponseEntity<Map<String, Object>>> applyFabricPattern(
      @RequestBody Map<String, Object> payload) {
    if (fabricService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    String pattern = (String) payload.get("pattern");
    String input = (String) payload.get("input");
    try {
      String result = fabricService.applyPattern(pattern, input);
      return Mono.just(ResponseEntity.ok(Map.of("pattern", pattern, "result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @GetMapping("/fabric/patterns")
  public ResponseEntity<Map<String, Object>> listFabricPatterns() {
    if (fabricService == null) return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    return ResponseEntity.ok(fabricService.listPatternsMetadata());
  }

  @PostMapping("/llamaindex/ingest")
  public Mono<ResponseEntity<Map<String, Object>>> llamaindexIngest(
      @RequestBody Map<String, Object> payload) {
    if (llamaIndexService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          llamaIndexService.ingestDocument(
              (String) payload.get("index_name"),
              (String) payload.get("text"),
              (Map<String, Object>) payload.getOrDefault("metadata", Map.of()));
      return Mono.just(ResponseEntity.ok(Map.of("status", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/llamaindex/query")
  public Mono<ResponseEntity<Map<String, Object>>> llamaindexQuery(
      @RequestBody Map<String, Object> payload) {
    if (llamaIndexService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      int topK = payload.containsKey("top_k") ? ((Number) payload.get("top_k")).intValue() : 5;
      var results =
          llamaIndexService.queryIndex(
              (String) payload.get("index_name"), (String) payload.get("query"), topK);
      return Mono.just(ResponseEntity.ok(Map.of("results", results)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/proxima/launch")
  public Mono<ResponseEntity<Map<String, Object>>> launchProximaAgent(
      @RequestBody Map<String, Object> payload) {
    if (proximaService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          proximaService.launchAgent((String) payload.get("task"), (String) payload.get("context"));
      return Mono.just(ResponseEntity.ok(Map.of("status", "launched", "result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/perplexity/search")
  public Mono<ResponseEntity<Map<String, Object>>> perplexitySearch(
      @RequestBody Map<String, Object> payload) {
    if (perplexityService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      int maxTokens =
          payload.containsKey("max_tokens")
              ? ((Number) payload.get("max_tokens")).intValue()
              : 2048;
      String result = perplexityService.search((String) payload.get("query"), maxTokens);
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/turbovec/upsert")
  public Mono<ResponseEntity<Map<String, Object>>> turbovecUpsert(
      @RequestBody Map<String, Object> payload) {
    if (turbovecService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      @SuppressWarnings("unchecked")
      List<Float> vector = (List<Float>) payload.get("vector");
      String result =
          turbovecService.upsert(
              (String) payload.get("collection"),
              (String) payload.get("id"),
              vector,
              (Map<String, Object>) payload.getOrDefault("metadata", Map.of()));
      return Mono.just(ResponseEntity.ok(Map.of("status", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/pinecone/upsert")
  public Mono<ResponseEntity<Map<String, Object>>> pineconeUpsert(
      @RequestBody Map<String, Object> payload) {
    if (pineconeService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      @SuppressWarnings("unchecked")
      List<Float> vector = (List<Float>) payload.get("vector");
      String result =
          pineconeService.upsert(
              (String) payload.get("id"),
              vector,
              (Map<String, Object>) payload.getOrDefault("metadata", Map.of()));
      return Mono.just(ResponseEntity.ok(Map.of("status", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/longcat/query")
  public Mono<ResponseEntity<Map<String, Object>>> longcatQuery(
      @RequestBody Map<String, Object> payload) {
    if (longcatService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result = longcatService.queryLongContext((String) payload.get("prompt"));
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/unsloth/finetune")
  public Mono<ResponseEntity<Map<String, Object>>> submitFineTuning(
      @RequestBody Map<String, Object> payload) {
    if (unslothService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      @SuppressWarnings("unchecked")
      Map<String, Object> hparams =
          (Map<String, Object>) payload.getOrDefault("hyperparameters", Map.of());
      String result =
          unslothService.submitFineTuning(
              (String) payload.get("model_id"), (String) payload.get("dataset_ref"), hparams);
      return Mono.just(ResponseEntity.ok(Map.of("status", "submitted", "result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/higgsfield/generate")
  public Mono<ResponseEntity<Map<String, Object>>> generateVideo(
      @RequestBody Map<String, Object> payload) {
    if (higgsfieldService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      int duration =
          payload.containsKey("duration") ? ((Number) payload.get("duration")).intValue() : 30;
      String result = higgsfieldService.generateVideo((String) payload.get("prompt"), duration);
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/free-llm/chat")
  public Mono<ResponseEntity<Map<String, Object>>> freeLLMChat(
      @RequestBody Map<String, Object> payload) {
    if (freeLLMService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          freeLLMService.chat(
              payload.containsKey("model") ? (String) payload.get("model") : "default",
              (String) payload.get("prompt"));
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/langchain/run")
  public Mono<ResponseEntity<Map<String, Object>>> langchainRun(
      @RequestBody Map<String, Object> payload) {
    if (langchainService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          langchainService.runChain(
              (String) payload.get("chain_name"), (String) payload.get("input"));
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/cohere/embed")
  public Mono<ResponseEntity<Map<String, Object>>> cohereEmbed(
      @RequestBody Map<String, Object> payload) {
    if (cohereService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      List<Double> vec = cohereService.embed((String) payload.get("text"));
      return Mono.just(ResponseEntity.ok(Map.of("embedding", vec)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/qdrant/upsert")
  public Mono<ResponseEntity<Map<String, Object>>> qdrantUpsert(
      @RequestBody Map<String, Object> payload) {
    if (qdrantService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      @SuppressWarnings("unchecked")
      List<Float> vector = (List<Float>) payload.get("vector");
      String result =
          qdrantService.upsert(
              (String) payload.get("collection"),
              (String) payload.get("id"),
              vector,
              (Map<String, Object>) payload.getOrDefault("metadata", Map.of()));
      return Mono.just(ResponseEntity.ok(Map.of("status", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/replicate/run")
  public Mono<ResponseEntity<Map<String, Object>>> replicateRun(
      @RequestBody Map<String, Object> payload) {
    if (replicateService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          replicateService.runModel(
              (String) payload.get("version"),
              (Map<String, Object>) payload.getOrDefault("input", Map.of()));
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/modal/invoke")
  public Mono<ResponseEntity<Map<String, Object>>> modalInvoke(
      @RequestBody Map<String, Object> payload) {
    if (modalService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          modalService.invoke(
              (String) payload.get("function"),
              (Map<String, Object>) payload.getOrDefault("input", Map.of()));
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/weaviate/upsert")
  public Mono<ResponseEntity<Map<String, Object>>> weaviateUpsert(
      @RequestBody Map<String, Object> payload) {
    if (weaviateService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          weaviateService.upsert(
              (String) payload.get("class"),
              (Map<String, Object>) payload.get("properties"),
              (String) payload.get("id"));
      return Mono.just(ResponseEntity.ok(Map.of("status", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/upstash/{command}")
  public Mono<ResponseEntity<Map<String, Object>>> upstashCommand(
      @PathVariable String command, @RequestBody Map<String, Object> payload) {
    if (upstashService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String key = (String) payload.get("key");
      String value = (String) payload.get("value");
      String result =
          switch (command.toLowerCase()) {
            case "get" -> upstashService.get(key);
            case "set" -> upstashService.set(key, value);
            case "del", "delete" -> upstashService.delete(key);
            default -> "unknown_command";
          };
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/openhuman/generate")
  public Mono<ResponseEntity<Map<String, Object>>> openHumanGenerate(
      @RequestBody Map<String, Object> payload) {
    if (openHumanService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result = openHumanService.generateAvatar(payload);
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/manifest/migrate")
  public Mono<ResponseEntity<Map<String, Object>>> manifestMigrate(
      @RequestBody Map<String, Object> payload) {
    if (manifestService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result = manifestService.migrateSchema((String) payload.get("manifest"));
      return Mono.just(ResponseEntity.ok(Map.of("status", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/syncin/sync")
  public Mono<ResponseEntity<Map<String, Object>>> syncInSync(
      @RequestBody Map<String, Object> payload) {
    if (syncInService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result = syncInService.sync((String) payload.get("dataset_id"), payload);
      return Mono.just(ResponseEntity.ok(Map.of("status", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/anuscli/run")
  public Mono<ResponseEntity<Map<String, Object>>> anusCliRun(
      @RequestBody Map<String, Object> payload) {
    if (anusCliService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      @SuppressWarnings("unchecked")
      List<String> args = (List<String>) payload.getOrDefault("args", List.of());
      String result = anusCliService.runScript((String) payload.get("script"), args);
      return Mono.just(ResponseEntity.ok(Map.of("output", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/github/create-pr")
  public Mono<ResponseEntity<Map<String, Object>>> createGitHubPR(
      @RequestBody Map<String, Object> payload) {
    if (githubSkillsService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result =
          githubSkillsService.createPullRequest(
              (String) payload.get("owner"),
              (String) payload.get("repo"),
              (String) payload.get("title"),
              (String) payload.get("head"),
              (String) payload.get("base"));
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/mirofish/detect")
  public Mono<ResponseEntity<Map<String, Object>>> miroFishDetect(
      @RequestBody Map<String, Object> payload) {
    if (miroFishService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      String result = miroFishService.detect((String) payload.get("image_path"));
      return Mono.just(ResponseEntity.ok(Map.of("result", result)));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @PostMapping("/ecc/generate-keypair")
  public Mono<ResponseEntity<Map<String, Object>>> eccGenerateKeypair() {
    if (eccToolsService == null)
      return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build());
    try {
      Map<String, Object> kp = eccToolsService.generateKeyPair();
      return Mono.just(ResponseEntity.ok(kp));
    } catch (Exception e) {
      return Mono.just(
          ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body(Map.of("error", e.getMessage())));
    }
  }

  @GetMapping("/health")
  public ResponseEntity<Map<String, String>> healthCheck() {
    return ResponseEntity.ok(Map.of("status", "ok", "service", "supremeai-external-integrations"));
  }
}
