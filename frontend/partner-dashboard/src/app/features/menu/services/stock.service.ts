import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import {
  ProductStockDTO,
  UpdateStockRequest,
  BulkStockUpdateResult,
} from '../models/menu.models';

export interface StockFilterParams {
  search?: string;
  status?: string;
}

export interface StockPage {
  content: ProductStockDTO[];
  totalElements: number;
  page: number;
  size: number;
}

export interface StockStats {
  total: number;
  inStock: number;
  lowStock: number;
  outOfStock: number;
}

@Injectable({ providedIn: 'root' })
export class StockService {
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

  private buildStockUrl(params?: StockFilterParams & { page?: number; size?: number }): string {
    const query: string[] = [];
    if (params?.search?.trim()) query.push(`search=${encodeURIComponent(params.search.trim())}`);
    if (params?.status)         query.push(`status=${encodeURIComponent(params.status)}`);
    if (params?.page != null)   query.push(`page=${params.page}`);
    if (params?.size != null)   query.push(`size=${params.size}`);
    return query.length ? `${this.base()}/stock?${query.join('&')}` : `${this.base()}/stock`;
  }

  getStockPage(params?: StockFilterParams & { page?: number; size?: number }): Observable<StockPage> {
    return this.api.get<StockPage>(this.buildStockUrl(params));
  }

  getStockStats(): Observable<StockStats> {
    return this.api.get<StockStats>(`${this.base()}/stock/stats`);
  }

  /** Fetches all matching items in one page — used for CSV export. */
  getAllFiltered(params?: StockFilterParams): Observable<ProductStockDTO[]> {
    return this.api.get<StockPage>(this.buildStockUrl({ ...params, page: 0, size: 9999 })).pipe(
      map(page => page.content),
    );
  }

  updateStock(productId: number, request: UpdateStockRequest): Observable<ProductStockDTO> {
    return this.api.patch<ProductStockDTO>(`${this.base()}/${productId}/stock`, request);
  }

  getLowStock(): Observable<ProductStockDTO[]> {
    return this.api.get<ProductStockDTO[]>(`${this.base()}/low-stock`);
  }

  getOutOfStock(): Observable<ProductStockDTO[]> {
    return this.api.get<ProductStockDTO[]>(`${this.base()}/out-of-stock`);
  }

  bulkUpdateStock(file: File): Observable<BulkStockUpdateResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.api.upload<BulkStockUpdateResult>(`${this.base()}/stock/bulk`, formData);
  }
}
