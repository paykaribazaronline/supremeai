package com.supremeai.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VectorStoreConfig {

  @Bean
  public VectorStore vectorStore(EmbeddingModel embeddingModel) {
    // We use a SimpleVectorStore (In-Memory) for easy local development without requiring external
    // DBs.
    // This will act as our "Centralized Memory" for all agents.
    // It uses the configured EmbeddingModel (e.g., OpenAI) to convert text into vectors.
    return new SimpleVectorStore(embeddingModel);
  }
}
