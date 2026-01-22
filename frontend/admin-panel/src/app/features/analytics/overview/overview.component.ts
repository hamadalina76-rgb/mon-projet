// src/app/features/analytics/overview/overview.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatGridListModule } from '@angular/material/grid-list';
import { TranslateModule } from '@ngx-translate/core';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatGridListModule,
    TranslateModule,
  ],
  template: `
    <div class="overview-grid">
      <mat-card>
        <mat-card-header>
          <h2>{{ 'analytics.overview' | translate }}</h2>
        </mat-card-header>
        <mat-card-content>
          <mat-grid-list cols="4" rowHeight="100px">
            <mat-grid-tile>
              <div class="stat-tile">
                <h3>Total Orders</h3>
                <p>{{ totalOrders }}</p>
              </div>
            </mat-grid-tile>
            <mat-grid-tile>
              <div class="stat-tile">
                <h3>Revenue</h3>
                <p>{{ totalRevenue | currency }}</p>
              </div>
            </mat-grid-tile>
            <mat-grid-tile>
              <div class="stat-tile">
                <h3>Active Users</h3>
                <p>{{ activeUsers }}</p>
              </div>
            </mat-grid-tile>
            <mat-grid-tile>
              <div class="stat-tile">
                <h3>Partners</h3>
                <p>{{ totalPartners }}</p>
              </div>
            </mat-grid-tile>
          </mat-grid-list>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .overview-grid {
      padding: 20px;
    }
    .stat-tile {
      text-align: center;
    }
  `]
})
export class OverviewComponent implements OnInit {
  totalOrders = 0;
  totalRevenue = 0;
  activeUsers = 0;
  totalPartners = 0;

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    // TODO: Implement
  }
}
