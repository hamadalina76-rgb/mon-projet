import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

declare global {
  interface Window {
    __RUNTIME_CONFIG__?: Record<string, any>;
  }
}

@Injectable({
  providedIn: 'root',
})
export class RuntimeConfigService {
  constructor(private http: HttpClient) {}

  async load(): Promise<void> {
    try {
      const config = await firstValueFrom(
        this.http.get<Record<string, any>>('/assets/config/config.json')
      );
      window.__RUNTIME_CONFIG__ = {
        ...(window.__RUNTIME_CONFIG__ || {}),
        ...(config || {}),
      };
    } catch {
      window.__RUNTIME_CONFIG__ = window.__RUNTIME_CONFIG__ || {};
    }
  }
}
