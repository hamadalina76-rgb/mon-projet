/**
 * SpeedLine - k6 Spike Test
 *
 * Simulates a sudden traffic spike (e.g., flash sale, peak lunch hour).
 *
 * Run:
 *   k6 run tests/performance/k6-spike-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');
const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '10s', target: 5 },     // Baseline
    { duration: '10s', target: 200 },    // Sudden spike!
    { duration: '30s', target: 200 },    // Sustained spike
    { duration: '10s', target: 5 },      // Spike ends
    { duration: '1m', target: 5 },       // Recovery period
    { duration: '10s', target: 0 },      // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<5000'],
    errors: ['rate<0.2'],
  },
};

export default function () {
  // Simulate customer ordering flow
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({
      email: 'customer@speedline-test.com',
      password: 'TestCustomer123!',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const ok = check(loginRes, { 'login ok': (r) => r.status === 200 });
  if (!ok) {
    errorRate.add(1);
    sleep(0.5);
    return;
  }
  errorRate.add(0);

  let token;
  try {
    const body = JSON.parse(loginRes.body);
    token = body.accessToken || body.access_token || body.token;
  } catch {
    return;
  }

  const headers = {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  // Browse partners (simulates opening the app during peak)
  const partnersRes = http.get(`${BASE_URL}/api/partners?page=0&size=20`, headers);
  check(partnersRes, { 'partners ok': (r) => r.status === 200 });

  sleep(0.3);
}
