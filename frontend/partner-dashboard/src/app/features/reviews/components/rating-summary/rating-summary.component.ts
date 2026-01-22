// src/app/features/reviews/components/rating-summary/rating-summary.component.ts - Angular 19
import { Component, input, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-rating-summary',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatIconModule, MatProgressBarModule],
  templateUrl: './rating-summary.component.html',
  styleUrls: ['./rating-summary.component.scss'],
})
export class RatingSummaryComponent {
  // Angular 19 Signal Input
  stats = input<any>(null);

  // Computed values
  averageRating = computed(() => this.stats()?.averageRating ?? 0);
  totalReviews = computed(() => this.stats()?.totalReviews ?? 0);
  
  ratingDistribution = computed(() => {
    const s = this.stats();
    if (!s?.distribution) return [0, 0, 0, 0, 0];
    return [5, 4, 3, 2, 1].map(r => this.getRatingPercentage(r));
  });

  stars = computed(() => {
    const rating = this.averageRating();
    return Array(5).fill(0).map((_, i) => i < Math.round(rating));
  });

  getRatingPercentage(rating: number): number {
    const s = this.stats();
    if (!s?.distribution) return 0;
    const total = Object.values(s.distribution).reduce((a: any, b: any) => a + b, 0) as number;
    return total > 0 ? (s.distribution[rating] / total) * 100 : 0;
  }
}
