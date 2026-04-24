package com.supremeai.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeEntry {
    private String id;
    private String topic;
    private String pattern;
    private String solution;
    private String sourceProvider;
    private double confidenceScore;
    private LocalDateTime createdAt;
}
