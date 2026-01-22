// src/app/features/dashboard/components/orders-chart/orders-chart.component.ts - Angular 19
import { Component, input, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';

@Component({
  selector: 'app-orders-chart',
  standalone: true,
  imports: [CommonModule, MatProgressSpinnerModule, BaseChartDirective],
  templateUrl: './orders-chart.component.html',
  styleUrls: ['./orders-chart.component.scss'],
})
export class OrdersChartComponent {
  // Angular 19 Signal Input
  data = input<any>(null);
  
  // Loading state
  loading = signal(false);

  chartType: ChartType = 'bar';

  // Chart data as computed from input
  chartData = computed<ChartConfiguration['data']>(() => {
    const inputData = this.data();
    return {
      labels: inputData?.labels || ['Lun', 'Mar', 'Mer', 'Jeu', 'Ven', 'Sam', 'Dim'],
      datasets: [
        {
          data: inputData?.values || [],
          label: 'Commandes',
          backgroundColor: 'rgba(255, 87, 34, 0.7)',
          borderColor: 'rgb(255, 87, 34)',
          borderWidth: 1,
        },
      ],
    };
  });

  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
    },
  };
}
