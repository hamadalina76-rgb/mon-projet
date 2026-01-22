// src/app/features/analytics/components/revenue-chart/revenue-chart.component.ts - Angular 19
import { Component, inject, input, effect, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { AnalyticsService } from '../../services/analytics.service';

@Component({
  selector: 'app-revenue-chart',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, BaseChartDirective],
  templateUrl: './revenue-chart.component.html',
  styleUrls: ['./revenue-chart.component.scss'],
})
export class RevenueChartComponent {
  private analyticsService = inject(AnalyticsService);

  // Angular 19 Signal Inputs
  period = input<string>('week');
  data = input<any[]>([]);

  // Signals
  chartLabels = signal<string[]>([]);
  chartValues = signal<number[]>([]);
  loading = signal(false);

  chartType: ChartType = 'line';
  
  chartData = computed<ChartConfiguration['data']>(() => {
    const inputData = this.data();
    if (inputData?.length > 0) {
      return {
        labels: inputData.map((d: any) => d.label),
        datasets: [
          {
            label: 'Revenus',
            data: inputData.map((d: any) => d.value),
            fill: true,
            borderColor: '#4CAF50',
            backgroundColor: 'rgba(76, 175, 80, 0.1)',
            tension: 0.4,
          },
        ],
      };
    }
    return {
      labels: this.chartLabels(),
      datasets: [
        {
          label: 'Revenus',
          data: this.chartValues(),
          fill: true,
          borderColor: '#4CAF50',
          backgroundColor: 'rgba(76, 175, 80, 0.1)',
          tension: 0.4,
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
    // Effect to reload data when period changes
    effect(() => {
      const p = this.period();
      if (p && this.data().length === 0) {
        this.loadData();
      }
    });
  }

  loadData(): void {
    // TODO: Implement API call
    this.chartLabels.set(['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim']);
    this.chartValues.set([1200, 1900, 1500, 2100, 1800, 2500, 2200]);
  }
}
