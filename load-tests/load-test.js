import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 100 },   // Ramp up to 100 users
    { duration: '1m', target: 100 },    // Stay at 100 users
    { duration: '30s', target: 0 },     // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95% of requests under 500ms
    'http_req_failed': ['rate<0.1'],    // Error rate under 10%
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // Test health endpoint
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, { 'health status 200': (r) => r.status === 200 });

  // Test metrics endpoint
  const metricsRes = http.get(`${BASE_URL}/actuator/metrics/http.server.requests`);
  check(metricsRes, { 'metrics status 200': (r) => r.status === 200 });

  // Test performance dashboard
  const dashboardRes = http.get(`${BASE_URL}/performance-dashboard.html`);
  check(dashboardRes, { 'dashboard status 200': (r) => r.status === 200 });

  sleep(1);
}
