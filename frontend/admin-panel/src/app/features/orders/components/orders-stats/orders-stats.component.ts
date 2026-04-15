import { Component, OnInit, OnDestroy, inject, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { Subject, Subscription, timer } from 'rxjs';
import { takeUntil, debounceTime } from 'rxjs/operators';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule, TranslateService } from '@ngx-translate/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { OrdersService } from '../../services/orders.service';
import { WebSocketService } from '@core/services/websocket.service';
import {
  OrderStatsKpis,
  OrderStatsChart,
  OrderStatsDistribution,
  OrderStatsResponse,
} from '../../models/admin-order.model';

interface TrendInfo {
  direction: 'up' | 'down' | 'flat';
  percent: number;
  positive: boolean;
}

@Component({
  selector: 'app-orders-stats',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterModule,
    MatButtonToggleModule,
    MatDatepickerModule, MatNativeDateModule,
    MatFormFieldModule, MatInputModule, MatIconModule,
    MatTooltipModule, MatProgressSpinnerModule,
    TranslateModule, BaseChartDirective,
  ],
  templateUrl: './orders-stats.component.html',
  styleUrls: ['./orders-stats.component.scss'],
})
export class OrdersStatsComponent implements OnInit, OnDestroy {
  private ordersService = inject(OrdersService);
  private translate = inject(TranslateService);
  private wsService = inject(WebSocketService);
  private destroy$ = new Subject<void>();
  private wsRefresh$ = new Subject<void>();
  private wsSub?: Subscription;
  private timerSub?: Subscription;

  @ViewChild('barChart') barChart?: BaseChartDirective;
  @ViewChild('doughnutChart') doughnutChartRef?: BaseChartDirective;

  loading = false;
  firstLoad = true;
  hasError = false;
  lastRefresh = new Date();
  granularity: 'HOUR' | 'DAY' = 'HOUR';
  selectedDate = new Date();
  maxDate = new Date();

  // Animated display values
  display = { totalOrders: 0, activeOrders: 0, cancelRate: 0, avgDeliveryMinutes: 0, totalRevenueTND: 0 };

  // Raw KPI values
  kpis: OrderStatsKpis = {
    totalOrders: 0, activeOrders: 0, cancelRate: 0, avgDeliveryMinutes: 0, totalRevenueTND: 0,
    prevTotalOrders: 0, prevCancelRate: 0, prevAvgDeliveryMinutes: 0, prevRevenueTND: 0,
  };

  // Trend indicators
  trends: Record<string, TrendInfo> = {
    totalOrders: { direction: 'flat', percent: 0, positive: true },
    cancelRate: { direction: 'flat', percent: 0, positive: true },
    avgDelivery: { direction: 'flat', percent: 0, positive: true },
    revenue: { direction: 'flat', percent: 0, positive: true },
  };

