// src/app/features/profile/services/profile.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private api = inject(ApiService);

  getProfile(): Observable<any> {
    return this.api.get('partner/profile');
  }

  updateProfile(data: any): Observable<any> {
    return this.api.put('partner/profile', data);
  }

  updateBusinessInfo(data: any): Observable<any> {
    return this.api.put('partner/profile/business', data);
  }

  uploadLogo(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('logo', file);
    return this.api.post('partner/profile/logo', formData);
  }

  uploadCover(file: File): Observable<any> {
    const formData = new FormData();
    formData.append('cover', file);
    return this.api.post('partner/profile/cover', formData);
  }

  getOpeningHours(): Observable<any> {
    return this.api.get('partner/profile/opening-hours');
  }

  updateOpeningHours(data: any): Observable<any> {
    return this.api.put('partner/profile/opening-hours', data);
  }

  getDeliveryZones(): Observable<any> {
    return this.api.get('partner/profile/delivery-zones');
  }

  updateDeliveryZones(data: any): Observable<any> {
    return this.api.put('partner/profile/delivery-zones', data);
  }

  changePassword(currentPassword: string, newPassword: string): Observable<any> {
    return this.api.post('partner/profile/change-password', { currentPassword, newPassword });
  }
}
