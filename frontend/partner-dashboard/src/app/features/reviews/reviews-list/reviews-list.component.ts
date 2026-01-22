// src/app/features/reviews/reviews-list/reviews-list.component.ts - Angular 19
import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TranslateModule } from '@ngx-translate/core';
import { ReviewsService } from '../services/reviews.service';
import { ReviewCardComponent } from '../components/review-card/review-card.component';
import { RatingSummaryComponent } from '../components/rating-summary/rating-summary.component';

@Component({
  selector: 'app-reviews-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatSelectModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    TranslateModule,
    ReviewCardComponent,
    RatingSummaryComponent,
  ],
  templateUrl: './reviews-list.component.html',
  styleUrls: ['./reviews-list.component.scss'],
})
export class ReviewsListComponent implements OnInit {
  private reviewsService = inject(ReviewsService);
  private snackBar = inject(MatSnackBar);

  // Angular 19 Signals
  reviews = signal<any[]>([]);
  stats = signal<any>(null);
  loading = signal(false);
  filter = signal<string>('all');
  totalItems = signal(0);
  pageSize = signal(10);
  currentPage = signal(0);

  // Computed values
  averageRating = computed(() => {
    const s = this.stats();
    return s?.averageRating ?? 0;
  });

  filteredReviews = computed(() => {
    const allReviews = this.reviews();
    const currentFilter = this.filter();
    
    if (currentFilter === 'all') return allReviews;
    if (currentFilter === 'positive') return allReviews.filter(r => r.rating >= 4);
    if (currentFilter === 'negative') return allReviews.filter(r => r.rating <= 2);
    if (currentFilter === 'pending') return allReviews.filter(r => !r.reply);
    return allReviews;
  });

  ngOnInit(): void {
    this.loadStats();
    this.loadReviews();
  }

  loadStats(): void {
    this.reviewsService.getReviewStats().subscribe({
      next: (data: any) => this.stats.set(data),
      error: (err: any) => console.error('Error loading stats:', err)
    });
  }

  loadReviews(page: number = 0): void {
    this.loading.set(true);
    this.currentPage.set(page);
    
    this.reviewsService.getReviews({ page, size: this.pageSize() }).subscribe({
      next: (response) => {
        this.reviews.set(response.items);
        this.totalItems.set(response.total);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Error loading reviews:', err);
        this.loading.set(false);
      }
    });
  }

  onFilterChange(filter: string): void {
    this.filter.set(filter);
    this.loadReviews();
  }

  onPageChange(event: PageEvent): void {
    this.pageSize.set(event.pageSize);
    this.loadReviews(event.pageIndex);
  }

  replyToReview(reviewId: string, reply: string): void {
    this.reviewsService.replyToReview(reviewId, reply).subscribe({
      next: () => {
        this.reviews.update(reviews => 
          reviews.map(r => r.id === reviewId ? { ...r, reply } : r)
        );
        this.snackBar.open('Réponse envoyée', 'OK', { duration: 3000 });
      },
      error: (err) => {
        console.error('Error replying to review:', err);
        this.snackBar.open('Erreur lors de l\'envoi', 'OK', { duration: 3000 });
      }
    });
  }
}
