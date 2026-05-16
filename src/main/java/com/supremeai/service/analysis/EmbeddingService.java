package com.supremeai.service.analysis;

import java.util.List;

public interface EmbeddingService {
    List<Double> generateEmbedding(String text);
    List<List<Double>> generateBatchEmbeddings(List<String> texts);
}
