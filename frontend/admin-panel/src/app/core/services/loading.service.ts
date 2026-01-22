// src/app/core/services/loading.service.ts
import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class LoadingService {
  private loadingSignal = signal(false);
  private requestCount = 0;

  isLoading = this.loadingSignal.asReadonly();

  show(): void {
    this.requestCount++;
    this.loadingSignal.set(true);
  }

  hide(): void {
    this.requestCount--;
    if (this.requestCount <= 0) {
      this.requestCount = 0;
      this.loadingSignal.set(false);
    }
  }
}
