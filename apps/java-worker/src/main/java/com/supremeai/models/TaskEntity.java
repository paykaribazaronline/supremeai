package com.supremeai.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "supreme_tasks")
public class TaskEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(name = "task_type", nullable = false)
    private String taskType;
    
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;
    
    @Column(name = "requested_by")
    private String requestedBy;
    
    @Column(nullable = false)
    private String status = "QUEUED"; // QUEUED, PROCESSING, COMPLETED, FAILED
    
    @Column(name = "result_json", columnDefinition = "TEXT")
    private String resultJson;
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
