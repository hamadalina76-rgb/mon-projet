// src/app/features/menu/services/menu.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class MenuService {
  private api = inject(ApiService);

  getCategories(): Observable<any> {
    return this.api.get('partner/menu/categories');
  }

  createCategory(data: any): Observable<any> {
    return this.api.post('partner/menu/categories', data);
  }

  updateCategory(id: string, data: any): Observable<any> {
    return this.api.put(`partner/menu/categories/${id}`, data);
  }

  deleteCategory(id: string): Observable<any> {
    return this.api.delete(`partner/menu/categories/${id}`);
  }

  reorderCategories(ids: string[]): Observable<any> {
    return this.api.post('partner/menu/categories/reorder', { ids });
  }
}
