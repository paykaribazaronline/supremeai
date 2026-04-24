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
public class ReasoningLog {
    private String id;
    private String taskId;
    private String decision;
    private String reason;
    private String modelName;
    private String status;
    private LocalDateTime timestamp;
    private String additionalMetadata;
}
