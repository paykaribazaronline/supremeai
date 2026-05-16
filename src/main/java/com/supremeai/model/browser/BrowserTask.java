package com.supremeai.model.browser;

import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Document(collectionName = "browser_tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrowserTask {
    private String id;
    private String goal;
    private String status; // active, completed, failed
    private Integer progress; // 0-100
    private List<String> findings;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String lastUrl; // Current/last visited URL
    private String errorMessage;

    public BrowserTask(String goal) {
        this.goal = goal;
        this.status = "active";
        this.progress = 0;
        this.startedAt = LocalDateTime.now();
    }
}

