// src/app/features/analytics/custom-reports/custom-reports.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-custom-reports',
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
        <h2>{{ 'analytics.customReports' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <p>Custom report builder</p>
      </mat-card-content>
    </mat-card>
  `,
})
export class CustomReportsComponent implements OnInit {
  ngOnInit(): void {}
}
