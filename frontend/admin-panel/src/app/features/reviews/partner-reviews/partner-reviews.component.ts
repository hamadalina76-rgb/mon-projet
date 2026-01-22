// src/app/features/reviews/partner-reviews/partner-reviews.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { DataTableComponent } from '@shared/components/data-table/data-table.component';
import { ReviewsService } from '../services/reviews.service';

@Component({
  selector: 'app-partner-reviews',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
    DataTableComponent,
  ],
  templateUrl: './partner-reviews.component.html',
  styleUrls: ['./partner-reviews.component.scss'],
})
export class PartnerReviewsComponent implements OnInit {
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
