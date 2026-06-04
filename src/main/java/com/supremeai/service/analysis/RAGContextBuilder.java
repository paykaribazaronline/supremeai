package com.supremeai.service.analysis;

import com.supremeai.model.analysis.CodeChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RAGContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(RAGContextBuilder.class);
    private static final int DEFAULT_TOP_K = 10;
    private static final int MAX_TOKENS_PER_AGENT = 8000;
    private static final int CONTEXT_LINES = 5;
    private static final double AVG_TOKENS_PER_LINE = 4.0;

    private final VectorSearchService vectorSearchService;

    public RAGContextBuilder(VectorSearchService vectorSearchService) {
        this.vectorSearchService = vectorSearchService;
    }

    public RAGContext buildContext(String projectId, String agentQuery, List<String> allFiles) {
        return buildContext(projectId, agentQuery, allFiles, DEFAULT_TOP_K, MAX_TOKENS_PER_AGENT);
    }

     public RAGContext buildContext(String projectId, String agentQuery, List<String> allFiles, int topK, int maxTokens) {
         List<CodeChunk> relevantChunks = vectorSearchService.searchSimilarChunks(projectId, agentQuery, topK);

         if (relevantChunks.isEmpty()) {
             log.debug("No relevant chunks found for query, falling back to file scan");
             return new RAGContext(
                 agentQuery,
                 List.of(),
                 List.of(),
                 0,
                 0,
                 allFiles.size()
             );
         }

         List<ContextChunk> contextChunks = new ArrayList<>();
         int totalTokens = 0;

         for (CodeChunk chunk : relevantChunks) {
             int estimatedTokens = (int) (chunk.getContent().split("\n").length * AVG_TOKENS_PER_LINE);

             if (totalTokens + estimatedTokens > maxTokens) {
                 break;
             }

             contextChunks.add(new ContextChunk(
                 chunk.getFile(),
                 chunk.getStartLine(),
                 chunk.getEndLine(),
                 chunk.getContent(),
                 1.0
             ));

             totalTokens += estimatedTokens;
         }

         List<String> contextFiles = contextChunks.stream()
             .map(ContextChunk::getFile)
             .distinct()
             .collect(Collectors.toList());

         return new RAGContext(
             agentQuery,
             contextChunks,
             contextFiles,
             totalTokens,
             contextFiles.size(),
             allFiles.size()
         );
     }

     public RAGContext buildContextForFile(String projectId, String filePath, int maxTokens) {
         List<CodeChunk> chunks = vectorSearchService.searchSimilarChunks(projectId, filePath, DEFAULT_TOP_K);

         List<ContextChunk> contextChunks = chunks.stream()
             .filter(c -> c.getFile().equals(filePath))
             .map(c -> new ContextChunk(
                 c.getFile(),
                 c.getStartLine(),
                 c.getEndLine(),
                 c.getContent(),
                 1.0
             ))
             .collect(Collectors.toList());

         int totalTokens = (int) (contextChunks.stream()
             .mapToInt(c -> c.getContent().split("\n").length)
             .sum() * AVG_TOKENS_PER_LINE);

         return new RAGContext(
             "analyze:" + filePath,
             contextChunks,
             List.of(filePath),
             totalTokens,
             1,
             1
         );
     }

     public static class RAGContext {
         private String agentQuery;
         private List<ContextChunk> relevantChunks;
         private List<String> contextFiles;
         private int totalTokens;
         private int filesScanned;
         private int totalFiles;

         public RAGContext() {}

         public RAGContext(String agentQuery, List<ContextChunk> relevantChunks, List<String> contextFiles, int totalTokens, int filesScanned, int totalFiles) {
             this.agentQuery = agentQuery;
             this.relevantChunks = relevantChunks;
             this.contextFiles = contextFiles;
             this.totalTokens = totalTokens;
             this.filesScanned = filesScanned;
             this.totalFiles = totalFiles;
         }

         public String getAgentQuery() { return agentQuery; }
         public void setAgentQuery(String agentQuery) { this.agentQuery = agentQuery; }

         public List<ContextChunk> getRelevantChunks() { return relevantChunks; }
         public void setRelevantChunks(List<ContextChunk> relevantChunks) { this.relevantChunks = relevantChunks; }

         public List<String> getContextFiles() { return contextFiles; }
         public void setContextFiles(List<String> contextFiles) { this.contextFiles = contextFiles; }

         public int getTotalTokens() { return totalTokens; }
         public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }

         public int getFilesScanned() { return filesScanned; }
         public void setFilesScanned(int filesScanned) { this.filesScanned = filesScanned; }

         public int getTotalFiles() { return totalFiles; }
         public void setTotalFiles(int totalFiles) { this.totalFiles = totalFiles; }
     }

     public static class ContextChunk {
         private String file;
         private int startLine;
         private int endLine;
         private String content;
         private double relevanceScore;

         public ContextChunk() {}

         public ContextChunk(String file, int startLine, int endLine, String content, double relevanceScore) {
             this.file = file;
             this.startLine = startLine;
             this.endLine = endLine;
             this.content = content;
             this.relevanceScore = relevanceScore;
         }

         public String getFile() { return file; }
         public void setFile(String file) { this.file = file; }

         public int getStartLine() { return startLine; }
         public void setStartLine(int startLine) { this.startLine = startLine; }

         public int getEndLine() { return endLine; }
         public void setEndLine(int endLine) { this.endLine = endLine; }

         public String getContent() { return content; }
         public void setContent(String content) { this.content = content; }

         public double getRelevanceScore() { return relevanceScore; }
         public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }
     }
}

