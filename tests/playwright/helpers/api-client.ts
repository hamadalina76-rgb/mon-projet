import { APIRequestContext, request } from '@playwright/test';

export class ApiClient {
  private baseUrl: string;
  private token: string | null = null;

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || process.env.API_GATEWAY_URL || 'http://localhost:8080';
  }

  async login(email: string, password: string): Promise<string> {
    const ctx = await request.newContext({ baseURL: this.baseUrl });
    const response = await ctx.post('/api/v1/auth/login', {
      data: { email, password },
    });

    if (!response.ok()) {
      throw new Error(`Login failed: ${response.status()} ${await response.text()}`);
    }

    const body = await response.json();
    this.token = body.access_token || body.accessToken || body.token;
    await ctx.dispose();
    return this.token!;
  }

  async authenticatedContext(): Promise<APIRequestContext> {
    if (!this.token) {
      throw new Error('Not logged in. Call login() first.');
    }

    return request.newContext({
      baseURL: this.baseUrl,
      extraHTTPHeaders: {
        Authorization: `Bearer ${this.token}`,
      },
    });
  }

  async get(path: string) {
    const ctx = await this.authenticatedContext();
    const res = await ctx.get(path);
    await ctx.dispose();
    return res;
  }

  async post(path: string, data: unknown) {
    const ctx = await this.authenticatedContext();
    const res = await ctx.post(path, { data });
    await ctx.dispose();
    return res;
  }

  async put(path: string, data: unknown) {
    const ctx = await this.authenticatedContext();
    const res = await ctx.put(path, { data });
    await ctx.dispose();
    return res;
  }

  async delete(path: string) {
    const ctx = await this.authenticatedContext();
    const res = await ctx.delete(path);
    await ctx.dispose();
    return res;
  }

  getToken(): string | null {
    return this.token;
  }
}
