import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { TranslateModule } from '@ngx-translate/core';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration } from 'chart.js';
import { PromotionsService } from '../services/promotions.service';
import { PromotionDashboard } from '@core/models/promotion.model';

@Component({
  selector: 'app-promotions-dashboard',
  standalone: true,
  imports: [
    CommonModule, RouterModule, MatCardModule, MatButtonModule,
    MatIconModule, MatProgressSpinnerModule, MatTooltipModule,
    TranslateModule, BaseChartDirective,
  ],
  templateUrl: './promotions-dashboard.component.html',
  styleUrls: ['./promotions-dashboard.component.scss'],
})
export class PromotionsDashboardComponent implements OnInit {
  loading = signal(true);
  dashboard = signal<PromotionDashboard | null>(null);
  error = signal<string | null>(null);

  barChartData: ChartConfiguration<'bar'>['data'] = { labels: [], datasets: [] };
  barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    indexAxis: 'y',
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1a1a2e',
        cornerRadius: 8,
        padding: 10,
        titleFont: { size: 13 },
        bodyFont: { size: 12 },
      },
    },
    scales: {
      x: { beginAtZero: true, ticks: { precision: 0 }, grid: { display: false } },
      y: { grid: { display: false }, ticks: { font: { weight: 'bold', size: 12 } } },
    },
  };

  lineChartData: ChartConfiguration<'line'>['data'] = { labels: [], datasets: [] };
  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false },
      tooltip: {
        backgroundColor: '#1a1a2e',
        cornerRadius: 8,
        padding: 10,
        intersect: false,
        mode: 'index',
      },
    },
    scales: {
      x: { grid: { display: false }, ticks: { maxRotation: 0, maxTicksLimit: 10, font: { size: 11 } } },
      y: { beginAtZero: true, ticks: { precision: 0 }, grid: { color: 'rgba(0,0,0,0.04)' } },
    },
    elements: { point: { radius: 0, hoverRadius: 5 } },
  };

  constructor(private svc: PromotionsService) {}

  ngOnInit(): void {
    this.svc.getDashboard().subscribe({
      next: (d) => {
        this.dashboard.set(d);
        this.buildCharts(d);
        this.loading.set(false);
      },
      error: (e) => {
        this.error.set(e?.message || 'Erreur');
        this.loading.set(false);
      },
    });
  }

  private buildCharts(d: PromotionDashboard): void {
    const colors = ['#E31E24', '#1E293B', '#3B82F6', '#F59E0B', '#10B981'];
    this.barChartData = {
      labels: d.top5.map(p => p.code),
      datasets: [{
        data: d.top5.map(p => p.usageCount),
        backgroundColor: d.top5.map((_, i) => colors[i % colors.length]),
        borderRadius: 6,
        borderSkipped: false,
        barThickness: 28,
      }],
    };

    this.lineChartData = {
      labels: d.dailyUsages.map(u => {
        const dt = new Date(u.date);
        return `${dt.getDate()}/${dt.getMonth() + 1}`;
      }),
      datasets: [{
        data: d.dailyUsages.map(u => u.count),
        borderColor: '#E31E24',
        backgroundColor: 'rgba(227,30,36,0.06)',
        fill: true,
        tension: 0.4,
        borderWidth: 2.5,
      }],
    };
  }
}
