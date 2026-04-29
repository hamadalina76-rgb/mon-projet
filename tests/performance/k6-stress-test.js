/**
 * SpeedLine - k6 Stress Test
 *
 * Pushes the system beyond normal load to find breaking points.
 *
 * Run:
 *   k6 run tests/performance/k6-stress-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const errorRate = new Rate('errors');
const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export const options = {
  stages: [
    { duration: '1m', target: 50 },    // Normal load
    { duration: '2m', target: 100 },   // High load
    { duration: '2m', target: 200 },   // Stress load
    { duration: '1m', target: 300 },   // Breaking point
    { duration: '2m', target: 300 },   // Sustained breaking point
    { duration: '1m', target: 0 },     // Recovery
  ],
  thresholds: {
    http_req_duration: ['p(99)<5000'],  // 99th percentile < 5s (lenient for stress)
    errors: ['rate<0.3'],               // Allow up to 30% errors under extreme stress
  },
};

export default function () {
  // Login
  const loginRes = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({
      email: 'admin@speedline-test.com',
      password: 'TestAdmin123!',
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const loginOk = check(loginRes, {
    'login ok': (r) => r.status === 200,
  });

  if (!loginOk) {
    errorRate.add(1);
    sleep(1);
    return;
  }

  errorRate.add(0);

  let token;
  try {
    const body = JSON.parse(loginRes.body);
    token = body.accessToken || body.access_token || body.token;
  } catch {
    errorRate.add(1);
    sleep(1);
    return;
  }

  const headers = {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };

  // Hit multiple endpoints concurrently
  const responses = http.batch([
    ['GET', `${BASE_URL}/api/orders?page=0&size=10`, null, headers],
    ['GET', `${BASE_URL}/api/partners?page=0&size=10`, null, headers],
    ['GET', `${BASE_URL}/api/users/customers?page=0&size=10`, null, headers],
  ]);

  for (const res of responses) {
    const ok = check(res, {
      'batch response ok': (r) => r.status >= 200 && r.status < 500,
    });
    if (!ok) errorRate.add(1);
    else errorRate.add(0);
  }

  sleep(0.5);
}
