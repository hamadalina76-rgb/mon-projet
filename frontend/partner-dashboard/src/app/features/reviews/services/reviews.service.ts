// src/app/features/reviews/services/reviews.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class ReviewsService {
  private api = inject(ApiService);

  getReviews(params?: any): Observable<any> {
    return this.api.get('partner/reviews', params);
  }

  getReviewStats(): Observable<any> {
    return this.api.get('partner/reviews/stats');
  }

  replyToReview(reviewId: string, reply: string): Observable<any> {
    return this.api.post(`partner/reviews/${reviewId}/reply`, { reply });
  }

  reportReview(reviewId: string, reason: string): Observable<any> {
    return this.api.post(`partner/reviews/${reviewId}/report`, { reason });
  }
}
