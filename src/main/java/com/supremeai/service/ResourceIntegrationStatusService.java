package com.supremeai.service;

import com.supremeai.service.analytics.PostHogService;
import com.supremeai.service.agent.ProximaAgentService;
import com.supremeai.service.memory.ClaudeMemAndMem0Service;
import com.supremeai.service.rag.LlamaIndexRagService;
import com.supremeai.service.redis.UpstashRedisService;
import com.supremeai.service.research.PerplexityResearchService;
import com.supremeai.service.security.ECCToolsService;
import com.supremeai.service.security.UltraplinianSecurityService;
import com.supremeai.service.vdb.PineconeVectorService;
import com.supremeai.service.vdb.QdrantVectorService;
import com.supremeai.service.vdb.TurbovecVectorService;
import com.supremeai.service.vdb.WeaviateVectorService;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Central read model for the external resource matrix in document/github_or_web_to_use.md. */
@Service
public class ResourceIntegrationStatusService {

  private final ClaudeMemAndMem0Service memoryService;
  private final PerplexityResearchService perplexityResearchService;
  private final LlamaIndexRagService llamaIndexRagService;
  private final PineconeVectorService pineconeVectorService;
  private final QdrantVectorService qdrantVectorService;
  private final TurbovecVectorService turbovecVectorService;
  private final WeaviateVectorService weaviateVectorService;
  private final UpstashRedisService upstashRedisService;
  private final PostHogService postHogService;
  private final ECCToolsService eccToolsService;
  private final UltraplinianSecurityService ultraplinianSecurityService;
  private final ProximaAgentService proximaAgentService;

  @Value("${sentry.dsn:}")
  private String sentryDsn;

  @Value("${groq.api.key:}")
  private String groqApiKey;

  @Value("${nvidia.dotapi.key:}")
  private String nvidiaDotapiKey;

  @Value("${higgsfield.api.key:}")
  private String higgsfieldApiKey;

  @Value("${openhuman.api.key:}")
  private String openHumanApiKey;

  @Value("${sync_in.api.url:}")
  private String syncInApiUrl;

  public ResourceIntegrationStatusService(
      ObjectProvider<ClaudeMemAndMem0Service> memoryService,
      ObjectProvider<PerplexityResearchService> perplexityResearchService,
      ObjectProvider<LlamaIndexRagService> llamaIndexRagService,
      ObjectProvider<PineconeVectorService> pineconeVectorService,
      ObjectProvider<QdrantVectorService> qdrantVectorService,
      ObjectProvider<TurbovecVectorService> turbovecVectorService,
      ObjectProvider<WeaviateVectorService> weaviateVectorService,
      ObjectProvider<UpstashRedisService> upstashRedisService,
      ObjectProvider<PostHogService> postHogService,
      ObjectProvider<ECCToolsService> eccToolsService,
      ObjectProvider<UltraplinianSecurityService> ultraplinianSecurityService,
      ObjectProvider<ProximaAgentService> proximaAgentService) {
    this.memoryService = memoryService.getIfAvailable();
    this.perplexityResearchService = perplexityResearchService.getIfAvailable();
    this.llamaIndexRagService = llamaIndexRagService.getIfAvailable();
    this.pineconeVectorService = pineconeVectorService.getIfAvailable();
    this.qdrantVectorService = qdrantVectorService.getIfAvailable();
    this.turbovecVectorService = turbovecVectorService.getIfAvailable();
    this.weaviateVectorService = weaviateVectorService.getIfAvailable();
    this.upstashRedisService = upstashRedisService.getIfAvailable();
    this.postHogService = postHogService.getIfAvailable();
    this.eccToolsService = eccToolsService.getIfAvailable();
    this.ultraplinianSecurityService = ultraplinianSecurityService.getIfAvailable();
    this.proximaAgentService = proximaAgentService.getIfAvailable();
  }

  public Map<String, Object> getStatusSnapshot() {
    Map<String, Object> status = new LinkedHashMap<>();
    status.put("claudeMem", configured(memoryService != null && memoryService.isClaudeMemConfigured()));
    status.put("mem0", configured(memoryService != null && memoryService.isMem0Configured()));
    status.put("perplexity", configured(perplexityResearchService != null && perplexityResearchService.isConfigured()));
    status.put("llamaIndex", configured(llamaIndexRagService != null && llamaIndexRagService.isConfigured()));
    status.put("pinecone", configured(pineconeVectorService != null && pineconeVectorService.isConfigured()));
    status.put("qdrant", configured(qdrantVectorService != null && qdrantVectorService.isConfigured()));
    status.put("turbovec", configured(turbovecVectorService != null && turbovecVectorService.isConfigured()));
    status.put("weaviate", configured(weaviateVectorService != null && weaviateVectorService.isConfigured()));
    status.put("upstash", configured(upstashRedisService != null && upstashRedisService.isConfigured()));
    status.put("postHog", configured(postHogService != null && postHogService.isConfigured()));
    status.put("sentry", configured(hasText(sentryDsn)));
    status.put("groq", configured(hasText(groqApiKey)));
    status.put("nvidiaDotapi", configured(hasText(nvidiaDotapiKey)));
    status.put("higgsfield", configured(hasText(higgsfieldApiKey)));
    status.put("openHuman", configured(hasText(openHumanApiKey)));
    status.put("syncIn", configured(hasText(syncInApiUrl)));
    status.put("ultraplinian", configured(ultraplinianSecurityService != null && ultraplinianSecurityService.isConfigured()));
    status.put("proxima", configured(proximaAgentService != null && proximaAgentService.isConfigured()));
    status.put("eccTools", configured(eccToolsService != null && eccToolsService.isAvailable()));
    return status;
  }

  private Map<String, Object> configured(boolean configured) {
    return available(configured, configured ? "ready" : "missing configuration");
  }

  private Map<String, Object> available(boolean available, String note) {
    return Map.of("available", available, "note", note);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }
}
