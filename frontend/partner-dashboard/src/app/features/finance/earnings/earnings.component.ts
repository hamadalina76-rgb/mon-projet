// src/app/features/finance/earnings/earnings.component.ts - Angular 19
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartType } from 'chart.js';
import { TranslateModule } from '@ngx-translate/core';
import { FinanceService } from '../services/finance.service';

@Component({
  selector: 'app-earnings',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatButtonToggleModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    BaseChartDirective,
    TranslateModule,
  ],
  templateUrl: './earnings.component.html',
  styleUrls: ['./earnings.component.scss'],
})
export class EarningsComponent implements OnInit {
  private financeService = inject(FinanceService);

  // Angular 19 Signals
  selectedPeriod = signal<string>('month');
  loading = signal(false);
  transactions = signal<any[]>([]);
  
  summary = signal({
    totalEarnings: 0,
    pendingPayout: 0,
    totalPaidOut: 0,
    ordersCount: 0,
  });

  chartLabels = signal<string[]>([]);
  chartValues = signal<number[]>([]);

  // Computed
  netEarnings = computed(() => {
    const s = this.summary();
    return s.totalEarnings - s.totalPaidOut;
  });

  displayedColumns = ['date', 'orderId', 'amount', 'commission', 'net'];

  chartType: ChartType = 'line';
  
  chartData = computed<ChartConfiguration['data']>(() => ({
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
  }));

  chartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
    },
  };

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    
    this.financeService.getEarningsSummary(this.selectedPeriod()).subscribe({
      next: (data: any) => {
        this.summary.set(data.summary);
        this.transactions.set(data.transactions);
        this.chartLabels.set(data.chart?.labels || []);
        this.chartValues.set(data.chart?.values || []);
        this.loading.set(false);
      },
      error: (err: any) => {
        console.error('Error loading earnings:', err);
        this.loading.set(false);
      }
    });
  }

  onPeriodChange(period: string): void {
    this.selectedPeriod.set(period);
    this.loadData();
  }

  onPageChange(event: PageEvent): void {
    // Load more transactions
  }

  exportData(): void {
    this.financeService.exportEarnings(this.selectedPeriod()).subscribe({
      next: (blob: Blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `earnings-${this.selectedPeriod()}.csv`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err: any) => console.error('Error exporting data:', err)
    });
  }
}
