// src/app/features/analytics/components/orders-chart/orders-chart.component.ts - Angular 19
import { Component, inject, input, effect, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-orders-chart',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, BaseChartDirective],
  templateUrl: './orders-chart.component.html',
  styleUrls: ['./orders-chart.component.scss'],
})
export class OrdersChartComponent {
  private analyticsService = inject(AnalyticsService);

  // Angular 19 Signal Inputs
  period = input<string>('week');
  data = input<any[]>([]);

  // Signals
  chartLabels = signal<string[]>([]);
  chartValues = signal<number[]>([]);
  loading = signal(false);

  chartType: ChartType = 'bar';
  
  chartData = computed<ChartConfiguration['data']>(() => {
    const inputData = this.data();
    if (inputData?.length > 0) {
      return {
        labels: inputData.map((d: any) => d.label),
        datasets: [
          {
            label: 'Commandes',
            data: inputData.map((d: any) => d.value),
            backgroundColor: '#2196F3',
            borderRadius: 8,
          },
        ],
      };
    }
    return {
      labels: this.chartLabels(),
      datasets: [
        {
          label: 'Commandes',
          data: this.chartValues(),
          backgroundColor: '#2196F3',
          borderRadius: 8,
        },
      ],
    };
  });

  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: {
        display: false,
      },
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: {
          color: 'rgba(0, 0, 0, 0.05)',
        },
      },
      x: {
        grid: {
          display: false,
        },
      },
    },
  };

  constructor() {
    effect(() => {
      const p = this.period();
      if (p && this.data().length === 0) {
        this.loadData();
      }
    });
  }

  loadData(): void {
    this.analyticsService.getOrdersData(this.period()).subscribe({
      next: (data: any) => {
        this.chartLabels.set(data.labels);
        this.chartValues.set(data.values);
      },
      error: () => {
        // Fallback data
        this.chartLabels.set(['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim']);
        this.chartValues.set([12, 19, 15, 21, 18, 25, 22]);
      }
    });
  }
}
