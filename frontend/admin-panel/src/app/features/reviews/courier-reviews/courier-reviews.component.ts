// src/app/features/reviews/courier-reviews/courier-reviews.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { ReviewsService } from '../services/reviews.service';

@Component({
  selector: 'app-courier-reviews',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './courier-reviews.component.html',
  styleUrls: ['./courier-reviews.component.scss'],
})
export class CourierReviewsComponent implements OnInit {
  private reviewsService = inject(ReviewsService);

  reviews: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadReviews();
  }

  loadReviews(): void {
    // TODO: Implement
  }
}
