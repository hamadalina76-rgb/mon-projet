import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import {
  MenuCategory,
  FullMenuResponse,
  CreateCategoryRequest,
  UpdateCategoryRequest,
  ReorderItem,
} from '../models/menu.models';

@Injectable({ providedIn: 'root' })
export class MenuService {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  private get partnerId(): number {
    const id = this.auth.getPartnerId();
    if (!id) throw new Error('Partner ID not available');
    return id;
  }

  private base(): string {
    return `partners/${this.partnerId}/menu`;
  }

  // ─── Full menu ────────────────────────────────────────────────────────────

  getFullMenu(): Observable<FullMenuResponse> {
    return this.api.get<FullMenuResponse>(this.base());
  }

  // ─── Categories ──────────────────────────────────────────────────────────

  getCategories(): Observable<MenuCategory[]> {
    return this.api.get<MenuCategory[]>(`${this.base()}/categories`);
  }

  getCategory(id: number): Observable<MenuCategory> {
    return this.api.get<MenuCategory>(`${this.base()}/categories/${id}`);
  }

  createCategory(data: CreateCategoryRequest): Observable<MenuCategory> {
    return this.api.post<MenuCategory>(`${this.base()}/categories`, data);
  }

  updateCategory(id: number, data: UpdateCategoryRequest): Observable<MenuCategory> {
    return this.api.put<MenuCategory>(`${this.base()}/categories/${id}`, data);
  }

  deleteCategory(id: number): Observable<void> {
    return this.api.delete<void>(`${this.base()}/categories/${id}`);
  }

  toggleCategoryVisibility(id: number): Observable<MenuCategory> {
    return this.api.patch<MenuCategory>(`${this.base()}/categories/${id}/toggle-visibility`, {});
  }

  reorderCategories(items: ReorderItem[]): Observable<MenuCategory[]> {
    return this.api.patch<MenuCategory[]>(`${this.base()}/categories/reorder`, { items });
  }

  /** Upload image de catégorie (multipart, champ 'file'). Retourne { url, category }. */
  uploadCategoryImage(categoryId: number, file: File): Observable<{ url: string; category: MenuCategory }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.upload<{ url: string; category: MenuCategory }>(
      `${this.base()}/categories/${categoryId}/upload/image`,
      formData
    );
  }
}
