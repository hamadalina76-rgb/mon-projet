import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable, map, catchError, throwError } from 'rxjs';
import { environment } from '@environments/environment';
import { AuthService } from '@core/services/auth.service';
import {
  Category,
  CategoryBusinessType,
  CreateCategoryRequest,
  UpdateCategoryRequest,
  CategoryStats,
  AuditLogEntry
} from '@core/models/category.model';

@Injectable({
  providedIn: 'root'
})
export class CategoriesService {
  private http = inject(HttpClient);
  private authService = inject(AuthService);

  // ✅ URL directe vers partner-service port 8083
  private baseUrl = `${environment.apiUrl}/categories`;

  constructor() {
    console.log('🎯 CategoriesService initialized with URL:', this.baseUrl);
    // http://localhost:8083/api/v1/categories ✅
  }

  // ==================== HEADERS ====================

  private adminHeaders(): HttpHeaders {
    const adminId = this.authService.currentUser()?.id ?? '1';
    const token =
      localStorage.getItem('admin_token') ||
      sessionStorage.getItem('admin_token');
    let headers = new HttpHeaders({ 'X-Admin-Id': String(adminId) });
    if (token) {
      headers = headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  }

  // ==================== MAPPER ====================

  private mapCategory(cat: any): Category {
    return {
      ...cat,
      // ✅ nameI18n — toujours un objet, jamais une string
      nameI18n: typeof cat.nameI18n === 'string'
        ? JSON.parse(cat.nameI18n)
        : cat.nameI18n ?? {},

      // ✅ categoryBusinessType — cast string → enum
      categoryBusinessType: cat.categoryBusinessType as CategoryBusinessType,

      // ✅ Valeurs null → propres
      slug:             cat.slug             ?? null,
      description:      cat.description      ?? null,
      icon:             cat.icon             ?? null,
      image:            cat.image            ?? null,
      parentId:         cat.parentId         ?? null,
      depth:            cat.depth            ?? null,
      backgroundColor:  cat.backgroundColor  ?? null,
      textColor:        cat.textColor        ?? null,
      partnerCount:     cat.partnerCount     ?? null,
      productCount:     cat.productCount     ?? null,
      createdAt:        cat.createdAt        ?? null,
      updatedAt:        cat.updatedAt        ?? null,
      createdBy:        cat.createdBy        ?? null,
    };
  }

  // ==================== GET ALL ====================

  getCategories(search?: string, businessType?: string, status?: string): Observable<Category[]> {
    const hasFilter = !!(search?.trim()) || !!businessType || (!!status && status !== 'all');

    if (hasFilter) {
      // Appel endpoint dédié /search
      let params = new HttpParams();
      if (search?.trim())             params = params.set('q', search.trim());
      if (businessType)               params = params.set('businessType', businessType);
      if (status && status !== 'all') params = params.set('status', status);

      return this.http
        .get<any[]>(`${this.baseUrl}/search`, { headers: this.adminHeaders(), params })
        .pipe(
          map(res => res.map(cat => this.mapCategory(cat))),
          catchError(error => {
            console.error('❌ Erreur recherche catégories:', error);
            return throwError(() => error);
          })
        );
    }

    // Sans filtre : liste complète
    return this.http
      .get<any[]>(this.baseUrl, { headers: this.adminHeaders() })
      .pipe(
        map(res => res.map(cat => this.mapCategory(cat))),
        catchError(error => {
          console.error('❌ Erreur chargement catégories:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== GET BY ID ====================

  getCategoryById(id: number): Observable<Category> {
    console.log('📞 Appel GET Category by ID:', id);
    return this.http
      .get<any>(`${this.baseUrl}/${id}`, { headers: this.adminHeaders() })
      .pipe(
        map(res => {
          console.log('✅ Category response brute:', res);
          const raw = res?.data ? res.data : res;
          return this.mapCategory(raw);
        }),
        catchError(error => {
          console.error('❌ Erreur lors du chargement de la catégorie:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== CREATE ====================

  createCategory(data: CreateCategoryRequest): Observable<Category> {
    console.log('📞 Appel POST Create Category:', data);
    return this.http
      .post<any>(this.baseUrl, data, { headers: this.adminHeaders() })
      .pipe(
        map(res => this.mapCategory(res)),
        catchError(error => {
          console.error('❌ Erreur lors de la création de la catégorie:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== UPDATE ====================

  updateCategory(id: number, data: UpdateCategoryRequest): Observable<Category> {
    console.log('📞 Appel PUT Update Category:', id, data);
    return this.http
      .put<any>(`${this.baseUrl}/${id}`, data, { headers: this.adminHeaders() })
      .pipe(
        map(res => this.mapCategory(res)),
        catchError(error => {
          console.error('❌ Erreur lors de la mise à jour de la catégorie:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== DELETE ====================

  deleteCategory(id: number): Observable<void> {
    console.log('📞 Appel DELETE Category:', id);
    return this.http
      .delete<void>(`${this.baseUrl}/${id}`, { headers: this.adminHeaders() })
      .pipe(
        catchError(error => {
          console.error('❌ Erreur lors de la suppression de la catégorie:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== UPLOAD ICON ====================

  uploadIcon(file: File): Observable<string> {
    const formData = new FormData();
    formData.append('file', file);
    // Ne pas inclure Content-Type dans les headers : le browser gère le boundary multipart automatiquement
    const adminId = this.authService.currentUser()?.id ?? '1';
    const token = localStorage.getItem('admin_token') || sessionStorage.getItem('admin_token');
    let uploadHeaders = new HttpHeaders({ 'X-Admin-Id': String(adminId) });
    if (token) uploadHeaders = uploadHeaders.set('Authorization', `Bearer ${token}`);
    return this.http
      .post<{ url: string }>(`${this.baseUrl}/upload-icon`, formData, { headers: uploadHeaders })
      .pipe(
        map(res => res.url),
        catchError(error => {
          console.error('❌ Erreur upload icône:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== TOGGLE ACTIVE ====================

  toggleCategoryStatus(id: number): Observable<Category> {
    console.log('📞 Appel PATCH Toggle Active:', id);
    return this.http
      .patch<any>(
        `${this.baseUrl}/${id}/toggle-active`,
        {},
        { headers: this.adminHeaders() }
      )
      .pipe(
        map(res => this.mapCategory(res)),
        catchError(error => {
          console.error('❌ Erreur lors du changement de statut:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== PARENT CANDIDATES ====================

  getParentCandidates(excludeId?: number): Observable<Category[]> {
    let params = new HttpParams();
    if (excludeId != null) params = params.set('excludeId', String(excludeId));
    return this.http
      .get<any[]>(`${this.baseUrl}/parent-candidates`, { headers: this.adminHeaders(), params })
      .pipe(
        map(res => res.map(cat => this.mapCategory(cat))),
        catchError(error => {
          console.error('\u274c Erreur chargement parent candidates:', error);
          return throwError(() => error);
        })
      );
  }

  // ==================== PARENT CATEGORIES ====================

  getParentCategories(excludeId?: number): Observable<Category[]> {
    return this.getCategories().pipe(
      map(categories => categories.filter(cat => cat.id !== excludeId))
    );
  }

  // ==================== STATS ====================

  getCategoryStats(id: number): Observable<CategoryStats> {
    return this.http
      .get<CategoryStats>(`${this.baseUrl}/${id}/stats`, { headers: this.adminHeaders() })
      .pipe(
        catchError(error => {
          console.error('❌ Erreur chargement stats catégorie:', error);
          return throwError(() => error);
        })
      );
  }

  getCategoryAuditTrail(id: number): Observable<AuditLogEntry[]> {
    return this.http
      .get<AuditLogEntry[]>(`${this.baseUrl}/${id}/audit-trail`, { headers: this.adminHeaders() })
      .pipe(
        catchError(error => {
          console.error('❌ Erreur chargement audit trail:', error);
          return throwError(() => error);
        })
      );
  }
}