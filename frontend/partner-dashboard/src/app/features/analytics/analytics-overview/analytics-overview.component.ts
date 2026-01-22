// src/app/features/analytics/analytics-overview/analytics-overview.component.ts - Angular 19
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { AnalyticsService } from '../services/analytics.service';
import { RevenueChartComponent } from '../components/revenue-chart/revenue-chart.component';
import { OrdersChartComponent } from '../components/orders-chart/orders-chart.component';
import { PerformanceMetricsComponent } from '../components/performance-metrics/performance-metrics.component';

@Component({
  selector: 'app-analytics-overview',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatButtonToggleModule,
    MatSelectModule,
    MatMenuModule,
    MatProgressSpinnerModule,
    TranslateModule,
    RevenueChartComponent,
    OrdersChartComponent,
    PerformanceMetricsComponent,
  ],
  templateUrl: './analytics-overview.component.html',
  styleUrls: ['./analytics-overview.component.scss'],
})
export class AnalyticsOverviewComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  // Angular 19 Signals
  selectedPeriod = signal<string>('week');
  overview = signal<any>(null);
  revenueData = signal<any[]>([]);
  ordersData = signal<any[]>([]);
  loading = signal(false);

  // Computed values
  totalRevenue = computed(() => {
    const data = this.overview();
    return data?.totalRevenue ?? 0;
  });

  totalOrders = computed(() => {
    const data = this.overview();
    return data?.totalOrders ?? 0;
  });

  averageOrderValue = computed(() => {
    const revenue = this.totalRevenue();
    const orders = this.totalOrders();
    return orders > 0 ? revenue / orders : 0;
  });

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    const period = this.selectedPeriod();
    
    this.analyticsService.getOverview(period).subscribe({
      next: (data) => {
        this.overview.set(data);
        this.revenueData.set(data.revenueChart || []);
        this.ordersData.set(data.ordersChart || []);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading analytics:', err);
        this.loading.set(false);
      }
    });
  }

  onPeriodChange(period: string): void {
    this.selectedPeriod.set(period);
    this.loadData();
  }

  exportData(format: string): void {
    this.analyticsService.exportReport(this.selectedPeriod(), format).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `analytics-${this.selectedPeriod()}.${format}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (err) => console.error('Error exporting data:', err)
    });
  }
}
