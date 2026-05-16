package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collectionName = "monitoring_logs")
public class MonitoringLog {
    @DocumentId
    private String id;
    private String level; // INFO, WARN, ERROR, SUCCESS, ALERT
    private String component; // e.g., GitHub, Monitoring, Auth
    private String message;
    private long timestamp;
}
