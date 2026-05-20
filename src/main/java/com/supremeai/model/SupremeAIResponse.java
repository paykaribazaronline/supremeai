package com.supremeai.model;

import com.supremeai.service.RootCauseAnalysisService;

public class SupremeAIResponse {
    private final boolean success;
    private final String message;
    private final Object data;
    private final RootCauseAnalysisService.RootCauseAnalysis rootCauseAnalysis;

    public SupremeAIResponse(boolean success, String message, Object data) {
        this(success, message, data, null);
    }

    public SupremeAIResponse(boolean success, String message, Object data, RootCauseAnalysisService.RootCauseAnalysis rootCauseAnalysis) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.rootCauseAnalysis = rootCauseAnalysis;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
    public RootCauseAnalysisService.RootCauseAnalysis getRootCauseAnalysis() { return rootCauseAnalysis; }
}
