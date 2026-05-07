package com.supremeai.event;

public class MetricUpdateEvent {
    private String metricName;
    private double value;
    private double threshold;

    public MetricUpdateEvent(String metricName, double value, double threshold) {
        this.metricName = metricName;
        this.value = value;
        this.threshold = threshold;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
