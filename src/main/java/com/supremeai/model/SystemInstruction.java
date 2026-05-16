package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "system_instructions")
public class SystemInstruction {
    @DocumentId
    private String id; // e.g., "app_generation_rules", "code_review_guidelines"
    private String title;
    private String content;
    private List<String> applicableTaskTypes;
    private boolean isActive;
    private int priority;
}
