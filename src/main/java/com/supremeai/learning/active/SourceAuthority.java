package com.supremeai.learning.active;

/**
 * Source authority hierarchy for conflict resolution.
 * Higher weight sources override lower-weight sources when confidence scores are similar.
 */
public enum SourceAuthority {
    OFFICIAL_DOCS(1.0),
    GITHUB(0.85),
    STACK_OVERFLOW(0.80),
    WIKIPEDIA(0.75),
    BLOGS(0.65),
    FORUMS(0.50);

    private final double weight;

    SourceAuthority(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    /**
     * Get authority from source name string (best effort matching).
     */
    public static SourceAuthority fromSourceName(String sourceName) {
        if (sourceName == null) return FORUMS;
        String lower = sourceName.toLowerCase();
        if (lower.contains("official") || lower.contains("documentation")) return OFFICIAL_DOCS;
        if (lower.contains("github") || lower.contains("gitlab")) return GITHUB;
        if (lower.contains("stackoverflow") || lower.contains("stack exchange")) return STACK_OVERFLOW;
        if (lower.contains("wikipedia")) return WIKIPEDIA;
        if (lower.contains("blog")) return BLOGS;
        return FORUMS;
    }
}
