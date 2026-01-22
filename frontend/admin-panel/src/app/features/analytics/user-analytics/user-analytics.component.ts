// src/app/features/analytics/user-analytics/user-analytics.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-user-analytics',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    TranslateModule,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'analytics.users' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>User analytics and statistics</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class UserAnalyticsComponent implements OnInit {
  ngOnInit(): void {}
}
