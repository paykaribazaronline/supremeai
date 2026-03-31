package org.example.service;

import org.example.model.DecisionTimeline;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Phase 6 Week 5-6: Timeline Renderer Service
 * Renders decision timelines as HTML/SVG with color coding
 * 
 * Features:
 * - HTML timeline rendering with CSS styling
 * - SVG-based visual timeline with hover tooltips
 * - Interactive drill-down capability
 * - Responsive design support
 * - Integration with 3D dashboard
 */
@Service
public class TimelineRenderer {

    // ==================== Timeline Rendering ====================

    /**
     * Render timeline as interactive HTML with CSS styling
     */
    public String renderTimelineHTML(DecisionTimeline timeline) {
        if (timeline.getEntries().isEmpty()) {
            return "<div class=\"timeline-empty\">No decisions recorded</div>";
        }

        StringBuilder html = new StringBuilder();
        html.append(getHTMLHeader());
        html.append(renderTimelineContainer(timeline));
        html.append(getHTMLFooter());

        return html.toString();
    }

    /**
     * Render timeline as SVG for visual representation
     */
    public String renderTimelineSVG(DecisionTimeline timeline) {
        if (timeline.getEntries().isEmpty()) {
            return "<svg class=\"timeline-empty\"><text>No data</text></svg>";
        }

        int width = 1200;
        int height = 400;
        int entryCount = Math.min(timeline.getEntries().size(), 20); // Max 20 visible
        int spacing = width / (entryCount + 1);

        StringBuilder svg = new StringBuilder();
        svg.append(String.format("<svg width=\"%d\" height=\"%d\" class=\"timeline-svg\">%n", width, height));
        svg.append(getTimelineAxis(width, height, entryCount, spacing));
        
        int index = 0;
        for (DecisionTimeline.TimelineEntry entry : timeline.getEntries()) {
            if (index >= 20) break;
            int x = spacing * (index + 1);
            int y = height / 2;
            svg.append(renderSVGEntry(entry, x, y, index));
            index++;
        }

        svg.append("</svg>%n");
        return svg.toString();
    }

    /**
     * Render timeline with statistics dashboard
     */
    public String renderTimelineWithStats(DecisionTimeline timeline) {
        StringBuilder html = new StringBuilder();
        html.append(getHTMLHeader());
        html.append("<div class=\"timeline-wrapper\">\n");
        html.append(renderStatsDashboard(timeline.getStats()));
        html.append(renderTimelineContainer(timeline));
        html.append("</div>\n");
        html.append(getHTMLFooter());
        return html.toString();
    }

