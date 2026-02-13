// src/app/core/services/admin.service.ts
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, map } from 'rxjs';
import { Admin, AdminRole, AdminStatus, RoleDefinition, Permission } from '@core/models/admin.model';
import { environment } from '@environments/environment';

// Interface pour la pagination Spring
interface Page<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

// Interface pour les requêtes backend
interface AdminRequest {
  fullName: string;
  email: string;
  roleId: number;
  status?: AdminStatus;
  avatar?: string;
  customPermissions?: string[];
  notes?: string;
}

// Interface pour la création avec auth
interface CreateAdminWithAuthRequest {
  fullName: string;
  email: string;
  password: string;
  roleId: number;
  status: string;
  customPermissions?: string[];
}

// Interface pour les réponses backend
interface AdminResponse {
  id: number;
  userId: number;
  fullName: string;
  email: string;
  role: AdminRoleResponse;
  status: AdminStatus;
  avatar?: string;
  lastLogin?: string;
  customPermissions?: string[];
  createdAt: string;
  updatedAt: string;
  notes?: string;
}

interface AdminRoleResponse {
  id: number;
  code: string;
  label: string;
  description?: string;
  color: string;
  trustLevel: number;
  maxRefundAmount?: number;
  requiresApproval: boolean;
  permissions: PermissionResponse[];
  createdAt: string;
  active: boolean;
}

