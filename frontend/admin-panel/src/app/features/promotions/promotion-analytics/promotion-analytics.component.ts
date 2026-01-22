// src/app/features/promotions/promotion-analytics/promotion-analytics.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-promotion-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'promotions.analytics' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>Promotion analytics will be displayed here</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class PromotionAnalyticsComponent implements OnInit {
  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    // TODO: Implement
  }
}
