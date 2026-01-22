// src/app/features/menu/services/product.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';

@Injectable({
  providedIn: 'root',
})
export class ProductService {
  private api = inject(ApiService);

  getProducts(categoryId?: string): Observable<any> {
    const params = categoryId ? { categoryId } : {};
    return this.api.get('partner/menu/products', params);
  }

  getProduct(id: string): Observable<any> {
    return this.api.get(`partner/menu/products/${id}`);
  }

  createProduct(data: any): Observable<any> {
    return this.api.post('partner/menu/products', data);
  }

  updateProduct(id: string, data: any): Observable<any> {
    return this.api.put(`partner/menu/products/${id}`, data);
  }

  deleteProduct(id: string): Observable<any> {
    return this.api.delete(`partner/menu/products/${id}`);
  }

  toggleAvailability(id: string): Observable<any> {
    return this.api.post(`partner/menu/products/${id}/toggle-availability`, {});
  }

  uploadImage(id: string, file: File): Observable<any> {
    const formData = new FormData();
    formData.append('image', file);
    return this.api.post(`partner/menu/products/${id}/image`, formData);
  }
}
