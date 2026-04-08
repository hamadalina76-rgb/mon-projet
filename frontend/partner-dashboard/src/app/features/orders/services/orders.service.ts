// src/app/features/orders/services/orders.service.ts
import { Injectable, inject } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { ApiService } from '@core/services/api.service';
import { AuthService } from '@core/services/auth.service';
import { TranslateService } from '@ngx-translate/core';

export interface OrderListParams {
  page?:    number;
  size?:    number;
  status?:  string;
  search?:  string;
  sortBy?:  string;
  sortDir?: string;
  /** ISO LocalDateTime, ex. 2026-04-07T00:00:00 */
  from?:    string;
  /** ISO LocalDateTime, ex. 2026-04-07T23:59:59 */
  to?:      string;
}

export interface OrderPage {
  content:          any[];
  totalElements:    number;
  totalPages:       number;
  number:           number;
  size:             number;
}

/** Filtres + pagination GET /orders/{id}/history */
export interface OrderHistoryParams {
  status?:    string;
  actorType?: string;
  /** ISO local, ex. 2026-04-07T17:29:00 */
  from?:      string;
  to?:        string;
  page?:      number;
  size?:      number;
}

export interface OrderHistoryPage {
  content:       any[];
  totalElements: number;
  totalPages:    number;
  number:        number;
  size:          number;
}

/**
 * Filtre statut côté UI (`READY`) → enum order-service (`READY_FOR_PICKUP`).
 */
function toApiOrderStatusFilter(status: string): string {
  const s = status.trim();
  if (s === 'READY') return 'READY_FOR_PICKUP';
  return s;
}

/**
 * Appelle order-service via la gateway : /api/orders/**
 */
@Injectable({ providedIn: 'root' })
export class OrdersService {
  private api       = inject(ApiService);
  private auth      = inject(AuthService);
  private translate = inject(TranslateService);

  private partnerIdOrThrow(): number {
    const id = this.auth.getPartnerId();
    if (id == null) {
      throw new Error(this.translate.instant('ORDERS.ERRORS.PARTNER_ID'));
    }
    return id;
  }

  getOrders(params: OrderListParams = {}): Observable<OrderPage> {
    const partnerId = this.partnerIdOrThrow();
    const queryParams: Record<string, any> = {
      page:    params.page    ?? 0,
      size:    params.size    ?? 10,
      sortBy:  params.sortBy  ?? 'priority',
      sortDir: params.sortDir ?? 'desc',
    };
    if (params.status && params.status !== 'all') {
      queryParams['status'] = toApiOrderStatusFilter(params.status);
    }
    if (params.search?.trim()) {
      queryParams['search'] = params.search.trim();
    }
    if (params.from) queryParams['from'] = params.from;
    if (params.to)   queryParams['to']   = params.to;

    return this.api.get<any>(`orders/partners/${partnerId}`, queryParams).pipe(
      map((res) => ({
        content:       (res?.content ?? []).map((o: any) => this.normalizeOrder(o)),
        totalElements: res?.totalElements ?? 0,
        totalPages:    res?.totalPages    ?? 0,
        number:        res?.number        ?? 0,
        size:          res?.size          ?? queryParams['size'],
      })),
      catchError((err) => {
        console.error('[OrdersService] getOrders', err);
        return of({ content: [], totalElements: 0, totalPages: 0, number: 0, size: params.size ?? 10 });
      })
    );
  }

  /** Normalise les clés (backend: ALL, PENDING…) pour l’UI (all, PENDING…). Même `from`/`to` que {@link getOrders}. */
  getOrderCounts(params?: Pick<OrderListParams, 'from' | 'to'>): Observable<Record<string, number>> {
    const partnerId = this.partnerIdOrThrow();
    const queryParams: Record<string, string> = {};
    if (params?.from) queryParams['from'] = params.from;
    if (params?.to) queryParams['to'] = params.to;
    return this.api.get<Record<string, number>>(`orders/partners/${partnerId}/counts`, queryParams).pipe(
      map((raw) => {
        const n = (k: string) => raw[k] ?? 0;
        return {
          ALL:       n('ALL'),
          all:       n('ALL'),
          PENDING:   n('PENDING'),
          CONFIRMED: n('CONFIRMED'),
          PREPARING: n('PREPARING'),
          READY:     n('READY'),
          CANCELLED: n('CANCELLED'),
        };
      }),
      catchError(() =>
        of({
          ALL: 0,
          all: 0,
          PENDING: 0,
          CONFIRMED: 0,
          PREPARING: 0,
          READY: 0,
          CANCELLED: 0,
        })
      )
    );
  }

  getOrder(id: string): Observable<any> {
    return this.api.get<any>(`orders/${id}`).pipe(map((o) => this.normalizeOrder(o)));
  }

  getOrderHistory(id: string, params?: OrderHistoryParams): Observable<OrderHistoryPage> {
    const q: Record<string, string | number> = {
      page: params?.page ?? 0,
      size: params?.size ?? 10,
    };
    if (params?.status?.trim()) q['status'] = toApiOrderStatusFilter(params.status.trim());
    if (params?.actorType?.trim()) q['actorType'] = params.actorType.trim();
    if (params?.from?.trim()) q['from'] = params.from.trim();
    if (params?.to?.trim()) q['to'] = params.to.trim();

    return this.api.get<any>(`orders/${id}/history`, q).pipe(
      map((res) => ({
        content: (res?.content ?? []).map((h: any) => ({
          status:               h.status,
          previousStatus:       h.previousStatus ?? null,
          description:          h.description ?? null,
          notes:                h.notes       ?? null,
          updatedBy:            h.updatedBy   ?? null,
          actorType:            h.actorType   ?? (h.updatedBy ? h.updatedBy.split(':')[0] : 'SYSTEM'),
          timestamp:            h.timestamp,
          estimatedPrepMinutes: h.estimatedPrepMinutes ?? null,
        })),
        totalElements: res?.totalElements ?? 0,
        totalPages:    res?.totalPages    ?? 0,
        number:        res?.number        ?? 0,
        size:          res?.size          ?? (params?.size ?? 10),
      })),
      catchError(() =>
        of({
          content:       [] as any[],
          totalElements: 0,
          totalPages:    0,
          number:        0,
          size:          params?.size ?? 10,
        })
      )
    );
  }

