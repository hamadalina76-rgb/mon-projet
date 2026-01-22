// src/app/features/analytics/courier-performance/courier-performance.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-courier-performance',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'analytics.courierPerformance' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>Courier performance metrics</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class CourierPerformanceComponent implements OnInit {
  ngOnInit(): void {}
}
