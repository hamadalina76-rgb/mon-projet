// src/app/features/reviews/moderation/moderation.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslateModule } from '@ngx-translate/core';
import { ReviewsService } from '../services/reviews.service';

@Component({
  selector: 'app-moderation',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    TranslateModule,
  ],
  templateUrl: './moderation.component.html',
  styleUrls: ['./moderation.component.scss'],
})
export class ModerationComponent implements OnInit {
  private reviewsService = inject(ReviewsService);

  reportedReviews: any[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadReportedReviews();
  }

  loadReportedReviews(): void {
    // TODO: Implement
  }

  approveReview(id: string): void {
    // TODO: Implement
  }

  rejectReview(id: string): void {
    // TODO: Implement
  }
}
