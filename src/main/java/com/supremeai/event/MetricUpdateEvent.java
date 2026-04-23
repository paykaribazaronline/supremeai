package com.supremeai.event;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MetricUpdateEvent {
    private String metricName;
    private double value;
    private double threshold;
}
