import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import {
  Product,
  CreateProductRequest,
  UpdateProductRequest,
  ReorderItem,
} from '../models/menu.models';

@Injectable({ providedIn: 'root' })
export class ProductService {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  private get partnerId(): number {
    const id = this.auth.getPartnerId();
    if (!id) throw new Error('Partner ID not available');
    return id;
  }

  private base(): string {
    return `partners/${this.partnerId}/menu/products`;
  }

  getProducts(categoryId?: number): Observable<Product[]> {
    const params = categoryId != null ? { categoryId } : undefined;
    return this.api.get<Product[]>(this.base(), params);
  }

  getProduct(id: number): Observable<Product> {
    return this.api.get<Product>(`${this.base()}/${id}`);
  }

  createProduct(data: CreateProductRequest): Observable<Product> {
    return this.api.post<Product>(this.base(), data);
  }

  updateProduct(id: number, data: UpdateProductRequest): Observable<Product> {
    return this.api.put<Product>(`${this.base()}/${id}`, data);
  }

  deleteProduct(id: number): Observable<void> {
    return this.api.delete<void>(`${this.base()}/${id}`);
  }

  toggleAvailability(id: number, isAvailable: boolean): Observable<Product> {
    return this.api.patch<Product>(`${this.base()}/${id}/availability`, { isAvailable });
  }

  /** Upload image produit (multipart, champ 'file'). Retourne { url, product }. */
  uploadProductImage(productId: number, file: File): Observable<{ url: string; product: Product }> {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.upload<{ url: string; product: Product }>(
      `${this.base()}/${productId}/upload/image`,
      formData
    );
  }

  reorderProducts(categoryId: number, items: ReorderItem[]): Observable<Product[]> {
    return this.api.patch<Product[]>(
      `partners/${this.partnerId}/menu/categories/${categoryId}/products/reorder`,
      { items }
    );
  }
}
