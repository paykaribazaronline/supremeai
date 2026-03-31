# Advanced Monitoring System - Complete Guide

## Overview

The Advanced Monitoring System provides comprehensive metrics collection, dashboard visualization, alert management, and performance tracing capabilities for the SupremeAI platform.

## Architecture Components

### 1. MetricsCollectorService
**Purpose**: Real-time collection and aggregation of application metrics

**Key Features**:
- Record metrics with custom tags and metadata
- Calculate statistics (min, max, mean, median, sum, count)
- Time-windowed metric queries
- Automatic retention management (1-hour default)
- Thread-safe concurrent operations

**Methods**:
```java
// Record a metric
void recordMetric(String name, double value, Map<String, String> tags)
void recordMetric(String name, double value)

// Retrieve metrics
MetricData getMetric(String name)
Collection<MetricData> getAllMetrics()

// Query metrics
Map<String, Object> getMetricStats(String name)
Collection<MetricData> getMetricsInRange(long startTime, long endTime)

// Maintenance
int clearOldMetrics()
```

**Statistics Response**:
```json
{
  "count": 100,
  "sum": 5000.0,
  "mean": 50.0,
  "median": 50.0,
  "min": 10.0,
  "max": 100.0
}
```

### 2. DashboardService
**Purpose**: Dashboard creation and visualization management

**Key Features**:
- Create and manage multiple dashboards
- Add metric widgets (line, bar, gauge, heatmap)
- Generate real-time dashboard reports
- Widget types support different visualizations
- Integration with MetricsCollectorService

**Methods**:
```java
// Dashboard CRUD
Dashboard createDashboard(String name, String description)
Dashboard getDashboard(String dashboardId)
Collection<Dashboard> listDashboards()
void deleteDashboard(String dashboardId)

// Widget management
void addWidget(String dashboardId, String metricName, String widgetType)
void removeWidget(String dashboardId, String widgetId)

// Reporting
Map<String, Object> generateDashboardReport(String dashboardId)
```

**Widget Types**:
- `line` - Time-series line chart
- `bar` - Bar chart for discrete values
- `gauge` - Gauge/dial for current values
- `heatmap` - Heatmap for two-dimensional data

**Dashboard Report**:
```json
{
  "dashboardId": "uuid",
  "name": "System Metrics",
  "description": "Main system overview",
  "widgets": [
    {
      "id": "widget-id",
      "metricName": "cpu_usage",
      "type": "line",
      "stats": {...}
    }
  ],
  "createdAt": 1711900000000,
  "updatedAt": 1711900000000
}
```

### 3. AlertManagementService
**Purpose**: Alert rule definition and triggering

**Key Features**:
- Create and manage alert rules
- 6 comparison operators: GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, EQUAL, NOT_EQUAL
- 3 severity levels: CRITICAL, WARNING, INFO
- Real-time alert evaluation
- Alert acknowledgment and resolution tracking

**Methods**:
```java
// Rule management
AlertRule createAlertRule(String name, String metricName, String condition, 
                         double threshold, String severity)
AlertRule getAlertRule(String ruleId)
Collection<AlertRule> listAlertRules()
void deleteAlertRule(String ruleId)

// Alert evaluation and tracking
List<AlertInstance> evaluateAlerts()
AlertInstance getActiveAlert(String alertId)
Collection<AlertInstance> listActiveAlerts()

// Alert lifecycle
void acknowledgeAlert(String alertId)
void resolveAlert(String alertId)
```

**Alert Rule Schema**:
```json
{
  "id": "rule-uuid",
  "name": "High CPU Usage",
  "metricName": "cpu_usage",
  "condition": "GREATER_THAN",
  "threshold": 80.0,
  "severity": "CRITICAL",
  "enabled": true,
  "createdAt": 1711900000000
}
```

