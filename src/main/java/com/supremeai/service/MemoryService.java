package com.supremeai.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoryService {

  private final VectorStore vectorStore;

  /** Stores knowledge (e.g., codebase, notes, docs) into the Vector DB. */
  public void ingestKnowledge(String agentId, String contentId, String content) {
    log.info("Ingesting knowledge into memory for Agent: {}, Content ID: {}", agentId, contentId);

    // We attach metadata to the document so other agents know where it came from
    Document document =
        new Document(
            content,
            Map.of(
                "agentId", agentId,
                "contentId", contentId,
                "timestamp", System.currentTimeMillis()));

    // In a real scenario, we should chunk the content before storing to fit LLM context limits.
    // For simplicity, we are storing the whole content here.
    vectorStore.add(List.of(document));

    log.info("Successfully ingested {} characters.", content.length());
  }

  /** Retrieves relevant context based on a semantic search query. */
  public String retrieveRelevantContext(String query, int topK) {
    log.info("Retrieving memory for query: '{}' with top {} results", query, topK);

    List<Document> results =
        vectorStore.similaritySearch(SearchRequest.query(query).withTopK(topK));

    if (results.isEmpty()) {
      return "No relevant context found in memory.";
    }

    // Combine the results into a single context string
    String contextStr =
        results.stream()
            .map(
                doc ->
                    "Source ["
                        + doc.getMetadata().get("agentId")
                        + " | "
                        + doc.getMetadata().get("contentId")
                        + "]:\n"
                        + doc.getContent())
            .collect(Collectors.joining("\n\n---\n\n"));

    log.info("Found {} relevant documents.", results.size());
    return contextStr;
  }
}
