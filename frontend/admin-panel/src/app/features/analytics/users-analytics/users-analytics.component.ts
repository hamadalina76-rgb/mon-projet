// src/app/features/analytics/users-analytics/users-analytics.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { NgChartsModule } from 'ng2-charts';
import { AnalyticsService } from '../services/analytics.service';

@Component({
  selector: 'app-users-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonToggleModule,
    MatIconModule,
    TranslateModule,
    NgChartsModule,
  ],
  templateUrl: './users-analytics.component.html',
  styleUrls: ['./users-analytics.component.scss'],
})
export class UsersAnalyticsComponent implements OnInit {
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
