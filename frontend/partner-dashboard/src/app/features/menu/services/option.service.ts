import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import {
  OptionGroup,
  Option,
  CreateOptionGroupRequest,
  UpdateOptionGroupRequest,
  CreateOptionRequest,
  UpdateOptionRequest,
  ReorderItem,
} from '../models/menu.models';

@Injectable({ providedIn: 'root' })
export class OptionService {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  private get partnerId(): number {
    const id = this.auth.getPartnerId();
    if (!id) throw new Error('Partner ID not available');
    return id;
  }

  private productBase(productId: number): string {
    return `partners/${this.partnerId}/menu/products/${productId}/option-groups`;
  }

  // ─── Option Groups ────────────────────────────────────────────────────────

  getGroups(productId: number): Observable<OptionGroup[]> {
    return this.api.get<OptionGroup[]>(this.productBase(productId));
  }

  createGroup(productId: number, data: CreateOptionGroupRequest): Observable<OptionGroup> {
    return this.api.post<OptionGroup>(this.productBase(productId), data);
  }

  updateGroup(productId: number, groupId: number, data: UpdateOptionGroupRequest): Observable<OptionGroup> {
    return this.api.put<OptionGroup>(`${this.productBase(productId)}/${groupId}`, data);
  }

  deleteGroup(productId: number, groupId: number): Observable<void> {
    return this.api.delete<void>(`${this.productBase(productId)}/${groupId}`);
  }

  reorderGroups(productId: number, items: ReorderItem[]): Observable<OptionGroup[]> {
    return this.api.patch<OptionGroup[]>(`${this.productBase(productId)}/reorder`, { items });
  }

  // ─── Options ──────────────────────────────────────────────────────────────

  createOption(productId: number, groupId: number, data: CreateOptionRequest): Observable<Option> {
    return this.api.post<Option>(`${this.productBase(productId)}/${groupId}/options`, data);
  }

  updateOption(_productId: number, _groupId: number, optionId: number, data: UpdateOptionRequest): Observable<Option> {
    return this.api.put<Option>(`partners/${this.partnerId}/menu/options/${optionId}`, data);
  }

  deleteOption(_productId: number, _groupId: number, optionId: number): Observable<void> {
    return this.api.delete<void>(`partners/${this.partnerId}/menu/options/${optionId}`);
  }
}