**Alert Instance Schema**:
```json
{
  "id": "alert-uuid",
  "ruleId": "rule-uuid",
  "name": "High CPU Usage",
  "severity": "CRITICAL",
  "metricValue": 85.5,
  "threshold": 80.0,
  "metricName": "cpu_usage",
  "triggeredAt": 1711900000000,
  "acknowledgedAt": null,
  "acknowledged": false
}
```

### 4. PerformanceMonitoringService
**Purpose**: Distributed tracing and performance profiling

**Key Features**:
- Distributed trace span creation and tracking
- Method-level performance metrics
- Percentile calculations (P99)
- Automatic performance aggregation
- Thread-safe span management

**Methods**:
```java
// Span management
TraceSpan startSpan(String traceId, String spanName, String parentSpanId)
void endSpan(String spanId)
TraceSpan getSpan(String spanId)
List<TraceSpan> getTraceSpans(String traceId)

// Performance metrics
PerformanceMetrics getMethodMetrics(String methodName)
Collection<PerformanceMetrics> getAllMethodMetrics()
Map<String, Object> getPerformanceReport()
```

**Trace Span Schema**:
```json
{
  "spanId": "span-uuid",
  "traceId": "trace-uuid",
  "spanName": "GET /api/users",
  "parentSpanId": "parent-span-uuid",
  "startTime": 1711900000000,
  "endTime": 1711900000050,
  "duration": 50,
  "tags": {
    "endpoint": "/api/users",
    "method": "GET"
  }
}
```

**Performance Metrics Schema**:
```json
{
  "methodName": "userService.getUser",
  "callCount": 1000,
  "totalDuration": 50000,
  "avgDuration": 50.0,
  "minDuration": 10,
  "maxDuration": 200,
  "medianDuration": 45.0,
  "p99Duration": 180.0
}
```

## REST API Endpoints

### Metrics Endpoints

#### Record Metric
```
POST /api/monitoring/metrics
?name=cpu_usage&value=45.5&tags[host]=server1

Response:
{
  "status": "success",
  "metric": "cpu_usage"
}
```

#### Get Metric
```
GET /api/monitoring/metrics/{name}

Response:
{
  "name": "cpu_usage",
  "dataPoints": 100,
  "statistics": {...}
}
```

#### List All Metrics
```
GET /api/monitoring/metrics

Response:
{
  "metrics": [...],
  "count": 42
}
```

#### Get Metric Statistics
```
GET /api/monitoring/metrics/{name}/stats

Response:
{
  "count": 100,
  "sum": 4500.0,
  "mean": 45.0,
  "median": 45.0,
  "min": 10.0,
  "max": 80.0
}
```

#### Clear Old Metrics
```
POST /api/monitoring/metrics/clear

Response:
{
  "status": "success",
  "clearedCount": 45
}
```

### Dashboard Endpoints

#### Create Dashboard
```
POST /api/monitoring/dashboards
?name=System%20Metrics&description=Main%20system%20overview

Response: Dashboard object
```

#### Get Dashboard
```
GET /api/monitoring/dashboards/{id}

Response: Dashboard object
```

#### List Dashboards
```
GET /api/monitoring/dashboards

Response:
{
  "dashboards": [...],
  "count": 5
}
```

#### Add Widget
```
POST /api/monitoring/dashboards/{id}/widgets
?metricName=cpu_usage&widgetType=line

Response:
{
  "status": "success",
  "dashboardId": "...",
  "metricName": "cpu_usage",
  "widgetType": "line"
}
```

#### Generate Report
```
GET /api/monitoring/dashboards/{id}/report

Response: Dashboard report with metrics data
```

#### Delete Dashboard
```
DELETE /api/monitoring/dashboards/{id}

Response:
{
  "status": "success",
  "dashboardId": "..."
}
```

### Alert Endpoints

#### Create Alert Rule
```
POST /api/monitoring/alerts/rules
?name=High%20CPU&metricName=cpu_usage&condition=GREATER_THAN&threshold=80&severity=CRITICAL

Response: AlertRule object
```

