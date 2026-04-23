import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '2m', target: 100 },   // Ramp up
        { duration: '5m', target: 500 },   // Sustained load
        { duration: '2m', target: 1000 },  // Peak load
        { duration: '2m', target: 0 },     // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95% under 2s
        http_req_failed: ['rate<0.01'],    // <1% errors
    },
};

const BASE_URL = __ENV.BASE_URL || 'https://supremeai-lhlwyikwlq-uc.a.run.app';

export default function () {
    const res = http.get(`${BASE_URL}/api/agents/status`);
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response time < 2s': (r) => r.timings.duration < 2000,
    });
    sleep(1);
}