  // Distribution (doughnut)
  distribution: OrderStatsDistribution = { pending: 0, confirmed: 0, preparing: 0, inDelivery: 0, delivered: 0, cancelled: 0 };
  doughnutData: ChartData<'doughnut'> = { labels: [], datasets: [] };
  doughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    animation: { duration: 800, easing: 'easeOutQuart' },
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1E293B',
        titleFont: { size: 12, weight: 'bold' },
        bodyFont: { size: 11 },
        padding: 12,
        cornerRadius: 10,
        boxPadding: 4,
        usePointStyle: true,
      },
    },
  };

  // Bar chart
  chartData: ChartData<'bar'> = { labels: [], datasets: [] };
  chartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    animation: { duration: 700, easing: 'easeOutQuart' },
    interaction: { mode: 'index', intersect: false },
    plugins: {
      legend: {
        position: 'bottom',
        labels: { usePointStyle: true, pointStyle: 'rectRounded', padding: 20, font: { size: 12, weight: 'bold' } },
      },
      tooltip: {
        backgroundColor: '#1E293B',
        titleFont: { size: 12, weight: 'bold' },
        bodyFont: { size: 11 },
        padding: 12,
        cornerRadius: 10,
        boxPadding: 4,
        usePointStyle: true,
      },
    },
    scales: {
      x: { stacked: true, grid: { display: false }, ticks: { font: { size: 11 }, maxRotation: 0 } },
      y: {
        stacked: true,
        beginAtZero: true,
        grid: { color: 'rgba(0,0,0,.04)' },
        ticks: { stepSize: 1, font: { size: 11 }, precision: 0 },
      },
    },
  };

  get distributionTotal(): number {
    const d = this.distribution;
    return d.pending + d.confirmed + d.preparing + d.inDelivery + d.delivered + d.cancelled;
  }

  ngOnInit(): void {
    this.startAutoRefresh();

    this.wsRefresh$.pipe(debounceTime(500), takeUntil(this.destroy$))
      .subscribe(() => this.loadStats());

    this.wsSub = this.wsService.onAdminNotification
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.wsRefresh$.next());
  }

  ngOnDestroy(): void {
    this.wsSub?.unsubscribe();
    this.timerSub?.unsubscribe();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private startAutoRefresh(): void {
    this.timerSub?.unsubscribe();
    this.timerSub = timer(0, 300_000)
      .pipe(takeUntil(this.destroy$))
      .subscribe(() => this.loadStats());
  }

  onGranularityChange(): void { this.loadStats(); }
  onDateChange(): void { this.loadStats(); }

  goToday(): void {
    this.selectedDate = new Date();
    this.loadStats();
  }

  refresh(): void {
    this.startAutoRefresh();
  }

  private formatDate(d: Date): string {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  get isToday(): boolean {
    return this.selectedDate.toDateString() === new Date().toDateString();
  }

  loadStats(): void {
    this.loading = true;
    this.hasError = false;
    this.ordersService.getStats(this.formatDate(this.selectedDate), this.granularity).subscribe({
      next: (res: OrderStatsResponse) => {
        this.kpis = res.kpis;
        this.animateCounters(res.kpis);
        this.computeTrends(res.kpis);
        if (res.distribution) {
          this.distribution = res.distribution;
          this.buildDoughnut(res.distribution);
        }
        this.buildChart(res.chart);
        this.lastRefresh = new Date();
        this.loading = false;
        this.firstLoad = false;
      },
      error: () => {
        this.loading = false;
        this.firstLoad = false;
        this.hasError = true;
        // Stop auto-refresh on error to avoid spamming failed requests
        this.timerSub?.unsubscribe();
      },
    });
  }

  private animateCounters(target: OrderStatsKpis): void {
    const duration = 600;
    const start = { ...this.display };
    const startTime = performance.now();

    const step = (now: number) => {
      const t = Math.min((now - startTime) / duration, 1);
      const ease = 1 - Math.pow(1 - t, 3);
      this.display = {
        totalOrders: Math.round(start.totalOrders + (target.totalOrders - start.totalOrders) * ease),
        activeOrders: Math.round(start.activeOrders + (target.activeOrders - start.activeOrders) * ease),
        cancelRate: +(start.cancelRate + (target.cancelRate - start.cancelRate) * ease).toFixed(1),
        avgDeliveryMinutes: +(start.avgDeliveryMinutes + (target.avgDeliveryMinutes - start.avgDeliveryMinutes) * ease).toFixed(1),
        totalRevenueTND: +(start.totalRevenueTND + (target.totalRevenueTND - start.totalRevenueTND) * ease).toFixed(2),
      };
      if (t < 1) requestAnimationFrame(step);
    };
    requestAnimationFrame(step);
  }

  private computeTrends(k: OrderStatsKpis): void {
    this.trends = {
      totalOrders: this.calcTrend(k.totalOrders, k.prevTotalOrders, true),
      cancelRate: this.calcTrend(k.cancelRate, k.prevCancelRate, false),
      avgDelivery: this.calcTrend(k.avgDeliveryMinutes, k.prevAvgDeliveryMinutes, false),
      revenue: this.calcTrend(k.totalRevenueTND, k.prevRevenueTND, true),
    };
  }

  private calcTrend(current: number, prev: number, upIsGood: boolean): TrendInfo {
    if (prev === 0 && current === 0) return { direction: 'flat', percent: 0, positive: true };
    if (prev === 0) return { direction: 'up', percent: 100, positive: upIsGood };
    const pct = Math.round(((current - prev) / prev) * 100);
    const dir = pct > 0 ? 'up' : pct < 0 ? 'down' : 'flat';
    const positive = dir === 'flat' || (dir === 'up' && upIsGood) || (dir === 'down' && !upIsGood);
    return { direction: dir, percent: Math.abs(pct), positive };
  }

  private buildDoughnut(d: OrderStatsDistribution): void {
    this.doughnutData = {
      labels: [
        this.translate.instant('orders.stats.dist.pending'),
        this.translate.instant('orders.stats.dist.confirmed'),
        this.translate.instant('orders.stats.dist.preparing'),
        this.translate.instant('orders.stats.dist.inDelivery'),
        this.translate.instant('orders.stats.dist.delivered'),
        this.translate.instant('orders.stats.dist.cancelled'),
      ],
      datasets: [{
        data: [d.pending, d.confirmed, d.preparing, d.inDelivery, d.delivered, d.cancelled],
        backgroundColor: [
          '#F59E0B', // pending - amber
          '#6366F1', // confirmed - indigo
          '#3B82F6', // preparing - blue
          '#8B5CF6', // in delivery - purple
          '#10B981', // delivered - green
          '#EF4444', // cancelled - red
        ],
        borderWidth: 0,
        hoverOffset: 6,
      }],
    };
  }

  private buildChart(chart: OrderStatsChart): void {
    this.chartData = {
      labels: chart.labels,
      datasets: [
        {
          label: this.translate.instant('orders.stats.chart.newOrders'),
          data: chart.newOrders,
          backgroundColor: 'rgba(59, 130, 246, 0.8)',
          hoverBackgroundColor: 'rgba(59, 130, 246, 1)',
          borderRadius: 4,
        },
        {
          label: this.translate.instant('orders.stats.chart.delivered'),
          data: chart.deliveredOrders,
          backgroundColor: 'rgba(16, 185, 129, 0.8)',
          hoverBackgroundColor: 'rgba(16, 185, 129, 1)',
          borderRadius: 4,
        },
        {
          label: this.translate.instant('orders.stats.chart.cancelled'),
          data: chart.cancelledOrders,
          backgroundColor: 'rgba(239, 68, 68, 0.8)',
          hoverBackgroundColor: 'rgba(239, 68, 68, 1)',
          borderRadius: 4,
        },
      ],
    };
  }
}
