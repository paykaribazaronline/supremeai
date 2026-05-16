package com.supremeai.service.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class VectorSearchServiceTest {

    @Test
    void testCosineSimilarityIdentical() {
        List<Double> a = List.of(1.0, 2.0, 3.0);
        List<Double> b = List.of(1.0, 2.0, 3.0);

        VectorSearchService service = new VectorSearchService(null, null);
        double similarity = service.cosineSimilarity(a, b);

        assertEquals(1.0, similarity, 0.001);
    }

    @Test
    void testCosineSimilarityOrthogonal() {
        List<Double> a = List.of(1.0, 0.0, 0.0);
        List<Double> b = List.of(0.0, 1.0, 0.0);

        VectorSearchService service = new VectorSearchService(null, null);
        double similarity = service.cosineSimilarity(a, b);

        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void testCosineSimilarityOpposite() {
        List<Double> a = List.of(1.0, 2.0, 3.0);
        List<Double> b = List.of(-1.0, -2.0, -3.0);

        VectorSearchService service = new VectorSearchService(null, null);
        double similarity = service.cosineSimilarity(a, b);

        assertEquals(-1.0, similarity, 0.001);
    }

    @Test
    void testCosineSimilarityEmpty() {
        VectorSearchService service = new VectorSearchService(null, null);
        double similarity = service.cosineSimilarity(List.of(), List.of());
        assertEquals(0.0, similarity, 0.001);
    }

    @Test
    void testCosineSimilarityDifferentSizes() {
        List<Double> a = List.of(1.0, 2.0);
        List<Double> b = List.of(1.0, 2.0, 3.0);

        VectorSearchService service = new VectorSearchService(null, null);
        double similarity = service.cosineSimilarity(a, b);
        assertEquals(0.0, similarity, 0.001);
    }
}
