import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatTableModule } from '@angular/material/table';
import { TranslateModule } from '@ngx-translate/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { PromotionsService } from '../services/promotions.service';
import { PromotionAnalytics, Promotion } from '@core/models/promotion.model';

@Component({
  selector: 'app-promotion-analytics',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatCardModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule, MatTooltipModule,
    MatTableModule, TranslateModule, BaseChartDirective,
  ],
  templateUrl: './promotion-analytics.component.html',
  styleUrls: ['./promotion-analytics.component.scss'],
})
export class PromotionAnalyticsComponent implements OnInit {
  loading = signal(true);
  promotion = signal<Promotion | null>(null);
  analytics = signal<PromotionAnalytics | null>(null);
  error = signal<string | null>(null);

  // Doughnut — quota usage
  doughnutData: ChartConfiguration<'doughnut'>['data'] = { labels: [], datasets: [] };
  doughnutOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    cutout: '72%',
    plugins: {
      legend: { display: false },
      tooltip: { backgroundColor: '#1a1a2e', cornerRadius: 8, padding: 10 },
    },
  };

  // Bar — applied vs revoked
  statusBarData: ChartConfiguration<'bar'>['data'] = { labels: [], datasets: [] };
  statusBarOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: { legend: { position: 'bottom', labels: { usePointStyle: true, padding: 16 } } },
    scales: {
      x: { grid: { display: false } },
      y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: 'rgba(0,0,0,.04)' } },
    },
  };

  displayedColumns = ['userId', 'orderId', 'discountAmount', 'status', 'createdAt'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private svc: PromotionsService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.router.navigate(['/promotions']); return; }
    this.loadData(+id);
  }

  private loadData(id: number): void {
    this.svc.getById(id).subscribe({
      next: (detail) => {
        this.promotion.set(detail.promotion);
        this.svc.getAnalytics(id).subscribe({
          next: (a) => {
            this.analytics.set(a);
            this.buildCharts(detail.promotion, a);
            this.loading.set(false);
          },
          error: () => { this.error.set('Erreur chargement analytics'); this.loading.set(false); },
        });
      },
      error: () => { this.error.set('Promotion introuvable'); this.loading.set(false); },
    });
  }

  private buildCharts(p: Promotion, a: PromotionAnalytics): void {
    // Doughnut
    const used = p.usageCount ?? 0;
    const limit = p.usageLimitTotal ?? 0;
    const remaining = Math.max(limit - used, 0);
    if (limit > 0) {
      this.doughnutData = {
        labels: ['Utilisé', 'Restant'],
        datasets: [{
          data: [used, remaining],
          backgroundColor: ['#E31E24', '#f1f5f9'],
          borderWidth: 0,
        }],
      };
    }

    // Status bar
    this.statusBarData = {
      labels: ['Utilisations'],
      datasets: [
        { label: 'Appliquées', data: [a.totalApplied], backgroundColor: '#10B981', borderRadius: 6 },
        { label: 'Révoquées', data: [a.totalRevoked], backgroundColor: '#EF4444', borderRadius: 6 },
      ],
    };
  }

  get usageRate(): number {
    const p = this.promotion();
    if (!p?.usageLimitTotal) return 0;
    return Math.round((p.usageCount / p.usageLimitTotal) * 100);
  }

  get usageBarColor(): string {
    const r = this.usageRate;
    if (r >= 90) return '#EF4444';
    if (r >= 70) return '#F59E0B';
    return '#10B981';
  }

  goBack(): void { this.router.navigate(['/promotions']); }
}