interface PermissionResponse {
  id: number;
  module: string;
  moduleLabel: string;
  icon: string;
  description?: string;
  descriptionEn?: string;
  descriptionAr?: string;
  enabled: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${environment.apiUrl}/admins`;
  private readonly rolesApiUrl = `${environment.apiUrl}/admin-roles`;

  /**
   * Récupère la liste des admins avec pagination et filtres
   */
  getAdmins(filters: {
    page?: number;
    size?: number;
    sortBy?: string;
    sortDirection?: 'ASC' | 'DESC';
    search?: string;
    status?: AdminStatus;
    roleId?: number;
  } = {}): Observable<{
    admins: Admin[];
    total: number;
    page: number;
    size: number;
  }> {
    let params = new HttpParams();
    
    if (filters.page !== undefined) params = params.set('page', filters.page.toString());
    if (filters.size !== undefined) params = params.set('size', filters.size.toString());
    if (filters.sortBy) params = params.set('sortBy', filters.sortBy);
    if (filters.sortDirection) params = params.set('sortDirection', filters.sortDirection);
    if (filters.search) params = params.set('search', filters.search);
    if (filters.status) params = params.set('status', filters.status);
    if (filters.roleId) params = params.set('roleId', filters.roleId.toString());

    return this.http.get<Page<AdminResponse>>(this.apiUrl, { params }).pipe(
      map(page => ({
        admins: page.content.map(this.mapAdminResponseToAdmin),
        total: page.totalElements,
        page: page.number,
        size: page.size
      }))
    );
  }

  /**
   * Récupère un admin par son ID
   */
  getAdminById(id: string): Observable<Admin | undefined> {
    return this.http.get<AdminResponse>(`${this.apiUrl}/${id}`).pipe(
      map(this.mapAdminResponseToAdmin)
    );
  }

  /**
   * Crée un nouvel admin
   */
  createAdmin(admin: Partial<Admin>): Observable<Admin> {
    const request: AdminRequest = {
      fullName: admin.fullName || '',
      email: admin.email || '',
      roleId: parseInt(admin.role?.id || '1'),
      status: admin.status,
      avatar: admin.avatar
    };

    return this.http.post<AdminResponse>(
      this.apiUrl, 
      request,
      { headers: { 'X-User-Id': this.getCurrentUserId() } }
    ).pipe(
      map(this.mapAdminResponseToAdmin)
    );
  }

  /**
   * Crée un admin avec son compte d'authentification
   * Utilise le nouvel endpoint /with-auth
   */
  createAdminWithAuth(adminData: any): Observable<Admin> {
    const request = {
      fullName: adminData.fullName,
      email: adminData.email,
      password: adminData.password,
      roleId: parseInt(adminData.role?.id || '1'),
      status: 'ACTIVE',
      customPermissions: adminData.permissions || []
    };

    return this.http.post<AdminResponse>(
      `${this.apiUrl}/with-auth`,
      request,
      { headers: { 'X-User-Id': this.getCurrentUserId() } }
    ).pipe(
      map(this.mapAdminResponseToAdmin)
    );
  }

  /**
   * Met à jour un admin existant
   */
  updateAdmin(id: string, admin: Partial<Admin>): Observable<Admin> {
    const request: AdminRequest = {
      fullName: admin.fullName || '',
      email: admin.email || '',
      roleId: parseInt(admin.role?.id || '1'),
      status: admin.status,
      avatar: admin.avatar,
      customPermissions: admin.permissions
    };

    return this.http.put<AdminResponse>(`${this.apiUrl}/${id}`, request).pipe(
      map(this.mapAdminResponseToAdmin)
    );
  }

  /**
   * Supprime un admin
   */
  deleteAdmin(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  /**
   * Change le statut d'un admin
   */
  changeAdminStatus(id: string, status: AdminStatus): Observable<Admin> {
    return this.http.patch<AdminResponse>(
      `${this.apiUrl}/${id}/status`,
      null,
      { params: { status } }
    ).pipe(
      map(this.mapAdminResponseToAdmin)
    );
  }

  /**
   * Récupère tous les rôles actifs
   */
  getRoles(): Observable<AdminRole[]> {
    return this.http.get<AdminRoleResponse[]>(`${this.rolesApiUrl}/active`).pipe(
      map(roles => roles.map(this.mapRoleResponseToRole))
    );
  }

  /**
   * Récupère toutes les définitions de rôles
   */
  getRoleDefinitions(): Observable<RoleDefinition[]> {
    return this.http.get<AdminRoleResponse[]>(this.rolesApiUrl).pipe(
      map(roles => roles.map(role => ({
        id: role.id.toString(),
        name: role.code,
        label: role.label,
        description: role.description || '',
        trustLevel: role.trustLevel,
        maxRefundAmount: role.maxRefundAmount || 0,
        permissions: role.permissions.map(p => ({
          module: p.module,
          moduleLabel: p.moduleLabel,
          icon: p.icon,
          description: p.description || '',
          enabled: p.enabled
        }))
      })))
    );
  }

  /**
   * Récupère les permissions disponibles
   */
  getAvailablePermissions(): Observable<Permission[]> {
    // On récupère tous les modules possibles depuis le rôle SUPER_ADMIN
    return this.http.get<AdminRoleResponse>(`${this.rolesApiUrl}/by-code/SUPER_ADMIN`).pipe(
      map(role => role.permissions.map(p => ({
        module: p.module,
        moduleLabel: p.moduleLabel,
        icon: p.icon,
        description: p.description || '',
        descriptionEn: p.descriptionEn,
        descriptionAr: p.descriptionAr,
        enabled: true
      })))
    );
  }

  /**
   * Mapper: AdminResponse → Admin
   */
  private mapAdminResponseToAdmin = (response: AdminResponse): Admin => {
    return {
      id: response.id.toString(),
      fullName: response.fullName,
      email: response.email,
      role: this.mapRoleResponseToRole(response.role),
      status: response.status,
      avatar: response.avatar,
      lastLogin: this.formatLastLogin(response.lastLogin),
      permissions: response.customPermissions || [],
      createdAt: response.createdAt
    };
  };

  /**
   * Mapper: AdminRoleResponse → AdminRole
   */
  private mapRoleResponseToRole = (response: AdminRoleResponse): AdminRole => {
    return {
      id: response.id.toString(),
      name: response.code,
      label: response.label,
      color: response.color,
      trustLevel: response.trustLevel,
      maxRefundAmount: response.maxRefundAmount,
      permissions: response.permissions.map(p => ({
        module: p.module,
        moduleLabel: p.moduleLabel,
        icon: p.icon,
        description: p.description || '',
        descriptionEn: p.descriptionEn,
        descriptionAr: p.descriptionAr,
        enabled: p.enabled
      }))
    };
  };

  /**
   * Formate la date de dernière connexion
   */
  private formatLastLogin(lastLogin?: string): string {
    if (!lastLogin) return 'Jamais';

    const date = new Date(lastLogin);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
    const diffDays = Math.floor(diffHours / 24);

    if (diffHours < 1) return 'Il y a quelques minutes';
    if (diffHours < 24) return `Il y a ${diffHours} heure${diffHours > 1 ? 's' : ''}`;
    if (diffDays === 1) return 'Hier';
    if (diffDays < 7) return `Il y a ${diffDays} jours`;

    return date.toLocaleDateString('fr-FR');
  }

  /**
   * Récupère l'ID de l'utilisateur courant
   * TODO: Récupérer depuis AuthService
   */
  private getCurrentUserId(): string {
    // À implémenter: récupérer depuis le token JWT ou AuthService
    return '1';
  }
}
