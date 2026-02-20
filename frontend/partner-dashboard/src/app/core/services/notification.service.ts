// src/app/core/services/notification.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';

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
  private api = inject(ApiService);

  getNotifications(userId: number, page: number = 0, size: number = 20): Observable<any> {
    return this.api.get(`notifications/${userId}?page=${page}&size=${size}`);
  }

  markAsRead(notificationId: string): Observable<any> {
    return this.api.put(`notifications/${notificationId}/read`, {});
  }

  markAllAsRead(userId: number): Observable<any> {
    return this.api.put(`notifications/${userId}/read-all`, {});
  }

  getUnreadCount(userId: number): Observable<any> {
    return this.api.get(`notifications/${userId}/unread-count`);
  }

  // Browser notification methods for order alerts
  async requestBrowserPermission(): Promise<void> {
    if (!('Notification' in window)) {
      console.warn('Browser does not support notifications');
      return;
    }

    if (Notification.permission !== 'granted') {
      await Notification.requestPermission();
    }
  }

  showBrowserNotification(title: string, body: string, icon?: string): void {
    if (!('Notification' in window)) {
      return;
    }

    if (Notification.permission === 'granted') {
      new Notification(title, {
        body,
        icon: icon || '/assets/images/logo.svg',
        badge: '/assets/images/logo-icon.svg'
      });
    }
  }

  newOrderAlert(orderNumber: string): void {
    // Play sound notification
    const audio = new Audio('/assets/sounds/notification.mp3');
    audio.play().catch(err => console.warn('Could not play notification sound:', err));
    
    console.log(`New order received: ${orderNumber}`);
  }
}
