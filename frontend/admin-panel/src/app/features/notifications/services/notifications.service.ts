// src/app/features/notifications/services/notifications.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class NotificationsService {
  private api = inject(ApiService);

  sendPushNotification(data: any): Observable<any> {
    return this.api.post('admin/notifications/push', data);
  }

  getNotificationHistory(): Observable<any> {
    return this.api.get('admin/notifications/history');
  }

  getTemplates(): Observable<any> {
    return this.api.get('admin/notifications/templates');
  }

  createTemplate(data: any): Observable<any> {
    return this.api.post('admin/notifications/templates', data);
  }

  updateTemplate(id: string, data: any): Observable<any> {
    return this.api.put(`admin/notifications/templates/${id}`, data);
  }

  scheduleNotification(data: any): Observable<any> {
    return this.api.post('admin/notifications/schedule', data);
  }
}
