// src/app/features/promotions/promotions-list/promotions-list.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';

@Component({
  selector: 'app-promotions-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <h2>{{ 'promotions.list' | translate }}</h2>
        <button mat-raised-button color="primary" (click)="createPromotion()">
          <mat-icon>add</mat-icon>
          New Promotion
        </button>
      </mat-card-header>
      <mat-card-content>
        <app-data-table [data]="promotions" [loading]="loading"></app-data-table>
      </mat-card-content>
    </mat-card>
  `,
})
export class PromotionsListComponent implements OnInit {
  private router = inject(Router);

  promotions: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadPromotions();
  }

  loadPromotions(): void {
    this.loading = true;
    // TODO: Implement
    this.promotions = [];
    this.loading = false;
  }

  createPromotion(): void {
    this.router.navigate(['/promotions/new']);
  }
}