#### List Alert Rules
```
GET /api/monitoring/alerts/rules

Response:
{
  "rules": [...],
  "count": 10
}
```

#### Evaluate Alerts
```
POST /api/monitoring/alerts/evaluate

Response:
{
  "status": "success",
  "triggeredAlerts": [...],
  "count": 2
}
```

#### Get Active Alerts
```
GET /api/monitoring/alerts/active

Response:
{
  "alerts": [...],
  "count": 5
}
```

#### Acknowledge Alert
```
POST /api/monitoring/alerts/{id}/acknowledge

Response:
{
  "status": "success",
  "alertId": "..."
}
```

#### Resolve Alert
```
POST /api/monitoring/alerts/{id}/resolve

Response:
{
  "status": "success",
  "alertId": "..."
}
```

### Performance Monitoring Endpoints

#### Start Trace Span
```
POST /api/monitoring/performance/spans
?traceId=trace-123&spanName=GET%20/users&parentSpanId=

Response:
{
  "spanId": "...",
  "traceId": "trace-123",
  "spanName": "GET /users"
}
```

#### End Trace Span
```
POST /api/monitoring/performance/spans/{spanId}/end

Response:
{
  "status": "success",
  "spanId": "..."
}
```

#### Get Trace Spans
```
GET /api/monitoring/performance/traces/{traceId}

Response:
{
  "traceId": "trace-123",
  "spans": [...],
  "count": 5
}
```

#### Get Method Metrics
```
GET /api/monitoring/performance/methods/{methodName}

Response:
{
  "methodName": "userService.getUser",
  "callCount": 1000,
  "totalDuration": 50000,
  "avgDuration": 50.0,
  "minDuration": 10,
  "maxDuration": 200,
  "medianDuration": 45.0,
  "p99Duration": 180.0
}
```

#### Get Performance Report
```
GET /api/monitoring/performance/report

Response:
{
  "methods": [...],
  "generatedAt": 1711900000000
}
```

## Usage Examples

### Example 1: Record and Monitor CPU Usage

```java
// Record CPU metrics
metricsCollector.recordMetric("cpu_usage", 45.5, Map.of("host", "server-1"));
metricsCollector.recordMetric("cpu_usage", 48.2, Map.of("host", "server-1"));

// Get statistics
Map<String, Object> stats = metricsCollector.getMetricStats("cpu_usage");
// Result: {count: 2, sum: 93.7, mean: 46.85, min: 45.5, max: 48.2, ...}

// Create alert rule
AlertManagementService.AlertRule rule = alertService.createAlertRule(
    "High CPU Usage",
    "cpu_usage",
    "GREATER_THAN",
    80.0,
    "CRITICAL"
);

// Evaluate alerts
List<AlertManagementService.AlertInstance> triggered = alertService.evaluateAlerts();
```

### Example 2: Create Dashboard with Multiple Widgets

```java
// Create dashboard
DashboardService.Dashboard dashboard = dashboardService.createDashboard(
    "System Health",
    "Real-time system metrics"
);

// Add widgets
dashboardService.addWidget(dashboard.id, "cpu_usage", "line");
dashboardService.addWidget(dashboard.id, "memory_usage", "gauge");
dashboardService.addWidget(dashboard.id, "disk_usage", "bar");

// Generate report
Map<String, Object> report = dashboardService.generateDashboardReport(dashboard.id);
```

### Example 3: Distributed Tracing

```java
// Start root span
PerformanceMonitoringService.TraceSpan rootSpan = performanceMonitoring.startSpan(
    "trace-12345",
    "POST /api/users",
    null
);

// Start child span
PerformanceMonitoringService.TraceSpan childSpan = performanceMonitoring.startSpan(
    "trace-12345",
    "database.query",
    rootSpan.spanId
);

// Add tags
childSpan.addTag("query", "SELECT * FROM users");

// End spans
performanceMonitoring.endSpan(childSpan.spanId);
performanceMonitoring.endSpan(rootSpan.spanId);

// Get metrics
PerformanceMonitoringService.PerformanceMetrics metrics = 
    performanceMonitoring.getMethodMetrics("database.query");
```

