// src/app/core/services/partner.service.ts
import { Injectable } from '@angular/core';
import { Observable, map } from 'rxjs';
import { ApiService } from './api.service';
import { PartnerProfileDto, ScheduleException } from '@core/models/partner.model';

export interface PartnerRegistrationRequest {
  // Step 1: Account & Business Info
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  phoneNumber: string;
  businessName: string;
  partnerType: string;
  
  // Step 2: Address
  address: string;
  city: string;
  postalCode: string;
  country: string;
  latitude: number;
  longitude: number;
  
  // Step 3: Legal Info
  legalStatus: string;
  tva?: string;
  legalRepFirstName: string;
  legalRepLastName: string;
  position: string;
  
  // Step 4: Bank Info
  accountHolderName: string;
  iban: string;
  bankName: string;
  currency: string;
  
  // Step 5: Operational Info
  preparationTime: string;
  acceptOnlinePayment: boolean;
  acceptCashPayment: boolean;
  
  // Step 6: Fees
  minimumOrder?: number;
  noMinimum?: boolean;
  
  // Step 7: Presentation
  shortDescription: string;
  fullDescription: string;
  tags: string[];
  
  // Step 8: Terms
  acceptTerms: boolean;
  acceptPrivacy: boolean;
}

export interface PartnerRegistrationResponse {
  message: string;
  partnerId?: number;
  userId?: number;
  status?: string;
}

@Injectable({
  providedIn: 'root',
})
export class PartnerService {
  constructor(private apiService: ApiService) {}

  /**
   * Register a new partner - Step 1: Create Auth Account
   * POST /api/v1/auth/register
   */
  registerPartnerAuth(data: {
    email: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
  }): Observable<{message: string}> {
    return this.apiService.post<{message: string}>('v1/auth/register', {
      ...data,
      role: 'PARTNER'
    });
  }

  /**
   * Register a new partner - Complete Registration (Full data)
   * POST /api/v1/partners (to be implemented in backend)
   * For now, this combines auth registration with partner profile creation
   */
  registerPartner(data: PartnerRegistrationRequest): Observable<PartnerRegistrationResponse> {
    // First, register the auth account
    return this.registerPartnerAuth({
      email: data.email,
      password: data.password,
      firstName: data.firstName,
      lastName: data.lastName,
      phoneNumber: data.phoneNumber
    }).pipe(
      // TODO: After auth registration, create partner profile with remaining data
      // This will be implemented when backend partner-service endpoints are ready
      map(response => ({
        message: response.message,
        status: 'pending'
      }))
    );
  }

  /**
   * Upload partner documents
   * POST /api/partners/{id}/documents
   */
  uploadDocuments(partnerId: number, formData: FormData): Observable<any> {
    return this.apiService.upload(`partners/${partnerId}/documents`, formData);
  }

  /**
   * Upload partner images (logo, cover, photos)
   * POST /api/partners/{id}/images
   */
  uploadImages(partnerId: number, formData: FormData): Observable<any> {
    return this.apiService.upload(`partners/${partnerId}/images`, formData);
  }

  /**
   * Get partner details
   * GET /api/partners/{id}
   */
  getPartner(id: number): Observable<PartnerProfileDto> {
    return this.apiService.get<PartnerProfileDto>(`partners/${id}`);
  }

  /**
   * Get partner by userId
   * GET /api/partners/by-user/{userId}
   */
  getPartnerByUserId(userId: number): Observable<PartnerProfileDto> {
    return this.apiService.get<PartnerProfileDto>(`partners/by-user/${userId}`);
  }

  /**
   * Complete partner profile (Phase 2 - after login)
   * PUT /api/partners/{id}/complete-profile
   */
  completeProfile(partnerId: number, data: any): Observable<any> {
    return this.apiService.put(`partners/${partnerId}/complete-profile`, data);
  }

  /**
   * Update partner
   * PUT /api/partners/{id}
   */
  updatePartner(id: number, data: any): Observable<any> {
    return this.apiService.put(`partners/${id}`, data);
  }

  /**
   * Update schedule exceptions (jours fériés, fermetures exceptionnelles)
   * PUT /api/partners/{id}/complete-profile with scheduleExceptionsJson only
   */
  updateScheduleExceptions(partnerId: number, exceptions: ScheduleException[]): Observable<any> {
    return this.apiService.put(`partners/${partnerId}/complete-profile`, {
      scheduleExceptionsJson: JSON.stringify(exceptions),
    });
  }

  /**
   * Toggle ouvert/fermé (acceptsOrders). PATCH /api/partners/{id}/status
   * Réservé OWNER ou ADMIN côté backend.
   */
  updateStatus(partnerId: number, isOpen: boolean): Observable<PartnerProfileDto> {
    return this.apiService.patch<PartnerProfileDto>(`partners/${partnerId}/status`, { isOpen });
  }

  /**
   * GET /api/partners/{id}/opening-hours – horaires dédiés
   */
  getOpeningHours(partnerId: number): Observable<any[]> {
    return this.apiService.get<any[]>(`partners/${partnerId}/opening-hours`);
  }

  /**
   * PUT /api/partners/{id}/opening-hours – modifier horaires
   */
  putOpeningHours(partnerId: number, hours: any[]): Observable<any[]> {
    return this.apiService.put<any[]>(`partners/${partnerId}/opening-hours`, hours);
  }

  /**
   * GET /api/partners/{id}/staff – liste du staff
   */
  getStaff(partnerId: number): Observable<any[]> {
    return this.apiService.get<any[]>(`partners/${partnerId}/staff`);
  }
}
