// src/app/features/analytics/partner-performance/partner-performance.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-partner-performance',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'analytics.partnerPerformance' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>Partner performance metrics</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class PartnerPerformanceComponent implements OnInit {
  ngOnInit(): void {}
}
