/**
 * SpeedLine - k6 Load & Performance Tests
 *
 * Tests the API gateway under load for key user flows.
 *
 * Install k6:
 *   brew install k6          (macOS)
 *   sudo apt install k6      (Linux)
 *   choco install k6         (Windows)
 *
 * Run:
 *   k6 run tests/performance/k6-load-test.js
 *   k6 run --vus 50 --duration 2m tests/performance/k6-load-test.js
 *   k6 run --out json=results.json tests/performance/k6-load-test.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const loginDuration = new Trend('login_duration');
const orderListDuration = new Trend('order_list_duration');
const partnerListDuration = new Trend('partner_list_duration');
const errorRate = new Rate('errors');

// Configuration
const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'admin@speedline-test.com';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'TestAdmin123!';
const CUSTOMER_EMAIL = __ENV.CUSTOMER_EMAIL || 'customer@speedline-test.com';
const CUSTOMER_PASSWORD = __ENV.CUSTOMER_PASSWORD || 'TestCustomer123!';

// Load test stages
export const options = {
  stages: [
    { duration: '30s', target: 10 },   // Ramp up to 10 users
    { duration: '1m', target: 25 },     // Ramp up to 25 users
    { duration: '2m', target: 25 },     // Stay at 25 users
    { duration: '1m', target: 50 },     // Peak: 50 concurrent users
    { duration: '1m', target: 50 },     // Stay at peak
    { duration: '30s', target: 0 },     // Ramp down
  ],
  thresholds: {
    http_req_duration: ['p(95)<3000'],  // 95% of requests < 3s
    http_req_failed: ['rate<0.05'],     // Error rate < 5%
    login_duration: ['p(95)<2000'],     // Login < 2s at p95
    order_list_duration: ['p(95)<2000'],
    partner_list_duration: ['p(95)<2000'],
    errors: ['rate<0.1'],
  },
};

function login(email, password) {
  const res = http.post(
    `${BASE_URL}/api/auth/login`,
    JSON.stringify({ email, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  loginDuration.add(res.timings.duration);

  const success = check(res, {
    'login status is 200': (r) => r.status === 200,
    'login has token': (r) => {
      try {
        const body = JSON.parse(r.body);
        return !!(body.accessToken || body.access_token || body.token);
      } catch {
        return false;
      }
    },
  });

  if (!success) {
    errorRate.add(1);
    return null;
  }

  errorRate.add(0);
  const body = JSON.parse(res.body);
  return body.accessToken || body.access_token || body.token;
}

function authHeaders(token) {
  return {
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  };
}

export default function () {
  // Randomly pick a user flow
  const flow = Math.random();

  if (flow < 0.4) {
    // 40% - Admin flow
    adminFlow();
  } else if (flow < 0.7) {
    // 30% - Customer browsing
    customerBrowseFlow();
  } else {
    // 30% - Partner dashboard
    partnerFlow();
  }

  sleep(1);
}

function adminFlow() {
  group('Admin Flow', () => {
    const token = login(ADMIN_EMAIL, ADMIN_PASSWORD);
    if (!token) return;

    // List orders
    group('List Orders', () => {
      const res = http.get(`${BASE_URL}/api/orders?page=0&size=20`, authHeaders(token));
      orderListDuration.add(res.timings.duration);
      check(res, { 'orders status 200': (r) => r.status === 200 });
    });

    // List partners
    group('List Partners', () => {
      const res = http.get(`${BASE_URL}/api/partners?page=0&size=20`, authHeaders(token));
      partnerListDuration.add(res.timings.duration);
      check(res, { 'partners status 200': (r) => r.status === 200 });
    });

    // List users
    group('List Users', () => {
      const res = http.get(`${BASE_URL}/api/users/customers?page=0&size=20`, authHeaders(token));
      check(res, { 'users status 200': (r) => r.status === 200 });
    });

    // List deliveries
    group('List Deliveries', () => {
      const res = http.get(`${BASE_URL}/api/deliveries`, authHeaders(token));
      check(res, { 'deliveries status 2xx': (r) => r.status >= 200 && r.status < 300 });
    });

    sleep(0.5);
  });
}

function customerBrowseFlow() {
  group('Customer Browse Flow', () => {
    const token = login(CUSTOMER_EMAIL, CUSTOMER_PASSWORD);
    if (!token) return;

    // Browse nearby partners
    group('Browse Partners', () => {
      const res = http.get(
        `${BASE_URL}/api/partners/nearby?lat=36.4513&lng=10.7357&radius=10`,
        authHeaders(token)
      );
      check(res, { 'nearby partners 2xx': (r) => r.status >= 200 && r.status < 400 });
    });

    // Get partner menu
    group('View Menu', () => {
      const partnersRes = http.get(`${BASE_URL}/api/partners?page=0&size=5`, authHeaders(token));
      if (partnersRes.status === 200) {
        try {
          const body = JSON.parse(partnersRes.body);
          const partners = body.content || body;
          if (Array.isArray(partners) && partners.length > 0) {
            const partnerId = partners[0].id;
            const menuRes = http.get(`${BASE_URL}/api/partners/${partnerId}/menu`, authHeaders(token));
            check(menuRes, { 'menu status 2xx': (r) => r.status >= 200 && r.status < 400 });
          }
        } catch (e) {
          // ignore parse errors
        }
      }
    });

    // Check zones
    group('Zone Check', () => {
      const res = http.post(
        `${BASE_URL}/api/location/zone-check`,
        JSON.stringify({ latitude: 36.4513, longitude: 10.7357 }),
        authHeaders(token)
      );
      check(res, { 'zone check 2xx': (r) => r.status >= 200 && r.status < 400 });
    });

    sleep(0.5);
  });
}

function partnerFlow() {
  group('Partner Flow', () => {
    const partnerEmail = __ENV.PARTNER_EMAIL || 'partner@speedline-test.com';
    const partnerPassword = __ENV.PARTNER_PASSWORD || 'TestPartner123!';
    const token = login(partnerEmail, partnerPassword);
    if (!token) return;

    // List incoming orders
    group('Partner Orders', () => {
      const res = http.get(`${BASE_URL}/api/orders?status=PENDING`, authHeaders(token));
      check(res, { 'partner orders 2xx': (r) => r.status >= 200 && r.status < 400 });
    });

    // Get own menu
    group('Partner Menu', () => {
      const res = http.get(`${BASE_URL}/api/partners/me/menu`, authHeaders(token));
      check(res, { 'own menu 2xx': (r) => r.status >= 200 && r.status < 400 });
    });

    sleep(0.5);
  });
}

// Smoke test (quick sanity check)
export function smokeTest() {
  const res = http.get(`${BASE_URL}/actuator/health`);
  check(res, { 'API is up': (r) => r.status === 200 });
}