    /**
     * Render compact timeline for dashboard integration
     */
    public String renderCompactTimeline(DecisionTimeline timeline, int maxEntries) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"compact-timeline\">\n");
        
        List<DecisionTimeline.TimelineEntry> entries = timeline.getEntries().stream()
            .limit(maxEntries)
            .toList();

        for (DecisionTimeline.TimelineEntry entry : entries) {
            html.append(renderCompactEntry(entry));
        }

        html.append("</div>\n");
        return html.toString();
    }

    // ==================== Component Rendering ====================

    /**
     * Render complete timeline container with all entries
     */
    private String renderTimelineContainer(DecisionTimeline timeline) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"timeline-container\">\n");
        html.append("<h3 class=\"timeline-title\">Decision Timeline</h3>\n");
        html.append("<div class=\"timeline\">\n");
        html.append("<div class=\"timeline-axis\"></div>\n");

        for (DecisionTimeline.TimelineEntry entry : timeline.getEntries()) {
            html.append(renderEntry(entry));
        }

        html.append("</div>\n");
        html.append("</div>\n");
        return html.toString();
    }

    /**
     * Render single timeline entry
     */
    private String renderEntry(DecisionTimeline.TimelineEntry entry) {
        String color = getColorClass(entry.color);
        long timeAgo = System.currentTimeMillis() - entry.timestamp;
        String timeAgoStr = formatTimeAgo(timeAgo);

        return String.format(
            "<div class=\"timeline-entry %s\" data-decision-id=\"%s\">\n" +
            "  <div class=\"timeline-marker\" title=\"%s\"></div>\n" +
            "  <div class=\"timeline-content\">\n" +
            "    <h4 class=\"timeline-agent\">%s</h4>\n" +
            "    <p class=\"timeline-decision\">%s</p>\n" +
            "    <p class=\"timeline-reasoning\">%s</p>\n" +
            "    <div class=\"timeline-meta\">\n" +
            "      <span class=\"timeline-outcome %s\">%s</span>\n" +
            "      <span class=\"timeline-confidence\">Confidence: %.1f%%</span>\n" +
            "      <span class=\"timeline-time\" title=\"%d\">%s ago</span>\n" +
            "    </div>\n" +
            "  </div>\n" +
            "</div>\n",
            color, entry.decisionId, entry.outcome,
            escapeHTML(entry.agent),
            escapeHTML(entry.decision),
            escapeHTML(entry.reasoning),
            color, entry.outcome,
            entry.confidence * 100,
            entry.timestamp, timeAgoStr
        );
    }

    /**
     * Render compact entry for dashboard
     */
    private String renderCompactEntry(DecisionTimeline.TimelineEntry entry) {
        String color = getColorClass(entry.color);
        return String.format(
            "<div class=\"compact-entry %s\">\n" +
            "  <span class=\"agent\">%s</span>\n" +
            "  <span class=\"decision\">%s</span>\n" +
            "  <span class=\"outcome\">%s</span>\n" +
            "</div>\n",
            color, entry.agent, entry.decision, entry.outcome
        );
    }

    /**
     * Render SVG entry point
     */
    private String renderSVGEntry(DecisionTimeline.TimelineEntry entry, int x, int y, int index) {
        String color = getNumberColor(entry.color);
        float radius = 8;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(
            "<circle cx=\"%d\" cy=\"%d\" r=\"%.1f\" fill=\"%s\" class=\"timeline-point\" data-index=\"%d\"/>\n",
            x, y, radius, color, index
        ));

        // Add tooltip
        svg.append(String.format(
            "<title>%s: %s (%s)</title>\n",
            entry.agent, entry.decision, entry.outcome
        ));

        // Add label below point
        if (index % 2 == 0) {
            svg.append(String.format(
                "<text x=\"%d\" y=\"%d\" class=\"timeline-label\">%s</text>\n",
                x, y + 25, entry.agent
            ));
        }

        return svg.toString();
    }

    /**
     * Render statistics dashboard
     */
    private String renderStatsDashboard(DecisionTimeline.TimelineStats stats) {
        StringBuilder html = new StringBuilder();
        html.append("<div class=\"stats-dashboard\">\n");
        html.append(String.format("  <div class=\"stat-item\">\n" +
                "    <h4>Total Decisions</h4>\n" +
                "    <p class=\"stat-value\">%d</p>\n" +
                "  </div>\n", stats.totalEntries));

        html.append(String.format("  <div class=\"stat-item success\">\n" +
                "    <h4>Successful</h4>\n" +
                "    <p class=\"stat-value\">%d</p>\n" +
                "    <p class=\"stat-percentage\">%.1f%%</p>\n" +
                "  </div>\n", stats.successfulCount, stats.successRate));

        html.append(String.format("  <div class=\"stat-item failed\">\n" +
                "    <h4>Failed</h4>\n" +
                "    <p class=\"stat-value\">%d</p>\n" +
                "    <p class=\"stat-percentage\">%.1f%%</p>\n" +
                "  </div>\n", stats.failedCount, stats.failureRate));

        html.append(String.format("  <div class=\"stat-item partial\">\n" +
                "    <h4>Partial</h4>\n" +
                "    <p class=\"stat-value\">%d</p>\n" +
                "  </div>\n", stats.partialCount));

        html.append(String.format("  <div class=\"stat-item\">\n" +
                "    <h4>Avg Confidence</h4>\n" +
                "    <p class=\"stat-value\">%.1f%%</p>\n" +
                "  </div>\n", stats.averageConfidence * 100));

        html.append("</div>\n");
        return html.toString();
    }

    /**
     * Render timeline axis for SVG
     */
    private String getTimelineAxis(int width, int height, int count, int spacing) {
        StringBuilder svg = new StringBuilder();
        int y = height / 2;

        // Horizontal axis line
        svg.append(String.format(
            "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#ccc\" stroke-width=\"2\"/>\n",
            spacing / 2, y, width - spacing / 2, y
        ));

        // Vertical tick marks
        for (int i = 1; i <= count; i++) {
            int x = spacing * i;
            svg.append(String.format(
                "<line x1=\"%d\" y1=\"%d\" x2=\"%d\" y2=\"%d\" stroke=\"#ddd\" stroke-width=\"1\"/>\n",
                x, y - 5, x, y + 5
            ));
        }

        return svg.toString();
    }

    // ==================== HTML/CSS/JS ====================

    /**
     * Get HTML document header with styles
     */
    private String getHTMLHeader() {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Decision Timeline</title>
                <style>
                    * { margin: 0; padding: 0; box-sizing: border-box; }
                    body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; background: #f5f5f5; }
                    
                    .timeline-container {
                        max-width: 1200px;
                        margin: 20px auto;
                        padding: 20px;
                        background: white;
                        border-radius: 8px;
                        box-shadow: 0 2px 8px rgba(0,0,0,0.1);
                    }
                    
                    .timeline-title {
                        font-size: 24px;
                        color: #333;
                        margin-bottom: 30px;
                        border-bottom: 2px solid #3498db;
                        padding-bottom: 10px;
                    }
                    
                    .timeline {
                        position: relative;
                        padding: 20px 0;
                    }
                    
                    .timeline-axis {
                        position: absolute;
                        left: 30px;
                        top: 0;
                        bottom: 0;
                        width: 2px;
                        background: #ecf0f1;
                    }
                    
                    .timeline-entry {
                        margin-bottom: 30px;
                        margin-left: 60px;
                        padding: 15px;
                        border-radius: 6px;
                        border-left: 4px solid #bdc3c7;
                        background: #fafafa;
                        transition: all 0.3s ease;
                    }
                    
                    .timeline-entry:hover {
                        background: white;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.1);
                        transform: translateX(5px);
                    }
                    
                    .timeline-entry.green {
                        border-left-color: #27ae60;
                        background: #f0fdf4;
                    }
                    
                    .timeline-entry.red {
                        border-left-color: #e74c3c;
                        background: #fef2f2;
                    }
                    
                    .timeline-entry.yellow {
                        border-left-color: #f39c12;
                        background: #fffbf0;
                    }
                    
                    .timeline-entry.grey {
                        border-left-color: #95a5a6;
                        background: #f8f9fa;
                    }
                    
                    .timeline-marker {
                        position: absolute;
                        left: -45px;
                        top: 25px;
                        width: 16px;
                        height: 16px;
                        border-radius: 50%;
                        background: white;
                        border: 3px solid #3498db;
                    }
                    
                    .timeline-entry.green .timeline-marker {
                        background: #27ae60;
                        border-color: #27ae60;
                    }
                    
                    .timeline-entry.red .timeline-marker {
                        background: #e74c3c;
                        border-color: #e74c3c;
                    }
                    
                    .timeline-entry.yellow .timeline-marker {
                        background: #f39c12;
                        border-color: #f39c12;
                    }
                    
                    .timeline-agent {
                        font-weight: 600;
                        color: #2c3e50;
                        margin-bottom: 5px;
                    }
                    
                    .timeline-decision {
                        color: #34495e;
                        margin: 5px 0;
                    }
                    
                    .timeline-reasoning {
                        color: #7f8c8d;
                        font-size: 0.9em;
                        font-style: italic;
                        margin: 5px 0;
                    }
                    
                    .timeline-meta {
                        display: flex;
                        gap: 15px;
                        margin-top: 10px;
                        font-size: 0.85em;
                    }
                    
                    .timeline-outcome {
                        padding: 2px 8px;
                        border-radius: 12px;
                        font-weight: 600;
                    }
                    
                    .timeline-outcome.green {
                        background: #d4edda;
                        color: #155724;
                    }
                    
                    .timeline-outcome.red {
                        background: #f8d7da;
                        color: #721c24;
                    }
                    
                    .timeline-outcome.yellow {
                        background: #fff3cd;
                        color: #856404;
                    }
                    
                    .timeline-confidence {
                        color: #3498db;
                        font-weight: 500;
                    }
                    
                    .timeline-time {
                        color: #95a5a6;
                    }
                    
                    .stats-dashboard {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
                        gap: 15px;
                        margin-bottom: 30px;
                    }
                    
                    .stat-item {
                        padding: 15px;
                        background: #ecf0f1;
                        border-radius: 6px;
                        text-align: center;
                    }
                    
                    .stat-item h4 {
                        color: #7f8c8d;
                        font-size: 0.85em;
                        margin-bottom: 8px;
                        text-transform: uppercase;
                    }
                    
                    .stat-value {
                        font-size: 28px;
                        font-weight: bold;
                        color: #2c3e50;
                    }
                    
                    .stat-percentage {
                        color: #3498db;
                        font-size: 0.9em;
                        margin-top: 5px;
                    }
                    
                    .stat-item.success { background: #d4edda; }
                    .stat-item.success h4 { color: #155724; }
                    .stat-item.success .stat-value { color: #155724; }
                    
                    .stat-item.failed { background: #f8d7da; }
                    .stat-item.failed h4 { color: #721c24; }
                    .stat-item.failed .stat-value { color: #721c24; }
                    
                    .stat-item.partial { background: #fff3cd; }
                    .stat-item.partial h4 { color: #856404; }
                    .stat-item.partial .stat-value { color: #856404; }
                    
                    .timeline-empty {
                        padding: 40px;
                        text-align: center;
                        color: #95a5a6;
                        font-size: 16px;
                    }
                    
                    .timeline-svg {
                        width: 100%;
                        height: auto;
                        border: 1px solid #ecf0f1;
                        border-radius: 6px;
                    }
                    
                    .timeline-point {
                        cursor: pointer;
                        transition: r 0.2s;
                    }
                    
                    .timeline-point:hover {
                        r: 12;
                    }
                </style>
            </head>
            <body>
            """;
    }

    /**
     * Get HTML document footer with scripts
     */
    private String getHTMLFooter() {
        return """
            <script>
                // Timeline interactivity
                document.querySelectorAll('[data-decision-id]').forEach(el => {
                    el.addEventListener('click', function() {
                        const decisionId = this.dataset.decisionId;
                        console.log('Drill-down: ' + decisionId);
                    });
                });
                
                // Responsive adjustments
                window.addEventListener('resize', function() {
                    // Recalculate layout if needed
                });
            </script>
            </body>
            </html>
            """;
    }

    // ==================== Utility Methods ====================

    /**
     * Get CSS color class for outcome
     */
    private String getColorClass(DecisionTimeline.OutcomeColor color) {
        return switch (color) {
            case GREEN -> "green";
            case RED -> "red";
            case YELLOW -> "yellow";
            case GREY -> "grey";
        };
    }

    /**
     * Get hex color for SVG rendering
     */
    private String getNumberColor(DecisionTimeline.OutcomeColor color) {
        return switch (color) {
            case GREEN -> "#27ae60";
            case RED -> "#e74c3c";
            case YELLOW -> "#f39c12";
            case GREY -> "#95a5a6";
        };
    }

    /**
     * Format time ago string
     */
    private String formatTimeAgo(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "d";
        if (hours > 0) return hours + "h";
        if (minutes > 0) return minutes + "m";
        return seconds + "s";
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHTML(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }
}
