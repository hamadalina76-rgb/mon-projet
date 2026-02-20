// src/app/core/services/notification.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@environments/environment';

export interface Notification {
  id: string;
  userId: number;
  type: string;
  title: string;
  message: string;
  data?: any;
  isRead: boolean;
  createdAt: string;
}

@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  private http = inject(HttpClient);
  private baseUrl = environment.notificationsApiUrl;

  getNotifications(userId: number, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.baseUrl}/notifications/${userId}`, { params });
  }

  /** Notifications pour les admins (inclut les demandes partenaires userId=0) */
  getAdminNotifications(adminUserId: number, page: number = 0, size: number = 20): Observable<any> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<any>(`${this.baseUrl}/notifications/for-admin/${adminUserId}`, { params });
  }

  getUnreadCountForAdmin(adminUserId: number): Observable<{ count: number }> {
    return this.http.get<any>(`${this.baseUrl}/notifications/for-admin/${adminUserId}/unread-count`);
  }

  markAllAsReadForAdmin(adminUserId: number): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/notifications/for-admin/${adminUserId}/read-all`, {});
  }

  markAsRead(notificationId: string): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/notifications/${notificationId}/read`, {});
  }

  markAllAsRead(userId: number): Observable<any> {
    return this.http.put<any>(`${this.baseUrl}/notifications/${userId}/read-all`, {});
  }

  getUnreadCount(userId: number): Observable<any> {
    return this.http.get<any>(`${this.baseUrl}/notifications/${userId}/unread-count`);
  }
}
