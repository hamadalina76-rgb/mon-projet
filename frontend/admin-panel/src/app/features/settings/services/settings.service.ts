// src/app/features/settings/services/settings.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class SettingsService {
  private api = inject(ApiService);

  getGeneralSettings(): Observable<any> {
    return this.api.get('admin/settings/general');
  }

  updateGeneralSettings(data: any): Observable<any> {
    return this.api.put('admin/settings/general', data);
  }

  getPaymentSettings(): Observable<any> {
    return this.api.get('admin/settings/payment');
  }

  updatePaymentSettings(data: any): Observable<any> {
    return this.api.put('admin/settings/payment', data);
  }

  getNotificationSettings(): Observable<any> {
    return this.api.get('admin/settings/notifications');
  }

  updateNotificationSettings(data: any): Observable<any> {
    return this.api.put('admin/settings/notifications', data);
  }

  getSecuritySettings(): Observable<any> {
    return this.api.get('admin/settings/security');
  }

  updateSecuritySettings(data: any): Observable<any> {
    return this.api.put('admin/settings/security', data);
  }
}
