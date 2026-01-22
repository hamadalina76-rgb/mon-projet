// src/app/features/reviews/services/reviews.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class ReviewsService {
  private api = inject(ApiService);

  getPartnerReviews(page: number, pageSize: number): Observable<any> {
    return this.api.get('admin/reviews/partners', { page, size: pageSize });
  }

  getCourierReviews(page: number, pageSize: number): Observable<any> {
    return this.api.get('admin/reviews/couriers', { page, size: pageSize });
  }

  moderateReview(id: string, action: 'approve' | 'reject'): Observable<any> {
    return this.api.post(`admin/reviews/${id}/${action}`, {});
  }

  deleteReview(id: string): Observable<any> {
    return this.api.delete(`admin/reviews/${id}`);
  }

  getReportedReviews(): Observable<any> {
    return this.api.get('admin/reviews/reported');
  }
}
