package com.supremeai.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AgentStatusEvent {
    private String agentId;
    private String newStatus;
}
