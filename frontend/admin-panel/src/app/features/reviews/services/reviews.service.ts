// src/app/features/reviews/services/reviews.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class ReviewsService {
  private api = inject(ApiService);

  getPartnerReviews(page: number, pageSize: number): Observable<any> {
    const params = new HttpParams().set('page', String(page)).set('size', String(pageSize));
    return this.api.get('admin/reviews/partners', params);
  }

  getCourierReviews(page: number, pageSize: number): Observable<any> {
    const params = new HttpParams().set('page', String(page)).set('size', String(pageSize));
    return this.api.get('admin/reviews/couriers', params);
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
