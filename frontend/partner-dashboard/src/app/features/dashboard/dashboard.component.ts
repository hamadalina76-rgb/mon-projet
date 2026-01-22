// src/app/features/dashboard/dashboard.component.ts
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TranslateModule } from '@ngx-translate/core';
import { DashboardService } from './services/dashboard.service';
import { StatsCardComponent } from './components/stats-card/stats-card.component';
import { OrdersChartComponent } from './components/orders-chart/orders-chart.component';
import { RecentOrdersComponent } from './components/recent-orders/recent-orders.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    TranslateModule,
    StatsCardComponent,
    OrdersChartComponent,
    RecentOrdersComponent,
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss'],
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  // Angular 19 Signals
  partnerName = signal('Partenaire');
  loading = signal(false);
  
  stats = signal({
    ordersToday: 0,
    ordersTrend: 0,
    revenueToday: 0,
    revenueTrend: 0,
    averageRating: 0,
    avgPrepTime: 0,
    ordersChart: [] as number[],
    recentOrders: [] as any[],
    popularProducts: [] as any[],
  });

  // Computed signals
  hasRecentOrders = computed(() => this.stats().recentOrders.length > 0);
  hasPopularProducts = computed(() => this.stats().popularProducts.length > 0);

  ngOnInit(): void {
    this.loadDashboard();
  }

  loadDashboard(): void {
    this.loading.set(true);
    this.dashboardService.getDashboardStats().subscribe({
      next: (data) => {
        this.stats.set(data);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }
}
