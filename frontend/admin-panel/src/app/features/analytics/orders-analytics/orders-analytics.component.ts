// src/app/features/analytics/orders-analytics/orders-analytics.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { NgChartsModule } from 'ng2-charts';
import { AnalyticsService } from '../services/analytics.service';

@Component({
  selector: 'app-orders-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonToggleModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    NgChartsModule,
  ],
  templateUrl: './orders-analytics.component.html',
  styleUrls: ['./orders-analytics.component.scss'],
})
export class OrdersAnalyticsComponent implements OnInit {
  private analyticsService = inject(AnalyticsService);

  period = 'month';
  loading = false;

  ngOnInit(): void {
    this.loadStats();
  }

  loadStats(): void {
    // TODO: Implement
  }
}
