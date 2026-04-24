// k6 Load Testing Script for SupremeAI
// Tests 500 concurrent users with < 2 second response time requirement
// Run: k6 run --vus 500 --duration 5m load-test.js

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const responseTimeTrend = new Trend('response_time');
const loginCounter = new Counter('login_requests');
const aiRequestCounter = new Counter('ai_requests');

// Test configuration
export const options = {
  stages: [
    { duration: '30s', target: 50 },   // Ramp up to 50 users
    { duration: '1m', target: 200 },    // Ramp to 200 users
    { duration: '1m', target: 500 },    // Ramp to 500 users
    { duration: '2m', target: 500 },    // Stay at 500 users
    { duration: '30s', target: 0 },     // Ramp down
  ],
  thresholds: {
    'http_req_duration': ['p(95)<2000', 'p(99)<2000'], // 95% and 99% under 2s
    'errors': ['rate<0.01'], // Error rate under 1%
  },
  ext: {
    loadimpact: {
      projectID: undefined,
      name: 'SupremeAI Load Test - 500 Users',
    },
  },
};

// Base URL - change to your environment
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Test data
const testUsers = [
  { username: 'user1', password: 'pass123' },
  { username: 'user2', password: 'pass123' },
  { username: 'user3', password: 'pass123' },
];

const aiPrompts = [
  'Create a simple React component',
  'Generate a REST API endpoint',
  'Write a Python function to sort array',
  'Build a login form',
  'Create a database schema',
];

export function setup() {
  console.log('Starting load test for SupremeAI...');
  console.log(`Target: ${BASE_URL}`);
  console.log('Goal: 500 concurrent users, < 2s response time');
  return { startTime: new Date().toISOString() };
}

export default function (data) {
  const user = testUsers[Math.floor(Math.random() * testUsers.length)];
  const prompt = aiPrompts[Math.floor(Math.random() * aiPrompts.length)];
  const headers = { 'Content-Type': 'application/json' };

  // Group 1: Health Check
  group('Health Check', function () {
    const res = http.get(`${BASE_URL}/actuator/health`);
    check(res, {
      'health status is 200': (r) => r.status === 200,
      'health response time < 500ms': (r) => r.timings.duration < 500,
    });
    responseTimeTrend.add(res.timings.duration);
  });

  sleep(1);

  // Group 2: User Login
  group('User Login', function () {
    loginCounter.add(1);
    const loginPayload = JSON.stringify({
      username: user.username,
      password: user.password,
    });

    const res = http.post(`${BASE_URL}/api/auth/login`, loginPayload, { headers });
    
    check(res, {
      'login status is 200 or 401': (r) => r.status === 200 || r.status === 401,
      'login response time < 2000ms': (r) => r.timings.duration < 2000,
    });

    responseTimeTrend.add(res.timings.duration);
    errorRate.add(res.status !== 200 && res.status !== 401);
  });

  sleep(1);

  // Group 3: AI Request (Core functionality)
  group('AI Request', function () {
    aiRequestCounter.add(1);
    const aiPayload = JSON.stringify({
      prompt: prompt,
      model: 'auto',
      maxTokens: 1500,
    });

    const startTime = new Date().getTime();
    const res = http.post(`${BASE_URL}/api/ai/generate`, aiPayload, { headers });
    const duration = new Date().getTime() - startTime;

    check(res, {
      'AI request status is 200': (r) => r.status === 200,
      'AI response time < 2000ms': (r) => r.timings.duration < 2000,
      'AI response has content': (r) => r.body && r.body.length > 0,
    });

    responseTimeTrend.add(res.timings.duration);
    errorRate.add(res.status !== 200);

    if (res.timings.duration > 2000) {
      console.warn(`Slow request: ${duration}ms for prompt: ${prompt.substring(0, 50)}...`);
    }
  });

  sleep(2);

  // Group 4: Cache Test (Check Redis caching)
  group('Cache Test', function () {
    const res = http.get(`${BASE_URL}/api/ai/models`);
    check(res, {
      'cache status is 200': (r) => r.status === 200,
      'cache response time < 1000ms': (r) => r.timings.duration < 1000,
    });
    responseTimeTrend.add(res.timings.duration);
  });

  sleep(Math.random() * 3 + 1); // Random sleep 1-4 seconds
}

export function teardown(data) {
  console.log('Load test completed!');
  console.log(`Started at: ${data.startTime}`);
  console.log(`Ended at: ${new Date().toISOString()}`);
}

// Helper function to generate random string
function randomString(length) {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
  let result = '';
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length));
  }
  return result;
}
