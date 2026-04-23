package com.supremeai.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ClarificationResponse {
    private boolean needsClarification;
    private List<String> questions;
    private String suggestedApproach;
    private String directResponse;
}
