// src/app/features/reviews/reviews-list/reviews-list.component.ts
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';

@Component({
  selector: 'app-reviews-list',
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
        <h2>{{ 'reviews.all' | translate }}</h2>
      </mat-card-header>
      <mat-card-content>
        <app-data-table [data]="reviews" [loading]="loading"></app-data-table>
      </mat-card-content>
    </mat-card>
  `,
})
export class ReviewsListComponent implements OnInit {
  reviews: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews(): void {
    this.loading = true;
    // TODO: Implement
    this.reviews = [];
    this.loading = false;
  }
}
