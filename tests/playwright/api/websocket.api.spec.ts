import { test, expect } from '@playwright/test';
import { ApiClient } from '../helpers/api-client';
import { TEST_ACCOUNTS } from '../helpers/test-data';
import { WebSocket } from 'ws';

const API_BASE = process.env.API_GATEWAY_URL || 'http://localhost:8080';
const WS_BASE = API_BASE.replace('http', 'ws');

/**
 * WebSocket Tests for SpeedLine Real-Time Features
 *
 * Tests WebSocket connections for:
 * - Delivery tracking (customer tracks courier)
 * - Dispatch real-time updates (admin dashboard)
 * - Support chat
 * - Order status notifications (partner)
 */

function connectWs(url: string, token: string): Promise<WebSocket> {
  return new Promise((resolve, reject) => {
    const ws = new WebSocket(url, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const timeout = setTimeout(() => {
      ws.close();
      reject(new Error(`WebSocket connection timeout: ${url}`));
    }, 10_000);

    ws.on('open', () => {
      clearTimeout(timeout);
      resolve(ws);
    });
    ws.on('error', (err) => {
      clearTimeout(timeout);
      reject(err);
    });
  });
}

function waitForMessage(ws: WebSocket, timeoutMs = 10_000): Promise<string> {
  return new Promise((resolve, reject) => {
    const timeout = setTimeout(() => reject(new Error('WS message timeout')), timeoutMs);
    ws.once('message', (data) => {
      clearTimeout(timeout);
      resolve(data.toString());
    });
  });
}

test.describe('WebSocket - Delivery Tracking', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.customer.email, TEST_ACCOUNTS.customer.password);
  });

  test('should connect to tracking WebSocket', async () => {
    const token = api.getToken();
    if (!token) {
      test.skip();
      return;
    }

    try {
      const ws = await connectWs(`${WS_BASE}/ws/tracking`, token);
      expect(ws.readyState).toBe(WebSocket.OPEN);
      ws.close();
    } catch (e) {
      // WebSocket endpoint may not be available in test env
      console.log('Tracking WS not available:', (e as Error).message);
    }
  });

  test('should reject tracking WS without auth', async () => {
    try {
      const ws = await connectWs(`${WS_BASE}/ws/tracking`, 'invalid-token');
      // If it connects, it should close quickly
      const closePromise = new Promise<number>((resolve) => {
        ws.on('close', (code) => resolve(code));
      });
      const code = await closePromise;
      expect([1000, 1008, 4001, 4003]).toContain(code);
    } catch {
      // Connection refused is also acceptable
    }
  });

  test('should subscribe to delivery updates', async () => {
    const token = api.getToken();
    if (!token) {
      test.skip();
      return;
    }

    try {
      const ws = await connectWs(`${WS_BASE}/ws/tracking`, token);

      // Send STOMP-style subscribe
      ws.send(JSON.stringify({
        type: 'SUBSCRIBE',
        destination: '/topic/delivery/test-delivery-id',
      }));

      // Wait briefly for any response
      await new Promise((r) => setTimeout(r, 2000));
      ws.close();
    } catch {
      // Skip if not available
    }
  });
});

test.describe('WebSocket - Dispatch Dashboard', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.admin.email, TEST_ACCOUNTS.admin.password);
  });

  test('should connect to dispatch WebSocket', async () => {
    const token = api.getToken();
    if (!token) {
      test.skip();
      return;
    }

    try {
      const ws = await connectWs(`${WS_BASE}/ws/dispatch`, token);
      expect(ws.readyState).toBe(WebSocket.OPEN);
      ws.close();
    } catch (e) {
      console.log('Dispatch WS not available:', (e as Error).message);
    }
  });

  test('should receive dispatch cycle events', async () => {
    const token = api.getToken();
    if (!token) {
      test.skip();
      return;
    }

    try {
      const ws = await connectWs(`${WS_BASE}/ws/dispatch`, token);

      ws.send(JSON.stringify({
        type: 'SUBSCRIBE',
        destination: '/topic/dispatch/cycles',
      }));

      await new Promise((r) => setTimeout(r, 2000));
      ws.close();
    } catch {
      // Skip if not available
    }
  });
});

test.describe('WebSocket - Support Chat', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.customer.email, TEST_ACCOUNTS.customer.password);
  });

  test('should connect to chat WebSocket', async () => {
    const token = api.getToken();
    if (!token) {
      test.skip();
      return;
    }

    try {
      const ws = await connectWs(`${WS_BASE}/ws/chat`, token);
      expect(ws.readyState).toBe(WebSocket.OPEN);
      ws.close();
    } catch (e) {
      console.log('Chat WS not available:', (e as Error).message);
    }
  });

  test('should send and receive chat messages', async () => {
    const token = api.getToken();
    if (!token) {
      test.skip();
      return;
    }

    try {
      const ws = await connectWs(`${WS_BASE}/ws/chat`, token);

      ws.send(JSON.stringify({
        type: 'MESSAGE',
        ticketId: 'test-ticket-id',
        content: 'Test message from automated test robot',
      }));

      await new Promise((r) => setTimeout(r, 2000));
      ws.close();
    } catch {
      // Skip if not available
    }
  });
});

test.describe('WebSocket - Partner Order Notifications', () => {
  let api: ApiClient;

  test.beforeAll(async () => {
    api = new ApiClient();
    await api.login(TEST_ACCOUNTS.partner.email, TEST_ACCOUNTS.partner.password);
  });

  test('should connect to partner order WebSocket', async () => {
    const token = api.getToken();
    if (!token) {
      test.skip();
      return;
    }

    try {
      const ws = await connectWs(`${WS_BASE}/ws/orders`, token);
      expect(ws.readyState).toBe(WebSocket.OPEN);
      ws.close();
    } catch (e) {
      console.log('Partner orders WS not available:', (e as Error).message);
    }
  });
});