## Alert Conditions Reference

| Condition | Operator | Example |
|-----------|----------|---------|
| GREATER_THAN | > | value > threshold |
| GREATER_THAN_OR_EQUAL | >= | value >= threshold |
| LESS_THAN | < | value < threshold |
| LESS_THAN_OR_EQUAL | <= | value <= threshold |
| EQUAL | == | value == threshold |
| NOT_EQUAL | != | value != threshold |

## Severity Levels

| Level | Use Case |
|-------|----------|
| CRITICAL | System down, data loss risk, immediate action required |
| WARNING | Performance degradation, resource constraints |
| INFO | Informational, trending data, non-critical |

## Best Practices

### Metrics Collection
1. **Use meaningful names**: Use descriptive names like `cpu_usage`, `request_latency_ms`
2. **Add context tags**: Include host, service, region for better filtering
3. **Regular cleanup**: Periodically call `clearOldMetrics()` to manage storage
4. **Monitor cardinality**: Avoid unbounded tag values

### Alerting
1. **Set appropriate thresholds**: Base on historical data and SLOs
2. **Avoid alert fatigue**: Set reasonable threshold values
3. **Acknowledge alerts**: Mark as acknowledged when investigating
4. **Resolve alerts**: Close alerts when issue is fixed

### Dashboards
1. **Organize by domain**: Create separate dashboards for different services
2. **Mix widget types**: Use appropriate visualization for data type
3. **Regular updates**: Add new metrics as services evolve
4. **Share reports**: Generate reports for stakeholders

### Performance Monitoring
1. **Instrument key paths**: Trace critical request flows
2. **Monitor P99 latency**: Focus on tail latency for user experience
3. **Regular review**: Analyze performance trends over time
4. **Set baselines**: Establish normal performance ranges

## Retention Policy

- **Default retention**: 1 hour
- **Configurable**: Modify `retentionPeriodMs` in MetricsCollectorService
- **Automatic cleanup**: Call `clearOldMetrics()` regularly
- **Alert independence**: Alerts are not subject to retention policy

## Thread Safety

All monitoring services are thread-safe:
- ConcurrentHashMap for storage
- Synchronized lists for data points
- Atomic operations for counters
- Suitable for multi-threaded environments

## Performance Characteristics

- **Metric recording**: O(1) insertion, thread-safe
- **Statistics calculation**: O(n log n) due to sorting
- **Alert evaluation**: O(m) where m = number of active rules
- **Dashboard generation**: O(w) where w = number of widgets

## Integration with Spring Boot

```yaml
# application.properties
monitoring.metrics.retention-ms=3600000
monitoring.alerts.enabled=true
monitoring.dashboard.auto-refresh=true
```

## Troubleshooting

### No metrics appearing
- Verify metrics are being recorded with correct names
- Check retention policy isn't clearing too aggressively
- Ensure MetricsCollectorService is properly injected

### Alerts not triggering
- Verify metric name matches exactly (case-sensitive)
- Check threshold value is set correctly
- Ensure alert rules are enabled
- Call `evaluateAlerts()` regularly

### Dashboard reports empty
- Add widgets to dashboard
- Ensure metrics are being recorded
- Verify metric names match
- Check dashboard hasn't been deleted

## Testing

All monitoring services include comprehensive test suites:
- **MetricsCollectorServiceTest**: 20 tests covering collection and statistics
- **DashboardServiceTest**: 18 tests for dashboard operations
- **AlertManagementServiceTest**: 25 tests for rule evaluation
- **PerformanceMonitoringServiceTest**: 22 tests for tracing

Run tests with:
```bash
./gradlew test --tests "*MonitoringTest"
```

## Version History

- v1.0.0 (April 2026): Initial release
  - Metrics collection and aggregation
  - Dashboard visualization
  - Alert rule management
  - Distributed tracing support
