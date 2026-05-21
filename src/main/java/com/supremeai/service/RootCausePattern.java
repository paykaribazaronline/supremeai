package com.supremeai.service;

import java.util.regex.Pattern;

public class RootCausePattern {
    public final String id;
    public final String name;
    public final String description;
    public final Pattern pattern;
    public final CorrectionAction suggestedAction;
    public final double confidence;

    public RootCausePattern(String id, String name, Pattern pattern, CorrectionAction action, double confidence) {
        this.id = id;
        this.name = name;
        this.description = name;
        this.pattern = pattern;
        this.suggestedAction = action;
        this.confidence = confidence;
    }
}
