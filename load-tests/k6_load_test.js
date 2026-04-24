import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
  stages: [
    { duration: '30s', target: 100 }, // ramp up to 100 users
    { duration: '1m', target: 500 },  // stay at 500 users
    { duration: '30s', target: 0 },   // ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests must complete below 2s
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export default function () {
  let res = http.get(`${BASE_URL}/api/system/metrics`);
  check(res, {
    'status is 200': (r) => r.status === 200,
    'response time < 2000ms': (r) => r.timings.duration < 2000,
  });

  // Simulate AI prompt
  let payload = JSON.stringify({
    prompt: 'Build a simple React component for a weather app',
    models: ['groq', 'openai']
  });
  
  let params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  let aiRes = http.post(`${BASE_URL}/api/ai/generate`, payload, params);
  check(aiRes, {
    'ai generate status is 200 or 202': (r) => r.status === 200 || r.status === 202,
  });

  sleep(1);
}
