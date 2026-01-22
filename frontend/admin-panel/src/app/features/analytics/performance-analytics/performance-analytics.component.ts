// src/app/features/analytics/performance-analytics/performance-analytics.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { NgChartsModule } from 'ng2-charts';
import { AnalyticsService } from '../services/analytics.service';

@Component({
  selector: 'app-performance-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    TranslateModule,
    NgChartsModule,
  ],
  templateUrl: './performance-analytics.component.html',
  styleUrls: ['./performance-analytics.component.scss'],
})
export class PerformanceAnalyticsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  loading = false;

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    // TODO: Implement
  }
}
