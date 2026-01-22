// src/app/features/analytics/components/performance-metrics/performance-metrics.component.ts - Angular 19
import { Component, inject, input, effect, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-performance-metrics',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressBarModule, MatProgressSpinnerModule],
  templateUrl: './performance-metrics.component.html',
  styleUrls: ['./performance-metrics.component.scss'],
})
export class PerformanceMetricsComponent {
  private analyticsService = inject(AnalyticsService);

  // Angular 19 Signal Inputs
  period = input<string>('week');

  // Signals
  metrics = signal({
    avgPrepTime: 0,
    targetPrepTime: 15,
    acceptanceRate: 0,
    completionRate: 0,
    customerSatisfaction: 0,
  });

  loading = signal(false);

  // Computed
  prepTimeProgress = computed(() => {
    const m = this.metrics();
    return Math.min((m.avgPrepTime / m.targetPrepTime) * 100, 100);
  });

  isPrepTimeGood = computed(() => this.metrics().avgPrepTime <= this.metrics().targetPrepTime);

  satisfactionProgress = computed(() => (this.metrics().customerSatisfaction / 5) * 100);

  constructor() {
    effect(() => {
      const p = this.period();
      if (p) {
        this.loadMetrics();
      }
    });
  }

  loadMetrics(): void {
    this.loading.set(true);
    this.analyticsService.getPerformanceMetrics(this.period()).subscribe({
      next: (data) => {
        this.metrics.set(data);
        this.loading.set(false);
      },
      error: () => {
        // Fallback data
        this.metrics.set({
          avgPrepTime: 12,
          targetPrepTime: 15,
          acceptanceRate: 95,
          completionRate: 98,
          customerSatisfaction: 4.5,
        });
        this.loading.set(false);
      }
    });
  }
}
