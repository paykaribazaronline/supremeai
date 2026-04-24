// k6 Load Test Script for SupremeAI
// Run: k6 run load-test.js
// Or with params: k6 run --vus 50 --duration 30s load-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('error_rate');
const responseTimeTrend = new Trend('response_time_trend');
const memoryUsageCounter = new Counter('memory_usage_check');
const latencyTrend = new Trend('latency_trend');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Warm up
    { duration: '1m', target: 50 },    // Ramp to 50 users
    { duration: '2m', target: 50 },    // Stay at 50 users
    { duration: '1m', target: 100 },   // Ramp to 100 users
    { duration: '2m', target: 100 },   // Stay at 100 users
    { duration: '30s', target: 0 },    // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2000'],  // 95% of requests under 2s
    'error_rate': ['rate<0.05'],          // Error rate under 5%
    'response_time_trend': ['p(90)<1500'], // 90% under 1.5s
  },
};

// Base URL
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test data
const providers = ['groq', 'openai', 'anthropic', 'ollama'];
const authToken = __ENV.AUTH_TOKEN || 'dev-admin-token-local';

export function setup() {
  console.log('Starting SupremeAI Load Test');
  console.log('Target URL: ' + BASE_URL);
  console.log('Test duration: ~7 minutes');
  return { startTime: new Date() };
}

export default function (data) {
  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${authToken}`,
  };

  group('Health Check & Metrics', function () {
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    check(healthRes, {
      'health check status 200': (r) => r.status === 200,
      'health check response time < 500ms': (r) => r.timings.duration < 500,
    }) || errorRate.add(1);
    
    // Check memory metrics if available
    const metricsRes = http.get(`${BASE_URL}/actuator/metrics/jvm.memory.used`);
    if (metricsRes.status === 200) {
      try {
        const metrics = JSON.parse(metricsRes.body);
        memoryUsageCounter.add(metrics.measurements?.value || 0);
      } catch (e) {
        // Ignore parse errors
      }
    }
  });

  group('Authentication', function () {
    const payload = JSON.stringify({
      idToken: 'test-firebase-token-' + Math.random()
    });
    
    const authRes = http.post(`${BASE_URL}/api/auth/firebase-login`, payload, { headers });
    check(authRes, {
      'auth endpoint responds': (r) => r.status === 200 || r.status === 401,
      'auth response time < 1s': (r) => r.timings.duration < 1000,
    }) || errorRate.add(1);
    
    responseTimeTrend.add(authRes.timings.duration);
  });

  group('AI Orchestration', function () {
    const provider = providers[Math.floor(Math.random() * providers.length)];
    const prompts = [
      'Create a simple TODO app with React',
      'Build a REST API for user management',
      'Generate a login page with Firebase Auth',
      'Make a dashboard with charts and graphs',
      'Create a chat application with WebSocket support',
    ];
    const prompt = prompts[Math.floor(Math.random() * prompts.length)];
    
    const payload = JSON.stringify({
      prompt: prompt,
      provider: provider,
      context: 'load-test-generation'
    });
    
    const orchRes = http.post(`${BASE_URL}/api/orchestrate`, payload, { headers });
    check(orchRes, {
      'orchestration responds': (r) => r.status !== 0,
      'orchestration time < 10s': (r) => r.timings.duration < 10000,
    }) || errorRate.add(1);
    
    latencyTrend.add(orchRes.timings.duration);
  });

  group('Performance Dashboard', function () {
    const perfRes = http.get(`${BASE_URL}/performance-dashboard.html`);
    check(perfRes, {
      'dashboard loads': (r) => r.status === 200,
      'dashboard size reasonable': (r) => r.body.length > 1000,
      'dashboard load < 500ms': (r) => r.timings.duration < 500,
    }) || errorRate.add(1);
  });

  group('API Key Management', function () {
    const keysRes = http.get(`${BASE_URL}/api/admin/api-keys`, { headers });
    check(keysRes, {
      'api keys endpoint accessible': (r) => r.status === 200 || r.status === 403,
      'api keys response < 500ms': (r) => r.timings.duration < 500,
    });
  });

  group('Admin Dashboard', function () {
    const adminRes = http.get(`${BASE_URL}/admin.html`);
    check(adminRes, {
      'admin dashboard loads': (r) => r.status === 200,
      'admin dashboard has content': (r) => r.body.includes('SupremeAI'),
    }) || errorRate.add(1);
  });

  // Think time between requests
  sleep(Math.random() * 3 + 1);  // 1-4 seconds
}

export function teardown(data) {
  console.log('Load Test Completed');
  console.log('Start Time: ' + data.startTime);
  console.log('End Time: ' + new Date());
  
  // Final memory check
  const finalMetrics = http.get(`${BASE_URL}/actuator/metrics/jvm.memory.used}`);
  if (finalMetrics.status === 200) {
    console.log('Final Memory Check: ' + finalMetrics.body);
  }
}

// Handle summary
export function handleSummary(data) {
  return {
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
    'load-test-results.json': JSON.stringify(data),
    'load-test-metrics.csv': generateCSV(data),
  };
}

function generateCSV(data) {
  let csv = 'metric,value\n';
  csv += `total_requests,${data.metrics.http_reqs.values.count}\n`;
  csv += `failed_requests,${data.metrics.http_req_failed.values.count}\n`;
  csv += `p95_response_time,${data.metrics.http_req_duration.values['p(95)']}\n`;
  csv += `avg_response_time,${data.metrics.http_req_duration.values.avg}\n`;
  csv += `error_rate,${data.metrics.error_rate.values.rate}\n`;
  return csv;
}

// textSummary helper (simplified)
function textSummary(data, options) {
  const indent = options?.indent || ' ';
  let summary = '\n=== SupremeAI Load Test Results ===\n\n';
  summary += `${indent}Total Requests: ${data.metrics.http_reqs.values.count}\n`;
  summary += `${indent}Failed Requests: ${data.metrics.http_req_failed.values.count}\n`;
  summary += `${indent}Error Rate: ${(data.metrics.error_rate.values.rate * 100).toFixed(2)}%\n\n`;
  summary += `${indent}Response Time (ms):\n`;
  summary += `${indent}${indent}Avg: ${data.metrics.http_req_duration.values.avg.toFixed(2)}\n`;
  summary += `${indent}${indent}Min: ${data.metrics.http_req_duration.values.min.toFixed(2)}\n`;
  summary += `${indent}${indent}Med: ${data.metrics.http_req_duration.values.med.toFixed(2)}\n`;
  summary += `${indent}${indent}Max: ${data.metrics.http_req_duration.values.max.toFixed(2)}\n`;
  summary += `${indent}${indent}P95: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}\n`;
  summary += `${indent}${indent}P99: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}\n\n`;
  summary += `${indent}Virtual Users: ${data.metrics.vus?.values?.value || 'N/A'}\n`;
  return summary;
}
