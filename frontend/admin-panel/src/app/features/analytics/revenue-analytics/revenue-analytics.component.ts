// src/app/features/analytics/revenue-analytics/revenue-analytics.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
// import { NgChartsModule } from 'ng2-charts';
import { AnalyticsService } from '../services/analytics.service';

@Component({
  selector: 'app-revenue-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonToggleModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    // NgChartsModule,
  ],
  templateUrl: './revenue-analytics.component.html',
  styleUrls: ['./revenue-analytics.component.scss'],
})
export class RevenueAnalyticsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  period = 'month';
  loading = false;

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    // TODO: Implement
  }

  changePeriod(period: string): void {
    this.period = period;
    this.loadStats();
  }

  exportReport(): void {
    this.loading = true;
    this.analyticsService.exportReport('revenue', this.period).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `revenue-report-${this.period}.csv`;
        a.click();
        window.URL.revokeObjectURL(url);
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }
}
