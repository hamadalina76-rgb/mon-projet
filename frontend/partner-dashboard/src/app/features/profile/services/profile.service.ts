// src/app/features/profile/services/profile.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { PartnerService } from '@core/services/partner.service';
import { PartnerProfileDto } from '@core/models/partner.model';

@Injectable({
  providedIn: 'root',
})
export class ProfileService {
  private api = inject(ApiService);
  private authService = inject(AuthService);
  private partnerService = inject(PartnerService);

  getProfile(): Observable<any> {
    return this.api.get('partner/profile');
  }

  /**
   * Récupère le profil partenaire complet depuis le backend (partners API).
   * Utilise partnerId si disponible, sinon by-user/{userId}.
   */
  getCurrentPartner(): Observable<PartnerProfileDto> {
    const user = this.authService.currentUser();
    if (!user) {
      return new Observable(obs => {
        obs.error(new Error('Non authentifié'));
      });
    }
    if (user.partnerId) {
      return this.partnerService.getPartner(user.partnerId);
    }
    return this.partnerService.getPartnerByUserId(user.id);
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

  /** Horaires depuis openingHoursDisplay du partner (partners API). */
  getOpeningHours(): Observable<any[]> {
    return this.getCurrentPartner().pipe(
      map(p => {
        const raw = p?.openingHoursDisplay;
        if (!raw) return [];
        try {
          const arr = JSON.parse(raw);
          return Array.isArray(arr) ? arr : [];
        } catch {
          return [];
        }
      })
    );
  }

  /** Mise à jour des horaires via complete-profile. */
  updateOpeningHours(data: any[]): Observable<any> {
    const partnerId = this.authService.getPartnerId();
    if (!partnerId) {
      return new Observable(obs => obs.error(new Error('Partenaire introuvable')));
    }
    return this.partnerService.completeProfile(partnerId, {
      openingHoursJson: JSON.stringify(data),
    });
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