  confirmOrder(id: string, estimatedPrepTime?: number): Observable<any> {
    const partnerId = this.partnerIdOrThrow();
    const body: { partnerId: number; estimatedPrepTime?: number } = { partnerId };
    if (estimatedPrepTime != null && estimatedPrepTime > 0) {
      body.estimatedPrepTime = estimatedPrepTime;
    }
    return this.api
      .post<any>(`orders/${id}/partner/confirm`, body)
      .pipe(map((o) => this.normalizeOrder(o)));
  }

  startPreparing(id: string): Observable<any> {
    const partnerId = this.partnerIdOrThrow();
    return this.api
      .put<any>(`orders/${id}/status`, {
        status: 'PREPARING',
        actorType: 'PARTNER',
        actorId: partnerId,
        notes: this.translate.instant('ORDERS.API.PREP_NOTES'),
      })
      .pipe(map((o) => this.normalizeOrder(o)));
  }

  markReady(id: string): Observable<any> {
    const partnerId = this.partnerIdOrThrow();
    return this.api
      .post<any>(`orders/${id}/partner/ready`, { partnerId })
      .pipe(map((o) => this.normalizeOrder(o)));
  }

  cancelOrder(id: string, reason: string): Observable<any> {
    const partnerId = this.partnerIdOrThrow();
    return this.api
      .deleteWithBody<any>(`orders/${id}`, {
        cancelledBy: 'PARTNER', actorId: partnerId,
        reason: reason || this.translate.instant('ORDERS.API.CANCEL_DEFAULT'),
      })
      .pipe(map((o) => this.normalizeOrder(o)));
  }

  updatePrepTime(id: string, prepTime: number): Observable<any> {
    const partnerId = this.partnerIdOrThrow();
    return this.api.post<any>(`orders/${id}/partner/confirm`, { partnerId, estimatedPrepTime: prepTime });
  }

  getMissedOrders(since: Date): Observable<any[]> {
    const partnerId = this.auth.getPartnerId();
    if (partnerId == null) return of([]);
    const sinceMs = since.getTime();
    return this.api.get<any[]>(`orders/partners/${partnerId}/active`).pipe(
      map((list) => {
        const arr = Array.isArray(list) ? list : [];
        return arr
          .map((o) => this.normalizeOrder(o))
          .filter((o) => {
            const t = o.createdAt ? new Date(o.createdAt).getTime() : 0;
            return t >= sinceMs;
          });
      }),
      catchError(() => of([]))
    );
  }

  private normalizeOrder(o: any): any {
    if (!o) return o;

    let status = (o.status as string);
    if (status === 'READY_FOR_PICKUP') status = 'READY';

    const rawHistory: any[] = o.statusHistory ?? [];
    const statusHistory = rawHistory.map((h: any) => ({
      status:                 h.status,
      previousStatus:         h.previousStatus  ?? null,
      timestamp:              h.timestamp,
      description:            h.description     ?? null,
      notes:                  h.notes           ?? null,
      updatedBy:              h.updatedBy       ?? null,
      actorType:              h.actorType       ?? (h.updatedBy ? h.updatedBy.split(':')[0] : 'SYSTEM'),
      estimatedPrepMinutes:   h.estimatedPrepMinutes ?? null,
    }));

    const tsOf = (s: string) => rawHistory.find((h: any) => h.status === s)?.timestamp;

    return {
      ...o,
      id:           o.id != null ? String(o.id) : o.id,
      status,
      createdAt:    o.orderTime         ?? o.createdAt,
      orderType:    o.type              ?? o.orderType,
      notes:        o.customerNotes     ?? o.notes,
      cancelReason: o.cancellationReason ?? o.cancelReason,
      subtotal:     o.subtotal,
      deliveryFee:  o.deliveryFee       ?? 0,
      serviceFee:   o.serviceFee        ?? 0,
      discount:     o.discount          ?? 0,
      total:        o.total,
      suggestedPreparationMinutes:
        o.suggestedPreparationMinutes != null
          ? Number(o.suggestedPreparationMinutes)
          : undefined,
      confirmedAt:  tsOf('CONFIRMED'),
      preparingAt:  tsOf('PREPARING'),
      readyAt:      tsOf('READY') ?? tsOf('READY_FOR_PICKUP'),
      deliveredAt:  tsOf('DELIVERED'),
      cancelledAt:  tsOf('CANCELLED'),
      statusHistory,
      customer: o.customer ?? {
        id:    o.customerId  != null ? String(o.customerId) : '',
        name:  o.customerName  ?? '',
        phone: o.customerPhone ?? '',
      },
      items: (o.items ?? []).map((it: any) => ({
        ...it,
        productId: it.productId != null ? String(it.productId) : it.productId,
        unitPrice: it.unitPrice ?? it.price ?? 0,
        preparationTimeMin:
          it.preparationTimeMin != null ? Number(it.preparationTimeMin) : undefined,
      })),
    };
  }
}
