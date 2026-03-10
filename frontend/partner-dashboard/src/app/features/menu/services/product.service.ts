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

  /** Page de produits avec filtres et pagination côté backend. */
  getProductsPage(params: {
    search?: string;
    categoryId?: number | null;
    status?: string;
    page: number;
    size: number;
  }): Observable<{ content: Product[]; totalElements: number; totalPages: number; size: number; number: number }> {
    const q: Record<string, string | number> = {
      page: params.page,
      size: params.size,
      status: params.status ?? 'all',
    };
    if (params.search != null && params.search !== '') q['search'] = params.search;
    if (params.categoryId != null) q['categoryId'] = params.categoryId;
    return this.api.get<{ content: Product[]; totalElements: number; totalPages: number; size: number; number: number }>(
      this.base(),
      q
    );
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

  /** TC-38 : Duplique le produit (et ses options). Retourne le nouveau produit. */
  duplicateProduct(id: number): Observable<Product> {
    return this.api.post<Product>(`${this.base()}/${id}/duplicate`, {});
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

  /** PATCH /partners/{id}/menu/products/reorder — items: [{ id, position }] */
  reorderProducts(items: ReorderItem[]): Observable<Product[]> {
    return this.api.patch<Product[]>(`${this.base()}/reorder`, { items });
  }

  /** PATCH /partners/{id}/menu/products/promotions — set promotion label, end date and optional discount %. */
  setPromotion(productIds: number[], promotionLabel: string | null, promotionEndDate: string | null, discountPercentage: number | null): Observable<Product[]> {
    const body: { productIds: number[]; promotionLabel?: string; promotionEndDate?: string; discountPercentage?: number } = { productIds };
    if (promotionLabel != null) body.promotionLabel = promotionLabel;
    if (promotionEndDate != null) body.promotionEndDate = promotionEndDate;
    if (discountPercentage != null && discountPercentage > 0) body.discountPercentage = discountPercentage;
    return this.api.patch<Product[]>(`${this.base()}/promotions`, body);
  }
}
